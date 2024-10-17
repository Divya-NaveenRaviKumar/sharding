package org.pilot.sharding.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@ConfigurationProperties("datasource")
public class ShardConfigurationProperties {

    @NotBlank
    List<ShardProperties> shards;

    public List<ShardProperties> getShards() {
        return shards;
    }

    public void setShards(List<ShardProperties> shards) {
        this.shards = shards;
    }
}
