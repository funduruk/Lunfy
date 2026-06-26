package ru.funduruk.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.funduruk.dto.*;
import ru.funduruk.manager.CallSoundManager;
import ru.funduruk.media.AudioCall;
import ru.funduruk.media.ScreenShare;
import ru.funduruk.media.VideoCall;
import ru.funduruk.net.ApiClient;
import ru.funduruk.net.ChatEventBus;
import ru.funduruk.net.WSClient;

public class CallController {

    @FXML private Label callStatusLabel;
    @FXML private Button endBtn;

    private String peerUser;
    private String chatId;
    private String callType;

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

        CallSoundManager.playDialing();
    }

    public void initIncoming(CallSignalDTO signal) {
        this.peerUser = signal.getFromUser();
        this.chatId = signal.getChatId();
        this.callType = signal.getCallType();

        setupCommon();
        callStatusLabel.setText("Входящий вызов от " + peerUser);
        showIncomingButtons(true);

        CallSoundManager.playRingtone();
    }

    @FXML private ImageView myAvatar;
    @FXML private ImageView peerAvatar;

    private void loadParticipantAvatar(javafx.scene.image.ImageView target, String username) {
        if (username == null || username.isEmpty()) return;

        String url = ApiClient.HTTP_BASE + "/api/users/" + username
                + "/avatar?t=" + System.currentTimeMillis();
        javafx.scene.image.Image img = new javafx.scene.image.Image(url, 112, 112, true, true, true);

        img.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                Platform.runLater(() -> {
                    target.setImage(img);
                    target.setClip(new javafx.scene.shape.Circle(56, 56, 56));
                });
            }
        });
    }

    @FXML private Label peerInitial;
    @FXML private Label peerNameLabel;
    @FXML private Label myInitial;
    @FXML private Label myNameLabel;

    private void setupCommon() {
        peerNameLabel.setText(peerUser);
        String me = ApiClient.getCurrentUsername();
        myNameLabel.setText(me);
        if (!me.isEmpty()) myInitial.setText(me.substring(0, 1).toUpperCase());
        if (peerUser != null && !peerUser.isEmpty())
            peerInitial.setText(peerUser.substring(0, 1).toUpperCase());
        loadParticipantAvatar(myAvatar, ApiClient.getCurrentUsername());
        loadParticipantAvatar(peerAvatar, peerUser);
        registerListeners();
    }

    @FXML private Button acceptBtn;
    @FXML private Button rejectBtn;

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
        CallSoundManager.stopAll();
        WSClient.send(new EnvelopeDTO("CALL_ANSWER",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        onCallEstablished();
    }

    @FXML
    private void reject() {
        CallSoundManager.stopAll();
        WSClient.send(new EnvelopeDTO("CALL_REJECT",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        closeCall();
    }

    @FXML
    private void end() {
        CallSoundManager.stopAll();
        WSClient.send(new EnvelopeDTO("CALL_END",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        stopMedia();
        closeCall();
    }

    @FXML private Button muteBtn;
    @FXML private Button screenBtn;

    private AudioCall audioCall;
    private boolean muted = false;

    @FXML
    private void toggleMute() {
        muted = !muted;
        muteBtn.setText(muted ? "🔇" : "🎤");
        if (muted) muteBtn.getStyleClass().add("call-btn-active");
        else muteBtn.getStyleClass().remove("call-btn-active");
        if (audioCall != null) audioCall.setMuted(muted);
    }

    private ScreenShare screenShare;
    private boolean sharing = false;

    @FXML
    private void toggleScreenShare() {
        if (!sharing) {
            sharing = true;
            screenBtn.setText("🛑");
            screenBtn.getStyleClass().add("call-btn-active");
            WSClient.send(new EnvelopeDTO("SCREEN_SHARE_START",
                    new ScreenFrameDTO(ApiClient.getCurrentUsername(), peerUser, chatId, "")));
            screenShare = new ScreenShare(ApiClient.getCurrentUsername(), peerUser, chatId);
            screenShare.start();
        } else {
            sharing = false;
            screenBtn.setText("🖥");
            screenBtn.getStyleClass().remove("call-btn-active");
            if (screenShare != null) { screenShare.stop(); screenShare = null; }
            WSClient.send(new EnvelopeDTO("SCREEN_SHARE_STOP",
                    new ScreenFrameDTO(ApiClient.getCurrentUsername(), peerUser, chatId, "")));
        }
    }

    @FXML private ImageView screenImageView;
    @FXML private HBox participantsBox;
    @FXML private StackPane screenStack;

    private void showScreen() {
        screenStack.setVisible(true); screenStack.setManaged(true);
        screenImageView.fitWidthProperty().bind(screenStack.widthProperty());
        screenImageView.fitHeightProperty().bind(screenStack.heightProperty());
        updateCenterLayout();
        repositionPeerVideo();
    }

    private void hideScreen() {
        screenImageView.setImage(null);
        updateCenterLayout();
        repositionPeerVideo();
    }

    private void updateScreen(String base64Jpeg) {
        byte[] data = java.util.Base64.getDecoder().decode(base64Jpeg);
        screenImageView.setImage(new Image(new java.io.ByteArrayInputStream(data)));
    }


    @FXML private Button videoBtn;
    @FXML private StackPane peerVideoStack;
    @FXML private ImageView peerVideoView;
    @FXML private StackPane selfVideoStack;
    @FXML private ImageView selfVideoView;

    private VideoCall videoCall;
    private boolean videoOn = false;

    private void stopMedia() {
        CallSoundManager.stopAll();
        if (audioCall != null) { audioCall.stop(); audioCall = null; }
        if (screenShare != null) { screenShare.stop(); screenShare = null; }
        if (videoCall != null) { videoCall.stop(); videoCall = null; }
        ChatEventBus.setOnAudioChunk(null);
        ChatEventBus.setOnScreenFrame(null);
        ChatEventBus.setOnScreenShareStart(null);
        ChatEventBus.setOnScreenShareStop(null);
        ChatEventBus.setOnVideoFrame(null);
        ChatEventBus.setOnVideoStart(null);
        ChatEventBus.setOnVideoStop(null);
    }

    private void closeAfterDelay() {
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            Platform.runLater(this::closeCall);
        }).start();
    }

    private void closeCall() {
        CallSoundManager.stopAll();
        GeneralController gc = GeneralController.getInstance();
        if (gc != null) gc.returnToChat();
    }

    @FXML
    private void toggleVideo() {
        if (!videoOn) {
            videoOn = true;
            videoBtn.setText("🚫");
            videoBtn.getStyleClass().add("call-btn-active");
            WSClient.send(new EnvelopeDTO("VIDEO_START",
                    new VideoFrameDTO(ApiClient.getCurrentUsername(), peerUser, chatId, "")));

            selfVideoStack.setVisible(true); selfVideoStack.setManaged(true);

            videoCall = new VideoCall(
                    ApiClient.getCurrentUsername(), peerUser, chatId,
                    frame -> Platform.runLater(() -> selfVideoView.setImage(toFxImage(frame)))
            );
            videoCall.start();
        } else {
            videoOn = false;
            videoBtn.setText("📹");
            videoBtn.getStyleClass().remove("call-btn-active");
            if (videoCall != null) { videoCall.stop(); videoCall = null; }
            selfVideoStack.setVisible(false); selfVideoStack.setManaged(false);
            selfVideoView.setImage(null);
            WSClient.send(new EnvelopeDTO("VIDEO_STOP",
                    new VideoFrameDTO(ApiClient.getCurrentUsername(), peerUser, chatId, "")));
        }
    }

    private void showPeerVideo() {
        peerVideoStack.setVisible(true); peerVideoStack.setManaged(true);
        updateCenterLayout();
        repositionPeerVideo();
    }

    private void updatePeerVideo(String base64Jpeg) {
        byte[] data = java.util.Base64.getDecoder().decode(base64Jpeg);
        peerVideoView.setImage(new Image(new java.io.ByteArrayInputStream(data)));
    }

    private void hidePeerVideo() {
        peerVideoStack.setVisible(false); peerVideoStack.setManaged(false);
        updateCenterLayout();
        peerVideoView.setImage(null);
    }

    @FXML private StackPane centerStack;

    private void repositionPeerVideo() {
        if (!peerVideoStack.isVisible()) return;

        boolean screenActive = screenStack.isVisible();

        peerVideoView.fitWidthProperty().unbind();
        peerVideoView.fitHeightProperty().unbind();

        if (screenActive) {
            // Маленькое окошко в углу при демонстрации
            StackPane.setAlignment(peerVideoStack, javafx.geometry.Pos.TOP_RIGHT);
            peerVideoStack.setMaxSize(220, 165);
            peerVideoStack.setMinSize(220, 165);
            peerVideoView.setFitWidth(216);
            peerVideoView.setFitHeight(161);
            StackPane.setMargin(peerVideoStack, new javafx.geometry.Insets(20, 20, 0, 0));
            if (!peerVideoStack.getStyleClass().contains("peer-video-corner")) {
                peerVideoStack.getStyleClass().add("peer-video-corner");
            }
        } else {
            // Большое во весь центр
            StackPane.setAlignment(peerVideoStack, javafx.geometry.Pos.CENTER);
            peerVideoStack.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            peerVideoStack.setMinSize(0, 0);
            StackPane.setMargin(peerVideoStack, javafx.geometry.Insets.EMPTY);
            peerVideoView.fitWidthProperty().bind(centerStack.widthProperty().subtract(40));
            peerVideoView.fitHeightProperty().bind(centerStack.heightProperty().subtract(40));
            peerVideoStack.getStyleClass().remove("peer-video-corner");
        }
    }

    private Image toFxImage(java.awt.image.BufferedImage bImage) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bImage, "jpg", baos);
            return new Image(new java.io.ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean autoStartVideo = false;
    public void setAutoStartVideo(boolean v) { this.autoStartVideo = v; }

    private void onCallEstablished() {
        CallSoundManager.stopAll();
        callStatusLabel.setText("Звонок с " + peerUser);
        showIncomingButtons(false);
        muteBtn.setVisible(true); muteBtn.setManaged(true);
        screenBtn.setVisible(true); screenBtn.setManaged(true);
        endBtn.setVisible(true); endBtn.setManaged(true);

        toggleModeBtn.setVisible(true);
        toggleModeBtn.setManaged(true);

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

        videoBtn.setVisible(true); videoBtn.setManaged(true);

        if (!ru.funduruk.media.VideoCall.isCameraAvailable()) {
            videoBtn.setOpacity(0.4);
            Tooltip noCam = new Tooltip("Нет активных камер");
            noCam.setShowDelay(javafx.util.Duration.millis(200));
            Tooltip.install(videoBtn, noCam);
            videoBtn.setOnAction(e -> {});
        }

        ChatEventBus.setOnVideoStart(frame -> {
            if (frame.getFromUser().equalsIgnoreCase(peerUser))
                Platform.runLater(this::showPeerVideo);
        });
        ChatEventBus.setOnVideoFrame(frame -> {
            if (frame.getFromUser().equalsIgnoreCase(peerUser))
                Platform.runLater(() -> updatePeerVideo(frame.getData()));
        });
        ChatEventBus.setOnVideoStop(frame -> {
            if (frame.getFromUser().equalsIgnoreCase(peerUser))
                Platform.runLater(this::hidePeerVideo);
        });

        if (autoStartVideo && !videoOn) {
            toggleVideo();
        }
    }

    public void forceEnd() {
        CallSoundManager.stopAll();
        WSClient.send(new EnvelopeDTO("CALL_END",
                new CallSignalDTO(ApiClient.getCurrentUsername(), peerUser, chatId, callType)));
        stopMedia();
        closeCall();
    }

    @FXML private javafx.scene.layout.BorderPane callRoot;
    @FXML private Button toggleModeBtn;

    private boolean expanded = true;

    @FXML
    private void toggleMode() {
        expanded = !expanded;
        applyMode();
    }

    private void applyMode() {
        GeneralController gc = GeneralController.getInstance();

        if (expanded) {
            callRoot.getTop().setVisible(true);
            ((javafx.scene.layout.VBox) callRoot.getTop()).setManaged(true);
            centerStack.setVisible(true);
            centerStack.setManaged(true);
            toggleModeBtn.setText("🗕");

            if (gc != null) gc.setCallContainerHeight(-1);

            repositionPeerVideo();
        } else {
            callRoot.getTop().setVisible(false);
            ((javafx.scene.layout.VBox) callRoot.getTop()).setManaged(false);
            centerStack.setVisible(false);
            centerStack.setManaged(false);
            toggleModeBtn.setText("🗖");

            if (gc != null) gc.setCallContainerHeight(80);
        }
    }

    private void updateCenterLayout() {
        boolean screenActive = screenStack.isVisible();
        boolean peerVideoActive = peerVideoStack.isVisible();

        if (screenActive) {
            // Демонстрация экрана — центр занят им, видео собеседника в угол
            participantsBox.setVisible(false);
            participantsBox.setManaged(false);
        } else if (peerVideoActive) {
            // Видео собеседника на весь центр, аватарки убираем
            participantsBox.setVisible(false);
            participantsBox.setManaged(false);
        } else {
            // Ничего нет — показываем аватарки
            participantsBox.setVisible(true);
            participantsBox.setManaged(true);
        }
    }
}