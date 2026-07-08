package com.homeofus.common.jdbc;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 本地开发阶段使用的轻量 ID 生成器。
 *
 * @author tanchaohong
 */
@Component
public class IdGenerator {

    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 生成业务主键。
     *
     * @return 业务主键
     */
    public Long nextId() {
        long epochPart = Instant.now().toEpochMilli() * 1000;
        long sequencePart = sequence.updateAndGet(value -> value >= 999 ? 0 : value + 1);
        return epochPart + sequencePart;
    }
}

