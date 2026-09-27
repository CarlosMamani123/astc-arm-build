package com.backoffice.backoffice.infrastructure.sender;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;

@ApplicationScoped
public class StorageAdapter {

    private final S3Service s3Service;

    @Inject
    public StorageAdapter(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public void uploadAvatar(String objectName, InputStream data, long size, String contentType) {
        // 🔥 eliminar antes (opcional pero recomendado)
        s3Service.deleteObject(objectName);

        s3Service.uploadObject(objectName, data, size, contentType);
    }

    public String getAvatarPresignedUrl(String objectName) {
        return s3Service.generatePresignedUrl(objectName, 60); // 60 minutos (ajusta según necesites)
    }

    public void deleteAvatar(String objectName) {
        s3Service.deleteObject(objectName);
    }
}