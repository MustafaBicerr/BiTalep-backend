package com.bitalep.dto;

import java.time.Instant;
import java.util.UUID;

public final class FileDtos {

    private FileDtos() {}

    public record AttachmentResponse(
            UUID id,
            String fileName,
            String originalName,
            String filePath,
            long fileSize,
            String mimeType,
            Instant uploadDate,
            UUID applicationId,
            UUID tenantId
    ) {}
}
