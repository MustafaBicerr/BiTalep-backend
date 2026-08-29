package com.bitalep.scheduler;

import com.bitalep.entity.Subscription;
import com.bitalep.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class SubscriptionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryJob.class);
    private final SubscriptionRepository subscriptions;

    public SubscriptionExpiryJob(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    @Transactional
    public void expire() {
        Instant now = Instant.now();
        List<Subscription> expired = subscriptions.findByActiveTrueAndDeletedAtIsNullAndCurrentPeriodEndBefore(now);
        for (Subscription sub : expired) {
            sub.setActive(false);
            subscriptions.save(sub);
            log.info("subscription_expired tenantId={}", sub.getTenantId());
        }
    }
}
