package com.grupo06sa.sistema_inventario.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/qr")
public class QrController {

    // Almacén temporal en memoria: token → base64
    private static final Map<String, String> qrStore = new ConcurrentHashMap<>();

    public static String storeQr(String qrBase64) {
        String token = UUID.randomUUID().toString().replace("-", "");
        qrStore.put(token, qrBase64);
        return token;
    }

    @GetMapping("/{token}.png")
    public ResponseEntity<byte[]> getQr(@PathVariable String token) {
        String qrBase64 = qrStore.get(token);
        if (qrBase64 == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageBytes = Base64.getDecoder().decode(qrBase64);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        // Eliminar después de servir (uso único)
        qrStore.remove(token);
        return ResponseEntity.ok().headers(headers).body(imageBytes);
    }
}
