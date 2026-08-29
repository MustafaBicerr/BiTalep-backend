package com.bitalep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "bitalep")
public record AppProperties(
        Jwt jwt,
        Refresh refresh,
        Billing billing,
        Files files,
        Cors cors,
        String panelBaseUrl,
        String demoSeedKey,
        Mail mail,
        RateLimit rateLimit
) {
    public record Jwt(String secret, long accessMinutes) {}
    public record Refresh(long days) {}
    public record Billing(boolean enforcement) {}
    public record Files(long maxBytes, String storagePath) {}
    public record Cors(String origins) {
        public List<String> originList() {
            return Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
    }
    public record Mail(String from) {}
    public record RateLimit(int authPerMinute) {}
}
