package com.bitalep.mapper;

import com.bitalep.dto.FileDtos;
import com.bitalep.dto.FormDtos;
import com.bitalep.dto.MiscDtos;
import com.bitalep.dto.UserDtos;
import com.bitalep.entity.AppNotification;
import com.bitalep.entity.AppUser;
import com.bitalep.entity.Application;
import com.bitalep.entity.ApplicationEvent;
import com.bitalep.entity.Attachment;
import com.bitalep.entity.Tenant;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DtoMapper {

    private DtoMapper() {}

    public static UserDtos.UserResponse user(AppUser u) {
        if (u == null) {
            return null;
        }
        return new UserDtos.UserResponse(
                u.getId(),
                u.getName(),
                u.getSurname(),
                u.getEmail(),
                u.getRole(),
                u.getDepartment(),
                u.getTenantId(),
                u.getCreatedAt()
        );
    }

    public static FileDtos.AttachmentResponse attachment(Attachment a) {
        return new FileDtos.AttachmentResponse(
                a.getId(),
                a.getFileName(),
                a.getOriginalName(),
                a.getFilePath(),
                a.getFileSize(),
                a.getMimeType(),
                a.getCreatedAt(),
                a.getApplicationId(),
                a.getTenantId()
        );
    }

    public static FormDtos.ApplicationResponse application(
            Application app,
            AppUser applicant,
            List<FileDtos.AttachmentResponse> attachments,
            List<FormDtos.TimelineEntry> timeline
    ) {
        return new FormDtos.ApplicationResponse(
                app.getId(),
                app.getTitle(),
                app.getDescription(),
                app.getFormType(),
                app.getFormType().displayName(),
                app.getStatus(),
                user(applicant),
                app.getApplicantId(),
                app.getTenantId(),
                app.getCreatedAt(),
                app.getUpdatedAt(),
                attachments,
                timeline,
                app.getRejectReason(),
                app.getUpdateReason()
        );
    }

    public static FormDtos.TimelineEntry timeline(ApplicationEvent ev, AppUser actor) {
        return new FormDtos.TimelineEntry(ev.getStatus(), ev.getCreatedAt(), user(actor), ev.getDescription());
    }

    public static MiscDtos.NotificationResponse notification(AppNotification n, AppUser actor) {
        return new MiscDtos.NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getDescription(),
                n.isRead(),
                n.getCreatedAt(),
                n.getRelatedRequestId(),
                user(actor)
        );
    }

    public static MiscDtos.CompanyResponse company(Tenant tenant, String plan, Boolean active, java.time.Instant periodEnd) {
        return new MiscDtos.CompanyResponse(
                tenant.getId(),
                tenant.getName(),
                plan,
                tenant.getCreatedAt(),
                active,
                periodEnd
        );
    }

    public static AppUser userFromMap(Map<UUID, AppUser> map, UUID id) {
        return id == null ? null : map.get(id);
    }
}
