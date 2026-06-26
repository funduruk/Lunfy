package ru.funduruk.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.funduruk.dto.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class WSClient {

    private static WebSocketClient client;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String lastUrl;
    private static volatile boolean shouldReconnect = true;

    public static void connect(String url) {
        lastUrl = url;
        shouldReconnect = true;
        doConnect();
    }

    public static void doConnect() {

        if (client != null && client.isOpen()) {
            System.out.println("Already connected, skipping");
            return;
        }

        try {
            client = new WebSocketClient(new URI(lastUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected");
                    ChatEventBus.fireReconnect();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        EnvelopeDTO env = mapper.readValue(message, EnvelopeDTO.class);

                        if ("CHAT_MESSAGE".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            MessageDTO msg = mapper.readValue(dataJson, MessageDTO.class);
                            msg.setMine("user-1".equals(msg.getSender()));
                            ChatEventBus.fireMessage(msg);

                        } else if ("DELETE_MESSAGE".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            MessageDTO msg = mapper.readValue(dataJson, MessageDTO.class);
                            ChatEventBus.fireDeleteMessage(msg);
                        }  else if ("DELETE_CHAT".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        MessageDTO msg = mapper.readValue(dataJson, MessageDTO.class);
                        ChatEventBus.fireDeleteChat(msg.getChatId());
                        }else if ("GROUP_MEMBER_KICKED".equals(env.getType()) ||
                                "GROUP_ROLE_CHANGED".equals(env.getType()) ||
                                "GROUP_MEMBER_ADDED".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireGroupMemberUpdate(env.getType(), data);
                        } else if ("GROUP_DM_CREATED".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireGroupDMCreated(data);
                        } else if ("CALL_OFFER".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        CallSignalDTO signal = mapper.readValue(dataJson, CallSignalDTO.class);
                        ChatEventBus.fireCallOffer(signal);
                    } else if ("CALL_ANSWER".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        CallSignalDTO signal = mapper.readValue(dataJson, CallSignalDTO.class);
                        ChatEventBus.fireCallAnswer(signal);
                    } else if ("CALL_REJECT".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        CallSignalDTO signal = mapper.readValue(dataJson, CallSignalDTO.class);
                        ChatEventBus.fireCallReject(signal);
                    } else if ("CALL_END".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        CallSignalDTO signal = mapper.readValue(dataJson, CallSignalDTO.class);
                        ChatEventBus.fireCallEnd(signal);
                    } else if ("AUDIO_CHUNK".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        AudioChunkDTO chunk = mapper.readValue(dataJson, AudioChunkDTO.class);
                        ChatEventBus.fireAudioChunk(chunk);
                    } else if ("SCREEN_FRAME".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        ScreenFrameDTO frame = mapper.readValue(dataJson, ScreenFrameDTO.class);
                        ChatEventBus.fireScreenFrame(frame);
                    } else if ("SCREEN_SHARE_START".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        ScreenFrameDTO frame = mapper.readValue(dataJson, ScreenFrameDTO.class);
                        ChatEventBus.fireScreenShareStart(frame);
                    } else if ("SCREEN_SHARE_STOP".equals(env.getType())) {
                        String dataJson = mapper.writeValueAsString(env.getData());
                        ScreenFrameDTO frame = mapper.readValue(dataJson, ScreenFrameDTO.class);
                        ChatEventBus.fireScreenShareStop(frame);
                        } else if ("VIDEO_FRAME".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            VideoFrameDTO frame = mapper.readValue(dataJson, VideoFrameDTO.class);
                            ChatEventBus.fireVideoFrame(frame);
                        } else if ("VIDEO_START".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            VideoFrameDTO frame = mapper.readValue(dataJson, VideoFrameDTO.class);
                            ChatEventBus.fireVideoStart(frame);
                        } else if ("VIDEO_STOP".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            VideoFrameDTO frame = mapper.readValue(dataJson, VideoFrameDTO.class);
                            ChatEventBus.fireVideoStop(frame);
                        } else if ("GROUP_INVITE".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireGroupInvite(data);
                        } else if ("GROUP_JOINED".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireGroupJoined(data);
                        } else if ("CHANNEL_CREATED".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireChannelCreated(data);
                        } else if ("CHANNEL_DELETED".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireChannelDeleted(data);
                        } else if ("FRIEND_REQUEST".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireFriendRequest(data);
                        } else if ("FRIEND_ACCEPTED".equals(env.getType()) || "FRIEND_REMOVED".equals(env.getType())) {
                            String dataJson = mapper.writeValueAsString(env.getData());
                            Map<String, Object> data = mapper.readValue(dataJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
                            ChatEventBus.fireFriendUpdate(env.getType(), data);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected: " + reason);
                    if (shouldReconnect) {
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            System.out.println(">>> Переподключение к WebSocket...");
                            doConnect();
                        }, "ws-reconnect").start();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();

        } catch (Exception e) {
            e.printStackTrace();

            if (shouldReconnect) {
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    doConnect();
                }, "ws-reconnect").start();
            }
        }
    }

    public static void send(EnvelopeDTO env) {
        try {
            if (client == null || !client.isOpen()) {
                System.err.println("WS not connected");
                return;
            }

            String json = mapper.writeValueAsString(env);
            client.send(json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        shouldReconnect = false;
        if (client != null && client.isOpen()) {
            client.close();
        }
    }
}