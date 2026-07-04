package com.campustrade.service;

import com.campustrade.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final MinioClient minioClient;
    private final String bucket;
    private final String endpoint;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:}")
    private String baseUrl;

    public FileUploadService(MinioClient minioClient,
                             @Value("${minio.bucket:campustrade}") String bucket,
                             @Value("${minio.endpoint:http://localhost:9000}") String endpoint) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    public String uploadImage(MultipartFile file) {
        try {
            return uploadToMinIO(file);
        } catch (Exception minioEx) {
            log.warn("MinIO/OSS failed ({}), falling back to local", minioEx.getMessage());
            return uploadToLocal(file);
        }
    }

    private String uploadToMinIO(MultipartFile file) throws Exception {
        // Skip auto-create for OSS (bucket names must be globally unique)
        if (isLikelyOSS()) {
            log.info("OSS mode detected, bucket must already exist: {}", bucket);
        } else {
            try {
                boolean found = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build()
                );
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
            } catch (Exception e) {
                log.warn("Bucket check failed, continuing anyway: {}", e.getMessage());
            }
        }

        String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(filename)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // OSS virtual-hosted URL: https://bucket.endpoint/filename
        // MinIO path-style URL: endpoint/bucket/filename
        String url;
        if (isLikelyOSS()) {
            url = endpoint.replace("https://", "https://" + bucket + ".") + "/" + filename;
        } else {
            url = endpoint + "/" + bucket + "/" + filename;
        }
        log.info("Image uploaded: {}", url);
        return url;
    }

    private String uploadToLocal(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String filename = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());
            log.info("File saved locally: {}", filePath);
            String relativeUrl = "/uploads/" + filename;
            return baseUrl.isEmpty() ? relativeUrl : baseUrl + relativeUrl;
        } catch (Exception e) {
            log.error("Local upload failed", e);
            throw new BusinessException(500, "图片上传失败");
        }
    }

    private boolean isLikelyOSS() {
        return endpoint.contains("aliyuncs.com");
    }
}
