package com.campustrade.service;

import com.campustrade.common.BusinessException;
import com.campustrade.config.OssConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final MinioClient client;
    private final OssConfig config;

    public FileUploadService(MinioClient client, OssConfig config) {
        this.client = client;
        this.config = config;
    }

    public String uploadImage(MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            // URL: https://bucket.oss-cn-hangzhou.aliyuncs.com/filename
            String url = config.getEndpoint().replace("https://", "https://" + config.getBucket() + ".") + "/" + filename;
            log.info("Image uploaded to OSS: {}", url);
            return url;
        } catch (Exception e) {
            log.error("OSS upload failed: {} | endpoint={} bucket={}",
                    e.getMessage(), config.getEndpoint(), config.getBucket(), e);
            throw new BusinessException(500, "图片上传失败: " + e.getMessage());
        }
    }
}
