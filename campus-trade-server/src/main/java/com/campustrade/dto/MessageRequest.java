package com.campustrade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {

    @NotBlank(message = "留言内容不能为空")
    private String content;
}
