package com.campustrade.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "campustrade.topic";
    public static final String MESSAGE_QUEUE = "campustrade.message.queue";
    public static final String BOOKMARK_QUEUE = "campustrade.bookmark.queue";
    public static final String MESSAGE_KEY = "message.notify";
    public static final String BOOKMARK_KEY = "bookmark.notify";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public Queue messageQueue() {
        return new Queue(MESSAGE_QUEUE, true);
    }

    @Bean
    public Queue bookmarkQueue() {
        return new Queue(BOOKMARK_QUEUE, true);
    }

    @Bean
    public Binding messageBinding() {
        return BindingBuilder.bind(messageQueue()).to(topicExchange()).with(MESSAGE_KEY);
    }

    @Bean
    public Binding bookmarkBinding() {
        return BindingBuilder.bind(bookmarkQueue()).to(topicExchange()).with(BOOKMARK_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
