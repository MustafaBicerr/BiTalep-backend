package com.bitalep.service.impl;

import com.bitalep.dto.MiscDtos;
import com.bitalep.dto.PaginationMeta;
import com.bitalep.entity.AppNotification;
import com.bitalep.entity.AppUser;
import com.bitalep.entity.NotificationType;
import com.bitalep.entity.UserRole;
import com.bitalep.exception.ApiException;
import com.bitalep.mapper.DtoMapper;
import com.bitalep.repository.NotificationRepository;
import com.bitalep.repository.UserRepository;
import com.bitalep.security.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    public record NotifPage(List<MiscDtos.NotificationResponse> data, PaginationMeta meta) {}

    public NotifPage list(int page, int pageSize) {
        int p = Math.max(page, 1);
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        UUID tenantId = TenantContext.tenantId();
        UUID userId = TenantContext.userId();
        Specification<AppNotification> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("tenantId"), tenantId));
            preds.add(cb.equal(root.get("recipientId"), userId));
            preds.add(cb.isNull(root.get("deletedAt")));
            return cb.and(preds.toArray(Predicate[]::new));
        };
        Page<AppNotification> result = notifications.findAll(
                spec, PageRequest.of(p - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<MiscDtos.NotificationResponse> data = result.getContent().stream()
                .map(n -> DtoMapper.notification(n, actor(n)))
                .toList();
        return new NotifPage(data, PaginationMeta.of(p, size, result.getTotalElements()));
    }

    @Transactional
    public void markRead(UUID id) {
        AppNotification n = notifications
                .findByIdAndTenantIdAndRecipientIdAndDeletedAtIsNull(id, TenantContext.tenantId(), TenantContext.userId())
                .orElseThrow(ApiException::notFound);
        n.setRead(true);
        n.setUpdatedBy(TenantContext.userId());
        notifications.save(n);
    }

    @Transactional
    public void markAllRead() {
        notifications.markAllRead(TenantContext.tenantId(), TenantContext.userId());
    }

    public MiscDtos.UnreadCount unreadCount() {
        long c = notifications.countByTenantIdAndRecipientIdAndReadFalseAndDeletedAtIsNull(
                TenantContext.tenantId(), TenantContext.userId());
        return new MiscDtos.UnreadCount(c);
    }

    public void notifyAdmins(NotificationType type, String title, String description, UUID relatedId) {
        UUID tenantId = TenantContext.tenantId();
        UUID actorId = TenantContext.userIdOrNull();
        users.findByTenantIdAndRoleAndDeletedAtIsNullAndActiveTrue(tenantId, UserRole.ADMIN).forEach(admin -> {
            if (admin.getId().equals(actorId)) {
                return;
            }
            save(tenantId, admin.getId(), actorId, relatedId, type, title, description);
        });
    }

    public void notifyUser(UUID recipientId, NotificationType type, String title, String description, UUID relatedId) {
        if (recipientId.equals(TenantContext.userIdOrNull())) {
            return;
        }
        save(TenantContext.tenantId(), recipientId, TenantContext.userIdOrNull(), relatedId, type, title, description);
    }

    private void save(
            UUID tenantId,
            UUID recipientId,
            UUID actorId,
            UUID relatedId,
            NotificationType type,
            String title,
            String description
    ) {
        AppNotification n = new AppNotification();
        n.setTenantId(tenantId);
        n.setRecipientId(recipientId);
        n.setActorId(actorId);
        n.setRelatedRequestId(relatedId);
        n.setType(type);
        n.setTitle(title);
        n.setDescription(description);
        n.setRead(false);
        n.setCreatedBy(actorId);
        n.setUpdatedBy(actorId);
        notifications.save(n);
    }

    private AppUser actor(AppNotification n) {
        if (n.getActorId() == null) {
            return null;
        }
        return users.findByIdAndTenantIdAndDeletedAtIsNull(n.getActorId(), n.getTenantId()).orElse(null);
    }
}
