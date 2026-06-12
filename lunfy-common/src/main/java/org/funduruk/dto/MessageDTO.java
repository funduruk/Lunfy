package org.funduruk.dto;

import lombok.Data;

@Data
public class MessageDTO {
    private long id;
    private String chatId;
    private String sender;
    private String text;
    private long timestamp;
    private boolean mine;
}