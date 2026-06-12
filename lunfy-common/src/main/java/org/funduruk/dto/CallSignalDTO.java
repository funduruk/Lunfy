package org.funduruk.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallSignalDTO {

    private String callId;
    private String from;
    private String to;
    private String sdp;       // offer / answer
    private String candidate; // ICE
}