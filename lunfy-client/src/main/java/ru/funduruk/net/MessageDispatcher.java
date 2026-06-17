//package ru.funduruk.net;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.funduruk.dto.EnvelopeDTO;
//import ru.funduruk.controller.ChatTabController;
//import org.funduruk.dto.MessageDTO;
//
//public class MessageDispatcher {
//
//    static ChatTabController chatTabController;
//    private static final ObjectMapper mapper = new ObjectMapper();
//
//    public static void handle(String json) {
//        try {
//            EnvelopeDTO env = mapper.readValue(json, EnvelopeDTO.class);
//
//            switch (env.getType()) {
//                case "CHAT_MESSAGE" -> handleChatMessage(env);
//                case "TYPING" -> handleTyping(env);
//                case "CALL_OFFER", "CALL_ANSWER", "ICE_CANDIDATE" ->
//                        handleCall(env);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void handleChatMessage(EnvelopeDTO env) {
//        MessageDTO msg = mapper.convertValue(
//                env.getData(), MessageDTO.class
//        );
//
//        chatTabController.onMessage(msg);
//    }
//
//    private static void handleTyping(EnvelopeDTO env) {
//        // TODO
//    }
//
//    private static void handleCall(EnvelopeDTO env) {
//        // TODO (WebRTC)
//    }
//}
