package com.campustrade.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.campustrade.common.BusinessException;
import com.campustrade.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final OSS ossClient;
    private final OssConfig config;

    public FileUploadService(OSS ossClient, OssConfig config) {
        this.ossClient = ossClient;
        this.config = config;
    }

    public String uploadImage(MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            byte[] bytes = file.getBytes();
            ossClient.putObject(new PutObjectRequest(
                    config.getBucket(), filename,
                    new ByteArrayInputStream(bytes)));
            String url = "https://" + config.getBucket() + "." + config.getEndpoint().replace("https://", "") + "/" + filename;
            log.info("Image uploaded to OSS: {}", url);
            return url;
        } catch (Exception e) {
            log.error("OSS upload failed: {}", e.getMessage(), e);
            throw new BusinessException(500, "图片上传失败: " + e.getMessage());
        }
    }
}
