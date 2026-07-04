package com.campustrade.service;

import com.campustrade.entity.Message;

import java.util.List;

public interface MessageService {

    /**
     * Get messages for a product
     */
    List<Message> getProductMessages(Long productId);

    /**
     * Create a message on a product
     */
    Message createMessage(Long productId, String content);

    /**
     * Delete a message (message author or product author)
     */
    void deleteMessage(Long messageId);
}
