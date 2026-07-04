package com.campustrade.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "oss")
@Data
public class OssConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    @PostConstruct
    public void init() {
        log.info("OSS config loaded: endpoint={}, bucket={}, accessKey={}***",
                endpoint, bucket, accessKey != null ? accessKey.substring(0, Math.min(4, accessKey.length())) : "null");
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region("oss-cn-hangzhou")
                .build();
    }
}
