package ru.funduruk.media;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.VideoFrameDTO;
import ru.funduruk.net.WSClient;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.function.Consumer;

public class VideoCall {

    private final String fromUser;
    private final String toUser;
    private final String chatId;

    private final Consumer<BufferedImage> localPreview;

    public VideoCall(String fromUser, String toUser, String chatId,
                     Consumer<BufferedImage> localPreview) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.chatId = chatId;
        this.localPreview = localPreview;
    }

    private volatile boolean running = false;
    private Webcam webcam;

    public void start() {
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                System.err.println("Камера не найдена");
                return;
            }
            webcam.setViewSize(WebcamResolution.VGA.getSize()); // 640x480
            webcam.open();
            running = true;
            new Thread(this::captureLoop, "video-capture").start();
            System.out.println("VideoCall запущен");
        } catch (Exception e) {
            System.err.println("Не удалось открыть камеру: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static final int FPS = 12;

    private void captureLoop() {
        long frameInterval = 1000 / FPS;
        while (running) {
            long startTime = System.currentTimeMillis();
            try {
                BufferedImage frame = webcam.getImage();
                if (frame != null) {
                    if (localPreview != null) localPreview.accept(frame);

                    String encoded = encodeJpeg(frame);
                    VideoFrameDTO dto = new VideoFrameDTO(fromUser, toUser, chatId, encoded);
                    WSClient.send(new EnvelopeDTO("VIDEO_FRAME", dto));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            long sleep = frameInterval - (System.currentTimeMillis() - startTime);
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static final float JPEG_QUALITY = 0.5f;

    private String encodeJpeg(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
        }
        writer.dispose();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public void stop() {
        running = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        System.out.println("VideoCall остановлен");
    }

    public static boolean isCameraAvailable() {
        try {
            return com.github.sarxos.webcam.Webcam.getDefault() != null;
        } catch (Exception e) {
            return false;
        }
    }
}