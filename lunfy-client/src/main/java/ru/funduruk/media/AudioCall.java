package ru.funduruk.media;

import lombok.Setter;
import org.funduruk.dto.AudioChunkDTO;
import org.funduruk.dto.EnvelopeDTO;
import ru.funduruk.net.WSClient;

import javax.sound.sampled.*;
import java.util.Base64;

public class AudioCall {

    // Format: 16 кГц, 16 бит
    private static final AudioFormat FORMAT =
            new AudioFormat(16000.0f, 16, 1, true, false);

    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private volatile boolean running = false;

    private final String fromUser;
    private final String toUser;
    private final String chatId;

    public AudioCall(String fromUser, String toUser, String chatId) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.chatId = chatId;
    }

    public void start() {
        running = true;
        try {
            // open micro
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, FORMAT);
            micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
            micLine.open(FORMAT);
            micLine.start();

            // open speaker
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, FORMAT);
            speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakerLine.open(FORMAT);
            speakerLine.start();

            new Thread(this::captureLoop, "audio-capture").start();

            System.out.println("AudioCall запущен");
        } catch (LineUnavailableException e) {
            System.err.println("Не удалось открыть аудиоустройство: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureLoop() {
        byte[] buffer = new byte[1024];
        while (running) {
            int read = micLine.read(buffer, 0, buffer.length);
            if (read > 0 && !muted) {
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                String encoded = Base64.getEncoder().encodeToString(chunk);
                AudioChunkDTO dto = new AudioChunkDTO(fromUser, toUser, chatId, encoded);
                WSClient.send(new EnvelopeDTO("AUDIO_CHUNK", dto));
            }
        }
    }

    public void playChunk(String base64Data) {
        if (!running || speakerLine == null) return;
        byte[] data = Base64.getDecoder().decode(base64Data);
        speakerLine.write(data, 0, data.length);
    }

    public void stop() {
        running = false;
        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }
        if (speakerLine != null) {
            speakerLine.drain();
            speakerLine.stop();
            speakerLine.close();
        }
        System.out.println("AudioCall остановлен");
    }

    @Setter
    private volatile boolean muted = false;
}