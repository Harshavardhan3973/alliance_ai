package com.alliance.quizmanagement.service;

import com.alliance.quizmanagement.exception.StorageException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for local file-based JSON persistence.
 * Provides atomic file writes and backup preservation to prevent data corruption.
 */
public class FileStorageService {
    private final File dataDir;
    private final ObjectMapper objectMapper;

    public FileStorageService() {
        this(new File("data"));
    }

    public FileStorageService(File dataDir) {
        this.dataDir = dataDir;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> List<T> loadList(String filename, TypeReference<List<T>> typeRef) throws StorageException {
        File file = new File(dataDir, filename);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(file, typeRef);
        } catch (IOException e) {
            throw new StorageException("Failed to read storage file: " + file.getAbsolutePath(), e);
        }
    }

    public synchronized <T> void saveList(String filename, List<T> list) throws StorageException {
        File targetFile = new File(dataDir, filename);
        File tempFile = new File(dataDir, filename + ".tmp");
        File backupFile = new File(dataDir, filename + ".bak");

        try {
            // Write to temporary file first
            objectMapper.writeValue(tempFile, list);

            // Create backup if target file exists
            if (targetFile.exists()) {
                Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Atomically move temp file to target
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Clean up temporary file on failure
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new StorageException("Failed to save data list to file: " + filename, e);
        }
    }

    public File getDataDir() {
        return dataDir;
    }
}
