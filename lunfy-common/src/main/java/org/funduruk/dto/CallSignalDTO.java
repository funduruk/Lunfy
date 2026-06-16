package org.funduruk.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalDTO {
    private String fromUser;   // кто звонит
    private String toUser;     // кому звонят
    private String chatId;     // чат/канал, в рамках которого звонок
    private String callType;   // "AUDIO" или "SCREEN"
}