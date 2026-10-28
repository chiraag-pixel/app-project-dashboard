package com.skillgap.service;

import com.skillgap.config.AppProperties;
import com.skillgap.entity.User;
import com.skillgap.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded files in a private directory outside the web root. Only the
 * relative path is saved in the database; files are served back exclusively via
 * an authenticated controller that checks ownership.
 */
@Service
public class DocumentStorageService {

    private final Path root;

    public DocumentStorageService(AppProperties properties) {
        this.root = properties.uploadPath().toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    /** Saves the file under user-<id>/uuid.ext and returns the relative path. */
    public String store(User user, MultipartFile file, String extension) {
        try {
            Path userDir = root.resolve("user-" + user.getId());
            Files.createDirectories(userDir);
            String relative = "user-" + user.getId() + "/" + UUID.randomUUID() + "." + extension;
            Path target = root.resolve(relative).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("Invalid file path.");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relative;
        } catch (IOException e) {
            throw new BusinessException("Could not store the uploaded file: " + e.getMessage());
        }
    }

    /** Opens a stored file for an authenticated download. */
    public InputStream open(String relativePath) {
        try {
            Path path = root.resolve(relativePath).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                throw new BusinessException("Stored file is missing.");
            }
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new BusinessException("Could not read the stored file.");
        }
    }

    /** Best-effort delete (used when a document record is removed). */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path path = root.resolve(relativePath).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // best effort
        }
    }
}
