package com.bitalep.service.impl;

import com.bitalep.dto.FormDtos;
import com.bitalep.dto.MiscDtos;
import com.bitalep.entity.Application;
import com.bitalep.entity.RequestStatus;
import com.bitalep.repository.ApplicationRepository;
import com.bitalep.security.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DashboardService {

    private final ApplicationRepository applications;
    private final FormService formService;

    public DashboardService(ApplicationRepository applications, FormService formService) {
        this.applications = applications;
        this.formService = formService;
    }

    public MiscDtos.DashboardResponse stats() {
        List<Application> items = applications.findAll(scoped(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Instant now = Instant.now();
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant overdueCut = now.minus(3, ChronoUnit.DAYS);

        long total = items.size();
        long pending = items.stream().filter(a -> a.getStatus() == RequestStatus.NEW || a.getStatus() == RequestStatus.IN_REVIEW).count();
        long approved = items.stream().filter(a -> a.getStatus() == RequestStatus.APPROVED).count();
        long rejected = items.stream().filter(a -> a.getStatus() == RequestStatus.REJECTED).count();
        long today = items.stream().filter(a -> !a.getCreatedAt().isBefore(todayStart)).count();
        long overdue = items.stream()
                .filter(a -> a.getStatus() == RequestStatus.IN_REVIEW)
                .filter(a -> a.getUpdatedAt().isBefore(overdueCut))
                .count();

        List<FormDtos.ApplicationResponse> recent = items.stream()
                .limit(10)
                .map(a -> formService.toDto(a, false, false))
                .toList();

        List<MiscDtos.StatusDistribution> dist = Arrays.stream(RequestStatus.values())
                .map(s -> new MiscDtos.StatusDistribution(s, items.stream().filter(a -> a.getStatus() == s).count()))
                .toList();

        List<MiscDtos.WeeklyTrendPoint> trend = new ArrayList<>();
        LocalDate todayDate = LocalDate.now(ZoneOffset.UTC);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = todayDate.minusDays(i);
            Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            long count = items.stream()
                    .filter(a -> !a.getCreatedAt().isBefore(start) && a.getCreatedAt().isBefore(end))
                    .count();
            trend.add(new MiscDtos.WeeklyTrendPoint(day, count));
        }

        return new MiscDtos.DashboardResponse(
                total, pending, approved, rejected, today, recent, dist, trend, overdue
        );
    }

    private Specification<Application> scoped() {
        return (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("tenantId"), TenantContext.tenantId()));
            preds.add(cb.isNull(root.get("deletedAt")));
            if (!TenantContext.isAdmin()) {
                preds.add(cb.equal(root.get("applicantId"), TenantContext.userId()));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }
}
