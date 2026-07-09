package com.homeofus.period.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.period.dto.CreatePeriodRecordRequest;
import com.homeofus.period.dto.SavePeriodProfileRequest;
import com.homeofus.period.service.PeriodService;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 月经管理接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/periods/me")
public class PeriodController {

    private final PeriodService periodService;

    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    /**
     * 查询当前登录成员的月经管理概览。
     *
     * @return 周期概览
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getMine() {
        return ApiResponse.ok(periodService.getMine());
    }

    /**
     * 保存当前登录成员的周期资料。
     *
     * @param request 保存请求
     * @return 最新概览
     */
    @PatchMapping("/profile")
    public ApiResponse<Map<String, Object>> saveProfile(@RequestBody SavePeriodProfileRequest request) {
        return ApiResponse.ok(periodService.saveProfile(request));
    }

    /**
     * 新增月经记录。
     *
     * @param request 新增请求
     * @return 新记录 ID
     */
    @PostMapping("/records")
    public ApiResponse<Map<String, Object>> createRecord(@Valid @RequestBody CreatePeriodRecordRequest request) {
        return ApiResponse.ok(periodService.createRecord(request));
    }
}
