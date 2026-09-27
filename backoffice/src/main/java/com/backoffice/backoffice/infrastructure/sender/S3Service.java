package com.backoffice.backoffice.infrastructure.sender;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@ApplicationScoped
public class S3Service {

    private final S3Client s3;
    private final S3Presigner presigner;

    @ConfigProperty(name = "storage.bucket.name")
    String bucketName;

    public S3Service(
            @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id") String accessKey,
            @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key") String secretKey,
            @ConfigProperty(name = "quarkus.s3.endpoint-override") String endpoint,
            @ConfigProperty(name = "quarkus.s3.aws.region") String region,
            @ConfigProperty(name = "storage.public-endpoint", defaultValue = "") String publicEndpoint
    ) {
        String effectivePublicEndpoint = (publicEndpoint != null && !publicEndpoint.isBlank())
                ? publicEndpoint : endpoint;

        System.out.println("[S3Service] Init -> internal endpoint=" + endpoint
                + ", publicEndpoint=" + publicEndpoint
                + ", effectivePresignEndpoint=" + effectivePublicEndpoint);

        // Cliente S3 para upload y delete
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .forcePathStyle(true)
                .build();

        // Presigner MUST use public/browser-accessible endpoint
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(effectivePublicEndpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .region(Region.of(region))
                .build();
    }

    public void uploadObject(String objectName, InputStream data, long size, String contentType) {
        try {
            byte[] bytes = data.readAllBytes();

            s3.putObject(
                b -> b.bucket(bucketName)
                      .key(objectName)
                      .contentType(contentType != null ? contentType : "image/jpeg"),
                RequestBody.fromBytes(bytes)
            );

            System.out.println("✅ Archivo subido correctamente: " + objectName + " (" + bytes.length + " bytes)");

        } catch (Exception e) {
            throw new RuntimeException("❌ Error al subir a S3: " + objectName, e);
        }
    }

    public void deleteObject(String objectName) {
        try {
            s3.deleteObject(b -> b.bucket(bucketName).key(objectName));
            System.out.println("🗑️ Archivo eliminado: " + objectName);
        } catch (Exception e) {
            System.out.println("⚠️ No se pudo eliminar: " + objectName);
        }
    }

    // 🔥 Presigned URL para mostrar la imagen en React
    public String generatePresignedUrl(String objectName, int expiryMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            String url = presigner.presignGetObject(presignRequest).url().toString();

            url = normalizeMinioUrl(url);

            System.out.println("[S3Service] Generated presigned URL -> key=" + objectName + ", expiryMinutes=" + expiryMinutes + ", url=" + url);
            return url;

        } catch (Exception e) {
            throw new RuntimeException("❌ Error generando presigned URL para: " + objectName, e);
        }
    }

    private String normalizeMinioUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host == null) return url;
            String expectedPrefix = bucketName + ".";
            if (host.startsWith(expectedPrefix)) {
                String actualHost = host.substring(expectedPrefix.length());
                String newPath = "/" + bucketName + uri.getRawPath();
                StringBuilder sb = new StringBuilder();
                sb.append(uri.getScheme()).append("://").append(actualHost);
                if (uri.getPort() > 0) sb.append(":").append(uri.getPort());
                sb.append(newPath);
                if (uri.getRawQuery() != null) sb.append("?").append(uri.getRawQuery());
                System.out.println("[S3Service] Normalized virtual-host-style URL: " + url + " -> " + sb);
                return sb.toString();
            }
        } catch (Exception e) {
            System.out.println("[S3Service] Warning: error normalizing URL: " + e.getMessage());
        }
        return url;
    }
}