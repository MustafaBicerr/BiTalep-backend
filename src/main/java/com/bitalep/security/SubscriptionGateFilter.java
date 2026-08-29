package com.bitalep.security;

import com.bitalep.config.AppProperties;
import com.bitalep.entity.Subscription;
import com.bitalep.repository.SubscriptionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class SubscriptionGateFilter extends OncePerRequestFilter {

    private final AppProperties props;
    private final SubscriptionRepository subscriptions;

    public SubscriptionGateFilter(AppProperties props, SubscriptionRepository subscriptions) {
        this.props = props;
        this.subscriptions = subscriptions;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.billing().enforcement()) {
            return true;
        }
        if (TenantContext.tenantIdOrNull() == null) {
            return true;
        }
        return isExempt(request.getMethod(), request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        UUID tenantId = TenantContext.tenantId();
        Subscription sub = subscriptions.findByTenantIdAndDeletedAtIsNull(tenantId).orElse(null);
        Instant now = Instant.now();
        boolean ok = sub != null && sub.isActive() && now.isBefore(sub.getCurrentPeriodEnd());
        if (!ok) {
            response.setStatus(402);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":{\"code\":\"SUBSCRIPTION_INACTIVE\",\"message\":\"errors:subscriptionInactive\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean isExempt(String method, String uri) {
        if (HttpMethod.POST.matches(method) && (
                uri.equals("/api/auth/login")
                        || uri.equals("/api/auth/register")
                        || uri.equals("/api/auth/refresh")
                        || uri.equals("/api/auth/logout")
                        || uri.equals("/api/auth/forgot-password")
                        || uri.equals("/api/auth/reset-password")
        )) {
            return true;
        }
        return HttpMethod.GET.matches(method) && (uri.equals("/api/users/me") || uri.equals("/api/company"));
    }
}
