package ru.funduruk.media;

import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.ScreenFrameDTO;
import ru.funduruk.net.WSClient;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

public class ScreenShare {

    private static final int FPS = 15;
    private static final int TARGET_WIDTH = 1280;
    private static final float JPEG_QUALITY = 0.5f;

    private int lastFrameHash = 0;

    private volatile boolean running = false;
    private Robot robot;
    private final Rectangle screenRect;

    private final String fromUser;
    private final String toUser;
    private final String chatId;

    public ScreenShare(String fromUser, String toUser, String chatId) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.chatId = chatId;
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }



    public void start() {
        try {
            robot = new Robot();
            running = true;
            new Thread(this::captureLoop, "screen-capture").start();
            System.out.println("ScreenShare запущен");
        } catch (AWTException e) {
            System.err.println("Не удалось создать Robot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureLoop() {
        long frameInterval = 1000 / FPS;
        while (running) {
            long startTime = System.currentTimeMillis();
            try {
                BufferedImage screen = robot.createScreenCapture(screenRect);
                BufferedImage scaled = scaleImage(screen);

                int hash = quickHash(scaled);
                if (hash != lastFrameHash) {
                    lastFrameHash = hash;
                    String encoded = encodeJpeg(scaled);
                    ScreenFrameDTO dto = new ScreenFrameDTO(fromUser, toUser, chatId, encoded);
                    WSClient.send(new EnvelopeDTO("SCREEN_FRAME", dto));
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

    private int quickHash(BufferedImage img) {
        int hash = 7;
        for (int y = 0; y < img.getHeight(); y += 16) {
            for (int x = 0; x < img.getWidth(); x += 16) {
                hash = hash * 31 + img.getRGB(x, y);
            }
        }
        return hash;
    }

    private BufferedImage scaleImage(BufferedImage src) {
        if (src.getWidth() <= TARGET_WIDTH) return src;
        double ratio = (double) TARGET_WIDTH / src.getWidth();
        int newW = TARGET_WIDTH;
        int newH = (int) (src.getHeight() * ratio);

        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return scaled;
    }

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
        System.out.println("ScreenShare остановлен");
    }
}