package com.example.lifecapsule.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

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
        String extension = extensionOf(file.getOriginalFilename());
        String relativePath = familyId + "/" + personId + "/" + UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(relativePath).normalize();

        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Fayl nomi noto'g'ri");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Fayl saqlanmadi", e);
        }

        return relativePath;
    }

    public String storeFamilyCover(Long familyId, MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        String relativePath = familyId + "/cover/" + UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(relativePath).normalize();

        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Fayl nomi noto'g'ri");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Fayl saqlanmadi", e);
        }

        return relativePath;
    }

    public String storePersonAvatar(Long familyId, Long personId, MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        String relativePath = familyId + "/" + personId + "/avatar/" + UUID.randomUUID() + extension;
        Path target = storageRoot.resolve(relativePath).normalize();

        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Fayl nomi noto'g'ri");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Fayl saqlanmadi", e);
        }

        return relativePath;
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = resolveExisting(relativePath);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Fayl topilmadi: " + relativePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Fayl topilmadi: " + relativePath, e);
        }
    }

    public void delete(String relativePath) {
        try {
            Path file = resolveExisting(relativePath);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Fayl o'chirilmadi", e);
        }
    }

    private Path resolveExisting(String relativePath) {
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
