package com.bitalep.service.impl;

import com.bitalep.dto.MiscDtos;
import com.bitalep.entity.Subscription;
import com.bitalep.entity.Tenant;
import com.bitalep.exception.ApiException;
import com.bitalep.mapper.DtoMapper;
import com.bitalep.repository.SubscriptionRepository;
import com.bitalep.repository.TenantRepository;
import com.bitalep.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final TenantRepository tenants;
    private final SubscriptionRepository subscriptions;

    public CompanyService(TenantRepository tenants, SubscriptionRepository subscriptions) {
        this.tenants = tenants;
        this.subscriptions = subscriptions;
    }

    public MiscDtos.CompanyResponse get() {
        requireAdmin();
        Tenant tenant = tenants.findByIdAndDeletedAtIsNull(TenantContext.tenantId()).orElseThrow(ApiException::notFound);
        Subscription sub = subscriptions.findByTenantIdAndDeletedAtIsNull(tenant.getId()).orElse(null);
        return DtoMapper.company(
                tenant,
                sub == null ? "PRO" : sub.getPlan(),
                sub == null ? null : sub.isActive(),
                sub == null ? null : sub.getCurrentPeriodEnd()
        );
    }

    @Transactional
    public MiscDtos.CompanyResponse update(MiscDtos.UpdateCompanyRequest req) {
        requireAdmin();
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw ApiException.validation("errors:validation");
        }
        Tenant tenant = tenants.findByIdAndDeletedAtIsNull(TenantContext.tenantId()).orElseThrow(ApiException::notFound);
        tenant.setName(req.name().trim());
        tenant.setUpdatedBy(TenantContext.userId());
        tenants.save(tenant);
        return get();
    }

    private static void requireAdmin() {
        if (!TenantContext.isAdmin()) {
            throw ApiException.forbidden();
        }
    }
}
