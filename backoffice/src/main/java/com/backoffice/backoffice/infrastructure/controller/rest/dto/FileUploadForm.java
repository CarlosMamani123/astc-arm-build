package com.backoffice.backoffice.infrastructure.controller.rest.dto;

import jakarta.ws.rs.FormParam;

import java.io.InputStream;

public class FileUploadForm {

    @FormParam("file")
    public InputStream file;

    @FormParam("fileName")
    public String fileName;

    @FormParam("fileSize")
    public Long fileSize;

    @FormParam("contentType")
    public String contentType;
}