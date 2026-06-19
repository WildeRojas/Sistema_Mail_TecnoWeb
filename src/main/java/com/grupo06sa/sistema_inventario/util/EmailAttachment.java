package com.grupo06sa.sistema_inventario.util;

public class EmailAttachment {
    private final String fileName;
    private final String contentType;
    private final byte[] data;

    public EmailAttachment(String fileName, String contentType, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
