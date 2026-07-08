package com.homeofus.record.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.record.dto.CreateQuickRecordRequest;
import com.homeofus.record.service.QuickRecordService;
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
 * 快速记录接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/records")
public class QuickRecordController {

    private final QuickRecordService quickRecordService;

    public QuickRecordController(QuickRecordService quickRecordService) {
        this.quickRecordService = quickRecordService;
    }

    /**
     * 创建快速记录。
     *
     * @param request 创建请求
     * @return 新记录 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateQuickRecordRequest request) {
        return ApiResponse.ok(quickRecordService.create(request));
    }

    /**
     * 查询最近快速记录。
     *
     * @param limit 查询数量
     * @return 最近记录
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findRecent(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(quickRecordService.findRecent(limit));
    }

    /**
     * 删除快速记录。
     *
     * @param id 记录 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id) {
        return ApiResponse.ok(quickRecordService.delete(id));
    }
}
