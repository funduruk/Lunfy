package ru.funduruk.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.funduruk.dto.EnvelopeDTO;
import org.funduruk.dto.MessageDTO;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WSClient {

    private static WebSocketClient client;
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void connect(String url) {

        if (client != null && client.isOpen()) {
            System.out.println("Already connected, skipping");
            return;
        }

        try {
            client = new WebSocketClient(new URI(url)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        System.out.println("CLIENT RAW MESSAGE: " + message);
                        EnvelopeDTO env = mapper.readValue(message, EnvelopeDTO.class);
                        System.out.println("TYPE = " + env.getType());
                        System.out.println("DATA = " + env.getData());

                        if ("CHAT_MESSAGE".equals(env.getType())) {
                            MessageDTO msg = mapper.convertValue(
                                    env.getData(),
                                    MessageDTO.class
                            );

                            msg.setMine(false);
                            ChatEventBus.fireMessage(msg);
                            System.out.println("FIRED TO EVENT BUS: " + msg.getText());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();

        } catch (Exception e) {
            e.printStackTrace();
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
}