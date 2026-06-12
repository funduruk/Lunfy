package org.funduruk.dto;

import lombok.Data;

@Data
public class TypingDTO {
    private String chatId;
    private String userId;
    private boolean typing;
}