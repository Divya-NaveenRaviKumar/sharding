package org.pilot.sharding.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ShardConfigurationProperties.class)
public class DataSourceConfiguration {
    private final ShardConfigurationProperties shardConfigurationProperties;

    public DataSourceConfiguration(ShardConfigurationProperties shardConfigurationProperties) {
        this.shardConfigurationProperties = shardConfigurationProperties;
    }

    @Bean
    public ShardedContext<DataSource> shardedDataSource() {
        Map<Integer, DataSource> shardedDataSources = shardConfigurationProperties.getShards().stream()
                .collect(Collectors.toMap(
                        ShardProperties::hash,
                        this::hikariDataSource
                ));

        return new ShardedContext<>(shardedDataSources);
    }

    private HikariDataSource hikariDataSource(ShardProperties shard) {
        HikariConfig hikariConfig = hikariConfig(shard);
        return new HikariDataSource(hikariConfig);
    }

    private HikariConfig hikariConfig(ShardProperties shard) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(shard.url());
        hikariConfig.setUsername(shard.username());
        hikariConfig.setPassword(shard.password());
        hikariConfig.setDriverClassName(shard.driverClassName());

        if (shard.hikari().containsKey("maximumPoolSize")) {
            hikariConfig.setMaximumPoolSize(Integer.parseInt(shard.hikari().get("maximumPoolSize")));
        }

        if (shard.hikari().containsKey("connectionInitSql")) {
            hikariConfig.setConnectionInitSql(shard.hikari().get("connectionInitSql"));
        }

        return hikariConfig;
    }

    @Bean
    public ShardedContext<Flyway> configureFlyway(ShardedContext<DataSource> shardedDataSource) {
        Map<Integer, Flyway> flywayMap = shardedDataSource.map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Flyway.configure()
                                .locations("classpath:/db/migration")
                                .dataSource(entry.getValue())
                                .load()
                ));

        flywayMap.values().forEach(Flyway::migrate);

        return new ShardedContext<>(flywayMap);
    }

    @Bean
    public ShardedContext<JdbcTemplate> shardedDatasourceJdbcTemplate(
            @Qualifier("shardedDataSource") ShardedContext<DataSource> shardedDataSource,
            @Qualifier("shardedDataSourceTransactionManager") ShardedContext<DataSourceTransactionManager> transactionManagers
    ) {
        Map<Integer, JdbcTemplate> jdbcTemplateMap = shardedDataSource.map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            DataSource dataSource = entry.getValue();
                            transactionManagers.getShardContextOfShardIndex(entry.getKey());
                            return new JdbcTemplate(dataSource);
                        }
                ));

        return new ShardedContext<>(jdbcTemplateMap);
    }

    @Bean
    public ShardedContext<DataSourceTransactionManager> shardedDataSourceTransactionManager(
            @Qualifier("shardedDataSource") ShardedContext<DataSource> shardDataSource
    ) {
        return new ShardedContext<>(
                shardDataSource.map.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> new DataSourceTransactionManager(entry.getValue())
                        ))
        );
    }
}
