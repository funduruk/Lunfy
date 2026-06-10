package ru.funduruk.net;

import org.funduruk.dto.MessageDTO;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatEventBus {

    private static Consumer<MessageDTO> messageListener;
    private static Consumer<MessageDTO> deleteListener;
    private static Consumer<String> deleteChatListener;
    private static BiConsumer<String, Map<String, Object>> groupMemberUpdateListener;
    private static Consumer<Map<String, Object>> groupDMCreatedListener;


    public static void setOnMessage(Consumer<MessageDTO> listener) {
        messageListener = listener;
    }

    public static void setOnDeleteMessage(Consumer<MessageDTO> listener) {
        deleteListener = listener;
    }

    public static void setOnDeleteChat(Consumer<String> listener) {
        deleteChatListener = listener;
    }

    public static void setOnGroupDMCreated(Consumer<Map<String, Object>> listener) { groupDMCreatedListener = listener; }

    public static void setOnGroupMemberUpdate(
            java.util.function.BiConsumer<String, Map<String, Object>> listener) {
        groupMemberUpdateListener = listener;
    }

    public static void fireMessage(MessageDTO msg) {
        System.out.println("fireMessage called, listener null: " + (messageListener == null));
        if (messageListener != null) {
            messageListener.accept(msg);
        }
    }

    public static void fireDeleteMessage(MessageDTO msg) {
        if (deleteListener != null) {
            deleteListener.accept(msg);
        }
    }

    public static void fireDeleteChat(String chatId) {
        if (deleteChatListener != null) {
            deleteChatListener.accept(chatId);
        }
    }

    public static void fireGroupMemberUpdate(String type, Map<String, Object> data) {
        if (groupMemberUpdateListener != null) {
            groupMemberUpdateListener.accept(type, data);
        }
    }

    public static void fireGroupDMCreated(Map<String, Object> data) {
        if (groupDMCreatedListener != null) {
            groupDMCreatedListener.accept(data);
        }
    }
}