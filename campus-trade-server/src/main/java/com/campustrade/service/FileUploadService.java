package com.campustrade.service;

import com.campustrade.common.BusinessException;
import com.campustrade.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final MinioClient minioClient;
    private final MinioConfig config;

    public FileUploadService(MinioClient minioClient, MinioConfig config) {
        this.minioClient = minioClient;
        this.config = config;
    }

    public String uploadImage(MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.getBucket()).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(config.getBucket()).build()
                );
            }

            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return config.getEndpoint() + "/" + config.getBucket() + "/" + filename;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage());
            throw new BusinessException(500, "图片上传失败");
        }
    }
}
