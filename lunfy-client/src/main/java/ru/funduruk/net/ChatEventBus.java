package ru.funduruk.net;

import org.funduruk.dto.*;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatEventBus {

    private static Consumer<MessageDTO> messageListener;
    private static Consumer<MessageDTO> deleteListener;
    private static Consumer<String> deleteChatListener;
    private static BiConsumer<String, Map<String, Object>> groupMemberUpdateListener;
    private static Consumer<Map<String, Object>> groupDMCreatedListener;
    private static java.util.function.Consumer<CallSignalDTO> callOfferListener;
    private static java.util.function.Consumer<CallSignalDTO> callAnswerListener;
    private static java.util.function.Consumer<CallSignalDTO> callRejectListener;
    private static java.util.function.Consumer<CallSignalDTO> callEndListener;
    private static java.util.function.Consumer<AudioChunkDTO> audioChunkListener;
    private static java.util.function.Consumer<ScreenFrameDTO> screenFrameListener;
    private static java.util.function.Consumer<ScreenFrameDTO> screenStartListener;
    private static java.util.function.Consumer<ScreenFrameDTO> screenStopListener;
    private static java.util.function.Consumer<VideoFrameDTO> videoFrameListener;
    private static java.util.function.Consumer<VideoFrameDTO> videoStartListener;
    private static java.util.function.Consumer<VideoFrameDTO> videoStopListener;

    public static void setOnMessage(Consumer<MessageDTO> listener) { messageListener = listener; }
    public static void setOnDeleteMessage(Consumer<MessageDTO> listener) { deleteListener = listener; }
    public static void setOnDeleteChat(Consumer<String> listener) { deleteChatListener = listener; }
    public static void setOnGroupDMCreated(Consumer<Map<String, Object>> listener) { groupDMCreatedListener = listener; }
    public static void setOnGroupMemberUpdate(java.util.function.BiConsumer<String, Map<String, Object>> listener) { groupMemberUpdateListener = listener; }
    public static void setOnCallOffer(java.util.function.Consumer<CallSignalDTO> l) { callOfferListener = l; }
    public static void setOnCallAnswer(java.util.function.Consumer<CallSignalDTO> l) { callAnswerListener = l; }
    public static void setOnCallReject(java.util.function.Consumer<CallSignalDTO> l) { callRejectListener = l; }
    public static void setOnCallEnd(java.util.function.Consumer<CallSignalDTO> l) { callEndListener = l; }
    public static void setOnAudioChunk(java.util.function.Consumer<AudioChunkDTO> l) { audioChunkListener = l; }
    public static void setOnScreenFrame(java.util.function.Consumer<ScreenFrameDTO> l) { screenFrameListener = l; }
    public static void setOnScreenShareStart(java.util.function.Consumer<ScreenFrameDTO> l) { screenStartListener = l; }
    public static void setOnScreenShareStop(java.util.function.Consumer<ScreenFrameDTO> l) { screenStopListener = l; }
    public static void setOnVideoFrame(java.util.function.Consumer<VideoFrameDTO> l) { videoFrameListener = l; }
    public static void setOnVideoStart(java.util.function.Consumer<VideoFrameDTO> l) { videoStartListener = l; }
    public static void setOnVideoStop(java.util.function.Consumer<VideoFrameDTO> l) { videoStopListener = l; }

    public static void fireMessage(MessageDTO msg) { if (messageListener != null) { messageListener.accept(msg); } }
    public static void fireDeleteMessage(MessageDTO msg) { if (deleteListener != null) { deleteListener.accept(msg); } }
    public static void fireDeleteChat(String chatId) { if (deleteChatListener != null) { deleteChatListener.accept(chatId); } }
    public static void fireGroupMemberUpdate(String type, Map<String, Object> data) { if (groupMemberUpdateListener != null) { groupMemberUpdateListener.accept(type, data); } }
    public static void fireGroupDMCreated(Map<String, Object> data) { if (groupDMCreatedListener != null) { groupDMCreatedListener.accept(data); } }
    public static void fireCallOffer(CallSignalDTO s) { if (callOfferListener != null) callOfferListener.accept(s); }
    public static void fireCallAnswer(CallSignalDTO s) { if (callAnswerListener != null) callAnswerListener.accept(s); }
    public static void fireCallReject(CallSignalDTO s) { if (callRejectListener != null) callRejectListener.accept(s); }
    public static void fireCallEnd(CallSignalDTO s) { if (callEndListener != null) callEndListener.accept(s); }
    public static void fireAudioChunk(AudioChunkDTO c) { if (audioChunkListener != null) audioChunkListener.accept(c); }
    public static void fireScreenFrame(ScreenFrameDTO f) { if (screenFrameListener != null) screenFrameListener.accept(f); }
    public static void fireScreenShareStart(ScreenFrameDTO f) { if (screenStartListener != null) screenStartListener.accept(f); }
    public static void fireScreenShareStop(ScreenFrameDTO f) { if (screenStopListener != null) screenStopListener.accept(f); }
    public static void fireVideoFrame(VideoFrameDTO f) { if (videoFrameListener != null) videoFrameListener.accept(f); }
    public static void fireVideoStart(VideoFrameDTO f) { if (videoStartListener != null) videoStartListener.accept(f); }
    public static void fireVideoStop(VideoFrameDTO f) { if (videoStopListener != null) videoStopListener.accept(f); }
}