package com.backoffice.backoffice.infrastructure.controller.rest;

import com.backoffice.backoffice.application.in.DeleteUserAvatarUseCase;
import com.backoffice.backoffice.application.in.GetUserAvatarUseCase;
import com.backoffice.backoffice.application.in.UploadUserAvatarUseCase;
import com.backoffice.backoffice.infrastructure.controller.rest.dto.FileUploadForm;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.MultipartForm;

import java.util.UUID;

@Path("/rest/files")
public class FileUploadResource {

    @Inject
    UploadUserAvatarUseCase uploadUseCase;

    @Inject
    DeleteUserAvatarUseCase deleteUserAvatarUseCase;

    @Inject
    GetUserAvatarUseCase getUserAvatarUseCase;

    // ====================== UPLOAD ======================
    @POST
    @Path("/avatar/{userId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadAvatar(
            @PathParam("userId") UUID userId,
            @MultipartForm FileUploadForm form
    ) {

        try {
            if (form == null || form.file == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Archivo obligatorio\"}")
                        .build();
            }

            String objectPath = uploadUseCase.execute(
                    userId,
                    form.file,
                    form.fileSize,
                    form.fileName,
                    form.contentType
            );

            return Response.ok("{\"path\":\"" + objectPath + "\"}")
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Error subiendo avatar\"}")
                    .build();
        }
    }

    // ====================== DELETE ======================
    @DELETE
    @Path("/avatar/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAvatar(@PathParam("userId") UUID userId) {

        try {
            deleteUserAvatarUseCase.execute(userId);

            return Response.ok("{\"message\":\"Avatar eliminado correctamente\"}")
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Error eliminando avatar\"}")
                    .build();
        }
    }

    // ====================== GET AVATAR ======================
    @GET
    @Path("/avatar/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvatar(@PathParam("userId") UUID userId) {

        try {
            String presignedUrl = getUserAvatarUseCase.execute(userId);

            return Response.ok("{\"url\":\"" + presignedUrl + "\"}")
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Error obteniendo avatar\"}")
                    .build();
        }
    }
}