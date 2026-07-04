package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.MessageRequest;
import com.campustrade.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/api/product/{id}/message")
    public Result<?> getMessages(@PathVariable Long id) {
        return Result.success(messageService.getProductMessages(id));
    }

    @PostMapping("/api/product/{id}/message")
    public Result<?> createMessage(@PathVariable Long id,
                                   @Valid @RequestBody MessageRequest request) {
        return Result.success(messageService.createMessage(id, request.getContent()));
    }

    @DeleteMapping("/api/message/{id}")
    public Result<?> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return Result.success();
    }
}
