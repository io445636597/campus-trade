package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.common.BusinessException;
import com.campustrade.config.RabbitMQConfig;
import com.campustrade.dto.NotificationEvent;
import com.campustrade.entity.Message;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.mapper.MessageMapper;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.LoginUser;
import com.campustrade.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final RabbitTemplate rabbitTemplate;

    public MessageServiceImpl(MessageMapper messageMapper,
                               UserMapper userMapper,
                               ProductMapper productMapper,
                               RabbitTemplate rabbitTemplate) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public List<Message> getProductMessages(Long productId) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getProductId, productId)
                        .orderByAsc(Message::getCreatedAt));

        return messages.stream().peek(m -> {
            User user = userMapper.selectById(m.getUserId());
            if (user != null) {
                m.setUser(user);
            }
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Message createMessage(Long productId, String content) {
        Long userId = LoginUser.getRequiredUserId();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        Message message = new Message();
        message.setProductId(productId);
        message.setUserId(userId);
        message.setContent(content);
        messageMapper.insert(message);

        User author = userMapper.selectById(userId);
        if (author != null) {
            message.setUser(author);
        }

        try {
            NotificationEvent event = new NotificationEvent();
            event.setType("MESSAGE");
            event.setFromUserId(userId);
            event.setToUserId(product.getUserId());
            event.setProductId(productId);
            event.setContent(content);
            event.setTimestamp(LocalDateTime.now());
            rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE, RabbitMQConfig.MESSAGE_KEY, event);
        } catch (Exception e) {
            log.warn("Failed to send message notification: {}", e.getMessage());
        }

        return message;
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId) {
        Long userId = LoginUser.getRequiredUserId();

        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "留言不存在");
        }

        Product product = productMapper.selectById(message.getProductId());
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        if (!message.getUserId().equals(userId) && !product.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除此留言");
        }

        messageMapper.deleteById(messageId);
    }
}
