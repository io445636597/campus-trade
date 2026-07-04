package com.campustrade.service;

import com.campustrade.config.RabbitMQConfig;
import com.campustrade.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationConsumer {

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_QUEUE)
    public void handleMessageNotification(NotificationEvent event) {
        log.info("[Message Notification] From userId={} to userId={}, productId={}, content={}",
                event.getFromUserId(), event.getToUserId(), event.getProductId(), event.getContent());
        // Future: push notification, email, SMS, etc.
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKMARK_QUEUE)
    public void handleBookmarkNotification(NotificationEvent event) {
        log.info("[Bookmark Notification] From userId={} to userId={}, productId={}",
                event.getFromUserId(), event.getToUserId(), event.getProductId());
        // Future: push notification, etc.
    }
}
