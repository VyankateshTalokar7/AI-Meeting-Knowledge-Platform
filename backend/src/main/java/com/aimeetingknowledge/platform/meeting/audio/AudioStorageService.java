package com.aimeetingknowledge.platform.meeting.audio;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AudioStorageService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "audio/mpeg", ".mp3",
            "audio/wav", ".wav",
            "audio/x-wav", ".wav",
            "audio/mp4", ".m4a",
            "audio/webm", ".webm"
    );

    private final StorageProperties storageProperties;

    public AudioStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public StoredAudioFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MissingAudioFileException();
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new AudioFileTooLargeException();
        }

        String contentType = normalizedContentType(file.getContentType());
        if (!storageProperties.allowedContentTypes().contains(contentType) || !EXTENSIONS.containsKey(contentType)) {
            throw new UnsupportedAudioTypeException();
        }

        Path storageDirectory = storageDirectory();
        String storedFilename = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path target = storageDirectory.resolve(storedFilename).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new AudioStorageException("Unable to store audio file.", null);
        }

        try {
            Files.createDirectories(storageDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredAudioFile(
                    sanitizedOriginalFilename(file.getOriginalFilename(), EXTENSIONS.get(contentType)),
                    storedFilename,
                    contentType,
                    file.getSize(),
                    target.toString()
            );
        } catch (IOException exception) {
            throw new AudioStorageException("Unable to store audio file.", exception);
        }
    }

    public void delete(String storedFilename) {
        Path storageDirectory = storageDirectory();
        Path target = storageDirectory.resolve(storedFilename).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new AudioStorageException("Unable to delete audio file.", null);
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new AudioStorageException("Unable to delete audio file.", exception);
        }
    }

    private Path storageDirectory() {
        return Path.of(storageProperties.audioDirectory()).toAbsolutePath().normalize();
    }

    private String normalizedContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizedOriginalFilename(String originalFilename, String extension) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "audio" + extension;
        }
        String fileName = Path.of(originalFilename.replace('\\', '/')).getFileName().toString()
                .replaceAll("[\\p{Cntrl}]", "");
        if (fileName.isBlank()) {
            return "audio" + extension;
        }
        return fileName.length() > 255 ? fileName.substring(0, 255) : fileName;
    }

    public record StoredAudioFile(
            String originalFilename,
            String storedFilename,
            String contentType,
            long fileSize,
            String storagePath
    ) {
    }
}
