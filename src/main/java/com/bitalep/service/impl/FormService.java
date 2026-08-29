package com.bitalep.service.impl;

import com.bitalep.dto.FileDtos;
import com.bitalep.dto.FormDtos;
import com.bitalep.dto.PaginationMeta;
import com.bitalep.entity.AppUser;
import com.bitalep.entity.Application;
import com.bitalep.entity.ApplicationEvent;
import com.bitalep.entity.Attachment;
import com.bitalep.entity.Department;
import com.bitalep.entity.FormType;
import com.bitalep.entity.NotificationType;
import com.bitalep.entity.RequestStatus;
import com.bitalep.exception.ApiException;
import com.bitalep.mapper.DtoMapper;
import com.bitalep.repository.ApplicationEventRepository;
import com.bitalep.repository.ApplicationRepository;
import com.bitalep.repository.AttachmentRepository;
import com.bitalep.repository.UserRepository;
import com.bitalep.security.TenantContext;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FormService {

    private static final Map<RequestStatus, Set<RequestStatus>> TRANSITIONS = Map.of(
            RequestStatus.NEW, Set.of(RequestStatus.IN_REVIEW, RequestStatus.CANCELLED),
            RequestStatus.IN_REVIEW, Set.of(RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED),
            RequestStatus.APPROVED, Set.of(),
            RequestStatus.REJECTED, Set.of(),
            RequestStatus.CANCELLED, Set.of()
    );

    private final ApplicationRepository applications;
    private final ApplicationEventRepository events;
    private final AttachmentRepository attachments;
    private final UserRepository users;
    private final NotificationService notifications;

    public FormService(
            ApplicationRepository applications,
            ApplicationEventRepository events,
            AttachmentRepository attachments,
            UserRepository users,
            NotificationService notifications
    ) {
        this.applications = applications;
        this.events = events;
        this.attachments = attachments;
        this.users = users;
        this.notifications = notifications;
    }

    public record FormPage(List<FormDtos.ApplicationResponse> data, PaginationMeta meta) {}

    public FormPage list(
            int page,
            int pageSize,
            String sortBy,
            String sortOrder,
            List<RequestStatus> status,
            List<FormType> formType,
            Instant dateFrom,
            Instant dateTo,
            String keyword,
            UUID applicantId,
            List<Department> department,
            Boolean hasAttachments,
            Instant updatedBefore
    ) {
        UUID tenantId = TenantContext.tenantId();
        int p = Math.max(page, 1);
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        Specification<Application> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("tenantId"), tenantId));
            preds.add(cb.isNull(root.get("deletedAt")));
            if (!TenantContext.isAdmin()) {
                preds.add(cb.equal(root.get("applicantId"), TenantContext.userId()));
            } else if (applicantId != null) {
                preds.add(cb.equal(root.get("applicantId"), applicantId));
            }
            if (status != null && !status.isEmpty()) {
                preds.add(root.get("status").in(status));
            }
            if (formType != null && !formType.isEmpty()) {
                preds.add(root.get("formType").in(formType));
            }
            if (dateFrom != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (updatedBefore != null) {
                preds.add(cb.lessThan(root.get("updatedAt"), updatedBefore));
            }
            if (TenantContext.isAdmin() && department != null && !department.isEmpty()) {
                Subquery<UUID> sq = q.subquery(UUID.class);
                Root<AppUser> u = sq.from(AppUser.class);
                sq.select(u.get("id")).where(
                        cb.equal(u.get("tenantId"), tenantId),
                        u.get("department").in(department),
                        cb.isNull(u.get("deletedAt"))
                );
                preds.add(root.get("applicantId").in(sq));
            }
            if (hasAttachments != null) {
                Subquery<Long> sq = q.subquery(Long.class);
                Root<Attachment> a = sq.from(Attachment.class);
                sq.select(cb.literal(1L)).where(
                        cb.equal(a.get("applicationId"), root.get("id")),
                        cb.equal(a.get("tenantId"), tenantId),
                        cb.isNull(a.get("deletedAt"))
                );
                if (hasAttachments) {
                    preds.add(cb.exists(sq));
                } else {
                    preds.add(cb.not(cb.exists(sq)));
                }
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        Sort sort = Sort.by(sortDir(sortOrder), mapSort(sortBy));
        Page<Application> result = applications.findAll(spec, PageRequest.of(p - 1, size, sort));
        List<FormDtos.ApplicationResponse> data = result.getContent().stream()
                .map(app -> toDto(app, false, false))
                .toList();
        return new FormPage(data, PaginationMeta.of(p, size, result.getTotalElements()));
    }

    public FormDtos.ApplicationResponse get(UUID id) {
        return toDto(loadVisible(id), true, true);
    }

    @Transactional
    public FormDtos.ApplicationResponse create(FormDtos.CreateApplicationRequest req) {
        if (req.formType() == null) {
            throw ApiException.validation("errors:validation");
        }
        Application app = new Application();
        app.setTenantId(TenantContext.tenantId());
        app.setApplicantId(TenantContext.userId());
        app.setTitle(req.title().trim());
        app.setDescription(req.description().trim());
        app.setFormType(req.formType());
        app.setStatus(RequestStatus.NEW);
        app.setCreatedBy(TenantContext.userId());
        app.setUpdatedBy(TenantContext.userId());
        applications.save(app);
        addEvent(app, RequestStatus.NEW, "Talep oluşturuldu");
        notifications.notifyAdmins(NotificationType.NEW_REQUEST, "Yeni talep", app.getTitle(), app.getId());
        return toDto(app, true, true);
    }

    @Transactional
    public FormDtos.ApplicationResponse update(UUID id, FormDtos.UpdateApplicationRequest req) {
        Application app = loadVisible(id);
        assertCanEdit(app);
        if (req.formType() == null) {
            throw ApiException.validation("errors:validation");
        }
        app.setTitle(req.title().trim());
        app.setDescription(req.description().trim());
        app.setFormType(req.formType());
        app.setUpdatedBy(TenantContext.userId());
        applications.save(app);
        return toDto(app, true, true);
    }

    @Transactional
    public void delete(UUID id) {
        Application app = loadVisible(id);
        assertCanEdit(app);
        app.setDeletedAt(Instant.now());
        app.setUpdatedBy(TenantContext.userId());
        applications.save(app);
    }

    @Transactional
    public FormDtos.ApplicationResponse review(UUID id) {
        return transition(id, RequestStatus.IN_REVIEW, "İncelemeye alındı", NotificationType.STATUS_CHANGE);
    }

    @Transactional
    public FormDtos.ApplicationResponse approve(UUID id) {
        return transition(id, RequestStatus.APPROVED, "Onaylandı", NotificationType.APPROVED);
    }

    @Transactional
    public FormDtos.ApplicationResponse reject(UUID id, String reason) {
        Application app = loadVisible(id);
        requireAdmin();
        assertTransition(app.getStatus(), RequestStatus.REJECTED);
        app.setRejectReason(reason);
        app.setStatus(RequestStatus.REJECTED);
        app.setUpdatedBy(TenantContext.userId());
        applications.save(app);
        String desc = reason == null || reason.isBlank() ? "Reddedildi" : reason;
        addEvent(app, RequestStatus.REJECTED, desc);
        notifications.notifyUser(app.getApplicantId(), NotificationType.REJECTED, "Talep reddedildi", app.getTitle(), app.getId());
        return toDto(app, true, true);
    }

    public List<FileDtos.AttachmentResponse> files(UUID applicationId) {
        loadVisible(applicationId);
        return attachments.findByTenantIdAndApplicationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                        TenantContext.tenantId(), applicationId)
                .stream()
                .map(DtoMapper::attachment)
                .toList();
    }

    private FormDtos.ApplicationResponse transition(UUID id, RequestStatus next, String description, NotificationType type) {
        Application app = loadVisible(id);
        requireAdmin();
        assertTransition(app.getStatus(), next);
        app.setStatus(next);
        app.setUpdatedBy(TenantContext.userId());
        applications.save(app);
        addEvent(app, next, description);
        notifications.notifyUser(app.getApplicantId(), type, description, app.getTitle(), app.getId());
        return toDto(app, true, true);
    }

    private void addEvent(Application app, RequestStatus status, String description) {
        ApplicationEvent ev = new ApplicationEvent();
        ev.setTenantId(app.getTenantId());
        ev.setApplicationId(app.getId());
        ev.setStatus(status);
        ev.setDescription(description);
        ev.setActorId(TenantContext.userIdOrNull());
        ev.setCreatedBy(TenantContext.userIdOrNull());
        ev.setUpdatedBy(TenantContext.userIdOrNull());
        events.save(ev);
    }

    private Application loadVisible(UUID id) {
        Application app = applications.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.tenantId())
                .orElseThrow(ApiException::notFound);
        if (!TenantContext.isAdmin() && !app.getApplicantId().equals(TenantContext.userId())) {
            throw ApiException.notFound();
        }
        return app;
    }

    private void assertCanEdit(Application app) {
        if (TenantContext.isAdmin()) {
            return;
        }
        if (!app.getApplicantId().equals(TenantContext.userId()) || app.getStatus() != RequestStatus.NEW) {
            throw ApiException.forbidden();
        }
    }

    private static void requireAdmin() {
        if (!TenantContext.isAdmin()) {
            throw ApiException.forbidden();
        }
    }

    private static void assertTransition(RequestStatus from, RequestStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw ApiException.conflict();
        }
    }

    FormDtos.ApplicationResponse toDto(Application app, boolean withFiles, boolean withTimeline) {
        AppUser applicant = users.findByIdAndTenantIdAndDeletedAtIsNull(app.getApplicantId(), app.getTenantId()).orElse(null);
        List<FileDtos.AttachmentResponse> files = withFiles
                ? attachments.findByTenantIdAndApplicationIdAndDeletedAtIsNullOrderByCreatedAtAsc(app.getTenantId(), app.getId())
                .stream().map(DtoMapper::attachment).toList()
                : null;
        List<FormDtos.TimelineEntry> timeline = null;
        if (withTimeline) {
            List<ApplicationEvent> evs = events.findByTenantIdAndApplicationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                    app.getTenantId(), app.getId());
            Map<UUID, AppUser> cache = new HashMap<>();
            timeline = evs.stream().map(ev -> {
                AppUser actor = null;
                if (ev.getActorId() != null) {
                    actor = cache.computeIfAbsent(ev.getActorId(),
                            id -> users.findByIdAndTenantIdAndDeletedAtIsNull(id, app.getTenantId()).orElse(null));
                }
                return DtoMapper.timeline(ev, actor);
            }).toList();
        }
        return DtoMapper.application(app, applicant, files, timeline);
    }

    private static Sort.Direction sortDir(String order) {
        return "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private static String mapSort(String sortBy) {
        if (sortBy == null) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "title" -> "title";
            case "status" -> "status";
            case "updatedDate" -> "updatedAt";
            case "createdDate" -> "createdAt";
            default -> "createdAt";
        };
    }
}
