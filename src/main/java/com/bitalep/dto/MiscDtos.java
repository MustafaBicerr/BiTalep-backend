package com.bitalep.dto;

import com.bitalep.entity.NotificationType;
import com.bitalep.entity.RequestStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MiscDtos {

    private MiscDtos() {}

    public record NotificationResponse(
            UUID id,
            NotificationType type,
            String title,
            String description,
            @JsonProperty("isRead") boolean isRead,
            Instant createdDate,
            UUID relatedRequestId,
            UserDtos.UserResponse actor
    ) {}

    public record UnreadCount(long count) {}

    public record CompanyResponse(
            UUID id,
            String name,
            String plan,
            Instant createdDate,
            Boolean subscriptionActive,
            Instant currentPeriodEnd
    ) {}

    public record UpdateCompanyRequest(String name) {}

    public record StatusDistribution(RequestStatus status, long count) {}

    public record WeeklyTrendPoint(LocalDate date, long count) {}

    public record DashboardResponse(
            long totalRequests,
            long pendingRequests,
            long approvedRequests,
            long rejectedRequests,
            long todayRequests,
            List<FormDtos.ApplicationResponse> recentRequests,
            List<StatusDistribution> statusDistribution,
            List<WeeklyTrendPoint> weeklyTrend,
            long overduePendingCount
    ) {}
}
