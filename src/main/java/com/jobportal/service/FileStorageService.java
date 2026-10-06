package com.jobportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String save(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!(original.endsWith(".pdf") || original.endsWith(".doc") || original.endsWith(".docx"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, DOC or DOCX files are allowed");
        }
        String ext = original.substring(original.lastIndexOf('.'));
        String name = userId + "_" + UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
        return name;
    }

    public Resource load(String name) {
        Path p = root.resolve(name).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new FileSystemResource(p);
    }
}