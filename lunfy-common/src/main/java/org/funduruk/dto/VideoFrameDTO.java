package org.funduruk.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameDTO {
    private String fromUser;
    private String toUser;
    private String chatId;
    private String data;
}