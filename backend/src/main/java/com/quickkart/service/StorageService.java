package com.quickkart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickkart.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

@Service
public class StorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/avif"
    );

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final Path localUploadPath;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StorageService(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret,
            @Value("${app.storage.local-upload-dir:uploads}") String localUploadDir,
            ObjectMapper objectMapper) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.localUploadPath = Paths.get(localUploadDir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    public record UploadResult(String url, String publicId, String provider) {}

    public UploadResult uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file cannot be empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image format. Allowed formats: JPEG, PNG, WebP, AVIF.");
        }

        if (isCloudinaryConfigured()) {
            return uploadToCloudinary(file, folder);
        }
        return saveLocally(file, folder);
    }

    public boolean isCloudinaryConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    private UploadResult uploadToCloudinary(MultipartFile file, String folder) {
        try {
            long timestamp = System.currentTimeMillis() / 1000L;
            String targetFolder = "quickkart/" + (folder != null && !folder.isBlank() ? folder : "general");
            String toSign = "folder=" + targetFolder + "&timestamp=" + timestamp + apiSecret;
            String signature = sha1Hex(toSign);

            String boundary = "----QuickKartBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.jpg";

            StringBuilder prefix = new StringBuilder();
            appendFormField(prefix, boundary, "api_key", apiKey);
            appendFormField(prefix, boundary, "timestamp", String.valueOf(timestamp));
            appendFormField(prefix, boundary, "folder", targetFolder);
            appendFormField(prefix, boundary, "signature", signature);
            prefix.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n")
                    .append("Content-Type: ").append(file.getContentType()).append("\r\n\r\n");

            byte[] prefixBytes = prefix.toString().getBytes(StandardCharsets.UTF_8);
            byte[] suffixBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
            System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
            System.arraycopy(fileBytes, 0, body, prefixBytes.length, fileBytes.length);
            System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileBytes.length, suffixBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("Cloudinary upload failed: " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return new UploadResult(root.get("secure_url").asText(), root.get("public_id").asText(), "cloudinary");
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to upload image to Cloudinary: " + ex.getMessage());
        }
    }

    private UploadResult saveLocally(MultipartFile file, String folder) {
        try {
            String safeFolder = (folder != null && !folder.isBlank()) ? folder.replaceAll("[^a-zA-Z0-9_-]", "") : "general";
            Path targetDir = localUploadPath.resolve(safeFolder);
            Files.createDirectories(targetDir);

            String ext = getExtension(file.getOriginalFilename(), file.getContentType());
            String fileName = UUID.randomUUID() + ext;
            Path targetFile = targetDir.resolve(fileName);
            Files.write(targetFile, file.getBytes());

            String url = "/uploads/" + safeFolder + "/" + fileName;
            return new UploadResult(url, safeFolder + "/" + fileName, "local");
        } catch (IOException ex) {
            throw new BadRequestException("Failed to save uploaded image: " + ex.getMessage());
        }
    }

    private void appendFormField(StringBuilder sb, String boundary, String name, String value) {
        sb.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                .append(value).append("\r\n");
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (List.of(".jpg", ".jpeg", ".png", ".webp", ".avif").contains(ext)) {
                return ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/avif" -> ".avif";
            default -> ".jpg";
        };
    }

    private String sha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
