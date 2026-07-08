package com.homeofus.system.dashboard;

import com.homeofus.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页摘要接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询今日摘要。
     *
     * @return 今日摘要
     */
    @GetMapping("/today")
    public ApiResponse<Map<String, Object>> getTodaySummary() {
        return ApiResponse.ok(dashboardService.getTodaySummary());
    }
}

