package ru.funduruk.net;

import org.funduruk.dto.MessageDTO;

import java.util.function.Consumer;

public class ChatEventBus {

    private static Consumer<MessageDTO> messageListener;

    public static void setOnMessage(Consumer<MessageDTO> listener) {
        messageListener = listener;
    }

    public static void fireMessage(MessageDTO msg) {
        System.out.println("fireMessage called, listener null: " + (messageListener == null));
        if (messageListener != null) {
            messageListener.accept(msg);
        }
    }
}
