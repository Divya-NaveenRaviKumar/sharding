package org.pilot.sharding.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.lang.Math.abs;

public class ShardedContext<T> {
    final Map<Integer, T> map;
    private final Logger logger = LoggerFactory.getLogger(ShardedContext.class);

    public ShardedContext(Map<Integer, T> map) {
        assert !map.values().isEmpty() : "There should be at least one data source.";
        this.map = map;
    }

    public T getShardContextOfShardIndex(int shardIndex) {
        return map.get(shardIndex);
    }

    public T getShardJdbcTemplate(long id) {
        int shard = shard(id);
        logger.info("Using Shard Context of type {} with index {} for id {}", map.get(shard).getClass().getSimpleName(), shard, id);
        return map.get(shard);
    }

    private int shard(Long id) {
        return abs(id.toString().hashCode()) % map.size();
    }
}
