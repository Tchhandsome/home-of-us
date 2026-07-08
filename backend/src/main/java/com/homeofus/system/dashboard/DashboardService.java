package com.homeofus.system.dashboard;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.time.TimeProvider;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 首页摘要业务服务。
 *
 * @author tanchaohong
 */
@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    private final TimeProvider timeProvider;

    public DashboardService(DashboardRepository dashboardRepository, TimeProvider timeProvider) {
        this.dashboardRepository = dashboardRepository;
        this.timeProvider = timeProvider;
    }

    /**
     * 查询今日摘要。
     *
     * @return 今日摘要
     */
    public Map<String, Object> getTodaySummary() {
        String currentMonth = timeProvider.today().toString().substring(0, 7);
        return dashboardRepository.loadTodaySummary(DefaultFamily.FAMILY_ID, currentMonth);
    }
}

