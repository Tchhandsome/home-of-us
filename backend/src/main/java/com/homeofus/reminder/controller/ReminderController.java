package com.homeofus.reminder.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.reminder.dto.CreateReminderRequest;
import com.homeofus.reminder.service.ReminderService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提醒中心接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /**
     * 创建提醒。
     *
     * @param request 创建请求
     * @return 新提醒 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateReminderRequest request) {
        return ApiResponse.ok(reminderService.create(request));
    }

    /**
     * 查询提醒列表。
     *
     * @param limit 查询数量
     * @return 提醒列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findReminders(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(reminderService.findReminders(limit));
    }

    /**
     * 完成提醒。
     *
     * @param id 提醒 ID
     * @return 更新结果
     */
    @PatchMapping("/{id}/complete")
    public ApiResponse<Map<String, Object>> complete(@PathVariable Long id) {
        return ApiResponse.ok(reminderService.complete(id));
    }
}

