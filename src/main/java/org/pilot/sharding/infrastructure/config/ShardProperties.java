package org.pilot.sharding.infrastructure.config;


import java.util.Map;

public record ShardProperties(
        String username,
        String password,
        int hash,
        String url,
        String driverClassName,
        Map<String, String> hikari
) {
    public ShardProperties {
        hikari = hikari != null ? hikari : Map.of();
    }
}
