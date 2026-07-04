package com.campustrade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.campustrade.repository")
public class ESConfig {
    // Spring Boot auto-configures ElasticsearchClient and ElasticsearchOperations
    // via spring.elasticsearch.* properties in application.yml
}
