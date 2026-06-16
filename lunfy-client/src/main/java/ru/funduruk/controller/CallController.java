package ru.funduruk.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.funduruk.dto.*;
import ru.funduruk.media.AudioCall;
import ru.funduruk.media.ScreenShare;
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

public class CallController {

    @FXML private Label callStatusLabel;
    @FXML private Label myInitial;
    @FXML private Label myNameLabel;
    @FXML private Label peerInitial;
    @FXML private Label peerNameLabel;
    @FXML private Button acceptBtn;
    @FXML private Button rejectBtn;
    @FXML private Button muteBtn;
    @FXML private Button screenBtn;
    @FXML private Button endBtn;
    @FXML private StackPane screenStack;
    @FXML private HBox participantsBox;
    @FXML private ImageView screenImageView;

    private String peerUser;
    private String chatId;
    private String callType;

    private AudioCall audioCall;
    private ScreenShare screenShare;
    private boolean sharing = false;
    private boolean muted = false;

    // ИСХОДЯЩИЙ
    public void initOutgoing(String peerUser, String chatId, String callType) {
        this.peerUser = peerUser;
        this.chatId = chatId;
        this.callType = callType;

        setupCommon();
        callStatusLabel.setText("Вызов " + peerUser + "...");
        showIncomingButtons(false);

        CallSignalDTO signal = new CallSignalDTO(
                ApiClient.getCurrentUsername(), peerUser, chatId, callType);
        WSClient.send(new EnvelopeDTO("CALL_OFFER", signal));
        endBtn.setVisible(true); endBtn.setManaged(true);
    }

    // ВХОДЯЩИЙ
    public void initIncoming(CallSignalDTO signal) {
        this.peerUser = signal.getFromUser();
        this.chatId = signal.getChatId();
        this.callType = signal.getCallType();

        setupCommon();
        callStatusLabel.setText("Входящий вызов от " + peerUser);
        showIncomingButtons(true);
    }

    private void setupCommon() {
        peerNameLabel.setText(peerUser);
        String me = ApiClient.getCurrentUsername();
        myNameLabel.setText(me);
        if (!me.isEmpty()) myInitial.setText(me.substring(0, 1).toUpperCase());
        if (peerUser != null && !peerUser.isEmpty())
            peerInitial.setText(peerUser.substring(0, 1).toUpperCase());
        registerListeners();
    }

    private void showIncomingButtons(boolean incoming) {
        acceptBtn.setVisible(incoming); acceptBtn.setManaged(incoming);
        rejectBtn.setVisible(incoming); rejectBtn.setManaged(incoming);
    }

    private void registerListeners() {
        ChatEventBus.setOnCallAnswer(signal -> {
            if (!signal.getFromUser().equalsIgnoreCase(peerUser)) return;
            Platform.runLater(this::onCallEstablished);
        });
        ChatEventBus.setOnCallReject(signal -> {
            if (!signal.getFromUser().equalsIgnoreCase(peerUser)) return;
            Platform.runLater(() -> {
                callStatusLabel.setText("Вызов отклонён");
                closeAfterDelay();
            });
        });
        ChatEventBus.setOnCallEnd(signal -> {
            if (!signal.getFromUser().equalsIgnoreCase(peerUser)) return;
            Platform.runLater(() -> {
                callStatusLabel.setText("Вызов завершён");
                stopMedia();
                closeAfterDelay();
            });
        });
    }

    @FXML
    private void accept() {
        WSClient.send(new EnvelopeDTO("CALL_ANSWER",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        onCallEstablished();
    }

    @FXML
    private void reject() {
        WSClient.send(new EnvelopeDTO("CALL_REJECT",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        closeCall();
    }

    @FXML
    private void end() {
        WSClient.send(new EnvelopeDTO("CALL_END",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        stopMedia();
        closeCall();
    }

    private void onCallEstablished() {
        callStatusLabel.setText("Звонок с " + peerUser);
        showIncomingButtons(false);
        muteBtn.setVisible(true); muteBtn.setManaged(true);
        screenBtn.setVisible(true); screenBtn.setManaged(true);
        endBtn.setVisible(true); endBtn.setManaged(true);

        audioCall = new AudioCall(ApiClient.getCurrentUsername(), peerUser, chatId);
        audioCall.start();

        ChatEventBus.setOnAudioChunk(chunk -> {
            if (chunk.getFromUser().equalsIgnoreCase(peerUser) && audioCall != null)
                audioCall.playChunk(chunk.getData());
        });

        ChatEventBus.setOnScreenShareStart(frame -> {
            if (frame.getFromUser().equalsIgnoreCase(peerUser))
                Platform.runLater(this::showScreen);
        });
        ChatEventBus.setOnScreenFrame(frame -> {
            if (frame.getFromUser().equalsIgnoreCase(peerUser))
                Platform.runLater(() -> updateScreen(frame.getData()));
        });
        ChatEventBus.setOnScreenShareStop(frame -> {
            if (frame.getFromUser().equalsIgnoreCase(peerUser))
                Platform.runLater(this::hideScreen);
        });
    }

    @FXML
    private void toggleMute() {
        muted = !muted;
        muteBtn.setText(muted ? "🔇" : "🎤");
        if (audioCall != null) audioCall.setMuted(muted);
    }

    @FXML
    private void toggleScreenShare() {
        if (!sharing) {
            sharing = true;
            screenBtn.setText("🛑");
            WSClient.send(new EnvelopeDTO("SCREEN_SHARE_START",
                    new ScreenFrameDTO(ApiClient.getCurrentUsername(), peerUser, chatId, "")));
            screenShare = new ScreenShare(ApiClient.getCurrentUsername(), peerUser, chatId);
            screenShare.start();
        } else {
            sharing = false;
            screenBtn.setText("🖥");
            if (screenShare != null) { screenShare.stop(); screenShare = null; }
            WSClient.send(new EnvelopeDTO("SCREEN_SHARE_STOP",
                    new ScreenFrameDTO(ApiClient.getCurrentUsername(), peerUser, chatId, "")));
        }
    }

    private void showScreen() {
        screenStack.setVisible(true); screenStack.setManaged(true);
        participantsBox.setVisible(false); participantsBox.setManaged(false);
        screenImageView.fitWidthProperty().bind(screenStack.widthProperty());
        screenImageView.fitHeightProperty().bind(screenStack.heightProperty());
    }

    private void updateScreen(String base64Jpeg) {
        byte[] data = java.util.Base64.getDecoder().decode(base64Jpeg);
        screenImageView.setImage(new Image(new java.io.ByteArrayInputStream(data)));
    }

    private void hideScreen() {
        screenStack.setVisible(false); screenStack.setManaged(false);
        participantsBox.setVisible(true); participantsBox.setManaged(true);
        screenImageView.setImage(null);
    }

    private void stopMedia() {
        if (audioCall != null) { audioCall.stop(); audioCall = null; }
        if (screenShare != null) { screenShare.stop(); screenShare = null; }
        ChatEventBus.setOnAudioChunk(null);
        ChatEventBus.setOnScreenFrame(null);
        ChatEventBus.setOnScreenShareStart(null);
        ChatEventBus.setOnScreenShareStop(null);
    }

    private void closeAfterDelay() {
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            Platform.runLater(this::closeCall);
        }).start();
    }

    private void closeCall() {
        // Возвращаемся к чату
        GeneralController gc = GeneralController.getInstance();
        if (gc != null) gc.returnToChat();
    }
}