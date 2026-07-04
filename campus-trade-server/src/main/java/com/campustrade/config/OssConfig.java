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
        log.info("OSS config: endpoint={}, bucket={}", endpoint, bucket);
    }

    @Bean
    public MinioClient minioClient() {
        // OSS requires virtual-hosted style: https://bucket.oss-cn-hangzhou.aliyuncs.com
        String vhostEndpoint = endpoint.replace("https://", "https://" + bucket + ".");
        log.info("OSS MinioClient endpoint: {}", vhostEndpoint);
        return MinioClient.builder()
                .endpoint(vhostEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
