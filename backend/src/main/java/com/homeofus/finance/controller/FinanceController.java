package com.homeofus.finance.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.finance.dto.CreateFinanceRecordRequest;
import com.homeofus.finance.service.FinanceService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家庭账本接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    /**
     * 创建账本记录。
     *
     * @param request 创建请求
     * @return 新账本记录 ID
     */
    @PostMapping("/records")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateFinanceRecordRequest request) {
        return ApiResponse.ok(financeService.create(request));
    }

    /**
     * 查询账本记录。
     *
     * @return 账本记录
     */
    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> findRecords() {
        return ApiResponse.ok(financeService.findRecords());
    }

    /**
     * 查询月度摘要。
     *
     * @param month 月份
     * @return 月度摘要
     */
    @GetMapping("/monthly-summary")
    public ApiResponse<List<Map<String, Object>>> summarizeMonth(@RequestParam(required = false) String month) {
        return ApiResponse.ok(financeService.summarizeMonth(month));
    }

    /**
     * 查询账本摘要。
     *
     * @return 摘要
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(financeService.summary());
    }

    /**
     * 查询账本默认分类。
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> findCategories() {
        return ApiResponse.ok(financeService.findCategories());
    }

    /**
     * 删除账本记录。
     *
     * @param id 记录 ID
     * @return 删除结果
     */
    @DeleteMapping("/records/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id) {
        return ApiResponse.ok(financeService.delete(id));
    }
}
