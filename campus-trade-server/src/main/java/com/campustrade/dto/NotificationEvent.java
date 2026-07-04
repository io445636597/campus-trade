package com.campustrade.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationEvent {

    private String type;
    private Long fromUserId;
    private Long toUserId;
    private Long productId;
    private String content;
    private LocalDateTime timestamp;
}
