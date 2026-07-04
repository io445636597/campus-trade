package com.campustrade.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }
}
