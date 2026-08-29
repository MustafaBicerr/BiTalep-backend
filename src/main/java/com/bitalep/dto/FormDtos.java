package com.bitalep.dto;

import com.bitalep.entity.Department;
import com.bitalep.entity.FormType;
import com.bitalep.entity.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FormDtos {

    private FormDtos() {}

    public record ApplicationResponse(
            UUID id,
            String title,
            String description,
            FormType formType,
            String formTypeName,
            RequestStatus status,
            UserDtos.UserResponse applicant,
            UUID applicantId,
            UUID tenantId,
            Instant createdDate,
            Instant updatedDate,
            List<FileDtos.AttachmentResponse> attachments,
            List<TimelineEntry> timeline
    ) {}

    public record TimelineEntry(
            RequestStatus status,
            Instant date,
            UserDtos.UserResponse actor,
            String description
    ) {}

    public record CreateApplicationRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 1000) String description,
            FormType formType
    ) {}

    public record UpdateApplicationRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 1000) String description,
            FormType formType
    ) {}

    public record RejectRequest(@Size(max = 1000) String reason) {}
}
