package com.homeofus.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 时间提供器，方便后续测试替换。
 *
 * @author tanchaohong
 */
@Component
public class TimeProvider {

    /**
     * 获取当前日期时间。
     *
     * @return 当前日期时间
     */
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期。
     *
     * @return 当前日期
     */
    public LocalDate today() {
        return LocalDate.now();
    }
}

