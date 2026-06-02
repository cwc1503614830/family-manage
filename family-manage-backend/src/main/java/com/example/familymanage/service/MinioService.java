package com.example.familymanage.service;

import com.example.familymanage.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private volatile boolean bucketReady;

    public MinioService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public Map<String, String> uploadImage(MultipartFile file, String folder) {
        ensureBucket();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("仅支持上传图片文件");
        }
        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String objectName = folder + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("文件上传失败：" + exception.getMessage(), exception);
        }
        Map<String, String> result = new HashMap<String, String>();
        result.put("url", buildFileUrl(objectName));
        result.put("objectName", objectName);
        return result;
    }

    public void writeObject(String objectName, HttpServletResponse response) throws IOException {
        String safeObjectName;
        try {
            safeObjectName = validateObjectName(objectName);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(safeObjectName)
                .build())) {
            response.setContentType(resolveContentType(safeObjectName));
            response.setHeader("Cache-Control", "public, max-age=86400");
            StreamUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    public String normalizePublicUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("/api/files/view/")) {
            return trimmed;
        }
        String objectName = extractObjectName(trimmed);
        if (!StringUtils.hasText(objectName)) {
            return trimmed;
        }
        return buildProxyUrl(objectName);
    }

    public String extractObjectNameFromRequestPath(String requestPath) {
        if (!StringUtils.hasText(requestPath)) {
            return "";
        }
        int index = requestPath.indexOf("/view/");
        if (index < 0) {
            return "";
        }
        String encoded = requestPath.substring(index + "/view/".length());
        try {
            return validateObjectName(URLDecoder.decode(encoded, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalArgumentException("非法文件路径");
        }
    }

    private synchronized void ensureBucket() {
        if (bucketReady) {
            return;
        }
        try {
            String bucket = properties.getBucket();
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            String policy = String.format(
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
                            + "\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                    bucket);
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            bucketReady = true;
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 初始化失败，请确认 API 端口配置正确（通常为 9000）：" + exception.getMessage(), exception);
        }
    }

    private String buildFileUrl(String objectName) {
        return buildProxyUrl(objectName);
    }

    private String buildProxyUrl(String objectName) {
        return "/api/files/view/" + objectName;
    }

    private String extractObjectName(String url) {
        if (url.contains("/api/files/view/")) {
            return url.substring(url.indexOf("/api/files/view/") + "/api/files/view/".length());
        }
        String bucketPrefix = "/" + properties.getBucket() + "/";
        int bucketIndex = url.indexOf(bucketPrefix);
        if (bucketIndex >= 0) {
            return url.substring(bucketIndex + bucketPrefix.length());
        }
        return "";
    }

    private String validateObjectName(String objectName) {
        if (!StringUtils.hasText(objectName) || objectName.contains("..")) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return objectName.trim();
    }

    private String resolveContentType(String objectName) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build()).contentType();
        } catch (Exception exception) {
            String lower = objectName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) {
                return "image/png";
            }
            if (lower.endsWith(".gif")) {
                return "image/gif";
            }
            if (lower.endsWith(".webp")) {
                return "image/webp";
            }
            return "image/jpeg";
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return ".gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
