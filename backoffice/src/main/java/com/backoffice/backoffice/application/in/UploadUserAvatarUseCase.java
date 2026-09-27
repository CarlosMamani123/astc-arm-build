package com.backoffice.backoffice.application.in;

import java.io.InputStream;
import java.util.UUID;

/**
 * Puerto de entrada para subir avatar de usuario
 */
public interface UploadUserAvatarUseCase {

    /**
     * Sube la imagen del avatar a MinIO y retorna la ruta del objeto
     * @return Ruta del objeto en MinIO (ej: private/user/{userId}/photo/{uuid}.jpg)
     */
    String execute(UUID userId, InputStream file, Long fileSize, String originalFileName, String contentType);
}