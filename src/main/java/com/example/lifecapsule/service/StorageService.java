package com.example.lifecapsule.service;

import com.example.lifecapsule.errors.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class StorageService {
    private final Path storageRoot;

    public StorageService(@Value("${app.storage.root:./uploads}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Fayllar uchun papka yaratilmadi: " + this.storageRoot, e);
        }
    }

    public String store(Long familyId, Long personId, MultipartFile file) {
        return storeUnder(familyId + "/" + personId, file);
    }

    public String storeFamilyCover(Long familyId, MultipartFile file) {
        return storeUnder(familyId + "/cover", file);
    }

    public String storePersonAvatar(Long familyId, Long personId, MultipartFile file) {
        return storeUnder(familyId + "/" + personId + "/avatar", file);
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Resource resource = new UrlResource(resolve(relativePath).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Fayl topilmadi");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Fayl topilmadi");
        }
    }

    /**
     * Deletes the file only once the surrounding transaction commits, so a rolled-back delete
     * never leaves a database row pointing at a file that is already gone.
     */
    public void delete(String relativePath) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteNow(relativePath);
                }
            });
        } else {
            deleteNow(relativePath);
        }
    }

    private void deleteNow(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", relativePath, e);
        }
    }

    private String storeUnder(String directory, MultipartFile file) {
        String relativePath = directory + "/" + UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Fayl saqlanmadi", e);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) deleteNow(relativePath);
                }
            });
        }
        return relativePath;
    }

    private Path resolve(String relativePath) {
        Path file = storageRoot.resolve(relativePath).normalize();
        if (!file.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Fayl yo'li noto'g'ri");
        }
        return file;
    }

    private String extensionOf(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return "";
        }
        String extension = cleaned.substring(dot).toLowerCase();
        if (extension.length() > 10 || !extension.matches("\\.[a-z0-9]+")) {
            return "";
        }
        return extension;
    }
}
