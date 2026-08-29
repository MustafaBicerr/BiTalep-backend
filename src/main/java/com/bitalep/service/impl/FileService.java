package com.bitalep.service.impl;

import com.bitalep.config.AppProperties;
import com.bitalep.dto.FileDtos;
import com.bitalep.dto.PaginationMeta;
import com.bitalep.entity.Application;
import com.bitalep.entity.Attachment;
import com.bitalep.entity.NotificationType;
import com.bitalep.exception.ApiException;
import com.bitalep.mapper.DtoMapper;
import com.bitalep.repository.ApplicationRepository;
import com.bitalep.repository.AttachmentRepository;
import com.bitalep.security.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final AttachmentRepository attachments;
    private final ApplicationRepository applications;
    private final NotificationService notifications;
    private final AppProperties props;

    public FileService(
            AttachmentRepository attachments,
            ApplicationRepository applications,
            NotificationService notifications,
            AppProperties props
    ) {
        this.attachments = attachments;
        this.applications = applications;
        this.notifications = notifications;
        this.props = props;
    }

    public record FilePage(List<FileDtos.AttachmentResponse> data, PaginationMeta meta) {}

    @Transactional
    public FileDtos.AttachmentResponse upload(MultipartFile file, UUID applicationId) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("errors:fileRequired");
        }
        if (file.getSize() > props.files().maxBytes()) {
            throw ApiException.validation("errors:fileTooLarge");
        }
        String mime = sniff(file);
        if (!ALLOWED.contains(mime)) {
            throw ApiException.validation("errors:fileType");
        }
        Application app = applications.findByIdAndTenantIdAndDeletedAtIsNull(applicationId, TenantContext.tenantId())
                .orElseThrow(ApiException::notFound);
        if (!TenantContext.isAdmin() && !app.getApplicantId().equals(TenantContext.userId())) {
            throw ApiException.notFound();
        }
        if (!TenantContext.isAdmin() && !FormService.personnelMutable(app.getStatus())) {
            throw ApiException.forbidden();
        }
        UUID id = UUID.randomUUID();
        String original = safeName(file.getOriginalFilename());
        String storedName = id + "_" + original;
        Path dir = Path.of(props.files().storagePath(), TenantContext.tenantId().toString(), applicationId.toString());
        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(storedName);
            file.transferTo(dest);
            Attachment att = new Attachment();
            att.setId(id);
            att.setTenantId(TenantContext.tenantId());
            att.setApplicationId(applicationId);
            att.setFileName(storedName);
            att.setOriginalName(original);
            att.setFilePath(TenantContext.tenantId() + "/" + applicationId + "/" + storedName);
            att.setFileSize(file.getSize());
            att.setMimeType(mime);
            att.setCreatedBy(TenantContext.userId());
            att.setUpdatedBy(TenantContext.userId());
            attachments.save(att);
            notifications.notifyAdmins(NotificationType.FILE_UPLOADED, "Dosya yüklendi", original, applicationId);
            return DtoMapper.attachment(att);
        } catch (IOException ex) {
            throw new IllegalStateException("file store failed", ex);
        }
    }

    public FileDtos.AttachmentResponse get(UUID id) {
        return DtoMapper.attachment(loadVisible(id));
    }

    public record FileContent(Resource resource, String mime, String downloadName) {}

    public FileContent content(UUID id) {
        Attachment att = loadVisible(id);
        Path path = Path.of(props.files().storagePath()).resolve(att.getFilePath()).normalize();
        Path root = Path.of(props.files().storagePath()).toAbsolutePath().normalize();
        if (!path.toAbsolutePath().normalize().startsWith(root)) {
            throw ApiException.notFound();
        }
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw ApiException.notFound();
        }
        return new FileContent(resource, att.getMimeType(), att.getOriginalName());
    }

    @Transactional
    public void delete(UUID id) {
        Attachment att = loadVisible(id);
        if (!TenantContext.isAdmin()) {
            Application app = applications.findByIdAndTenantIdAndDeletedAtIsNull(att.getApplicationId(), att.getTenantId())
                    .orElseThrow(ApiException::notFound);
            if (!FormService.personnelMutable(app.getStatus())) {
                throw ApiException.forbidden();
            }
        }
        att.setDeletedAt(Instant.now());
        att.setUpdatedBy(TenantContext.userId());
        attachments.save(att);
    }

    public FilePage listAll(int page, int pageSize) {
        int p = Math.max(page, 1);
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        UUID tenantId = TenantContext.tenantId();
        Specification<Attachment> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("tenantId"), tenantId));
            preds.add(cb.isNull(root.get("deletedAt")));
            if (!TenantContext.isAdmin()) {
                var sq = q.subquery(UUID.class);
                var app = sq.from(Application.class);
                sq.select(app.get("id")).where(
                        cb.equal(app.get("tenantId"), tenantId),
                        cb.equal(app.get("applicantId"), TenantContext.userId()),
                        cb.isNull(app.get("deletedAt"))
                );
                preds.add(root.get("applicationId").in(sq));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        Page<Attachment> result = attachments.findAll(
                spec, PageRequest.of(p - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new FilePage(
                result.getContent().stream().map(DtoMapper::attachment).toList(),
                PaginationMeta.of(p, size, result.getTotalElements())
        );
    }

    public MediaType mediaType(String mime) {
        try {
            return MediaType.parseMediaType(mime);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private Attachment loadVisible(UUID id) {
        Attachment att = attachments.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.tenantId())
                .orElseThrow(ApiException::notFound);
        if (TenantContext.isAdmin()) {
            return att;
        }
        Application app = applications.findByIdAndTenantIdAndDeletedAtIsNull(att.getApplicationId(), att.getTenantId())
                .orElseThrow(ApiException::notFound);
        if (!app.getApplicantId().equals(TenantContext.userId())) {
            throw ApiException.notFound();
        }
        return att;
    }

    private static String sniff(MultipartFile file) {
        String ct = file.getContentType();
        return ct == null ? "application/octet-stream" : ct.toLowerCase();
    }

    private static String safeName(String original) {
        String name = original == null ? "file" : original;
        name = name.replace("\\", "_").replace("/", "_").replace("..", "_");
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name.isBlank() ? "file" : name;
    }
}
