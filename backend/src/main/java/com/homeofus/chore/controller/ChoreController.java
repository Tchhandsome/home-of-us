package com.homeofus.chore.controller;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import com.homeofus.chore.dto.UpdateChoreTaskRequest;
import com.homeofus.chore.service.ChoreService;
import com.homeofus.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享待办接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/chore/tasks")
public class ChoreController {

    private final ChoreService choreService;

    public ChoreController(ChoreService choreService) {
        this.choreService = choreService;
    }

    /**
     * 创建待办任务。
     *
     * @param request 创建请求
     * @return 新任务 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateChoreTaskRequest request) {
        return ApiResponse.ok(choreService.create(request));
    }

    /**
     * 查询待办任务。
     *
     * @return 待办任务
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findTasks() {
        return ApiResponse.ok(choreService.findTasks());
    }

    /**
     * 更新待办任务。
     *
     * @param taskId 任务 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PatchMapping("/{taskId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long taskId,
            @Valid @RequestBody UpdateChoreTaskRequest request) {
        return ApiResponse.ok(choreService.update(taskId, request));
    }

    /**
     * 认领共享任务。
     *
     * @param taskId 任务 ID
     * @return 更新结果
     */
    @PatchMapping("/{taskId}/claim")
    public ApiResponse<Map<String, Object>> claim(@PathVariable Long taskId) {
        return ApiResponse.ok(choreService.claim(taskId));
    }

    /**
     * 完成待办任务。
     *
     * @param taskId 任务 ID
     * @return 更新结果
     */
    @PatchMapping("/{taskId}/complete")
    public ApiResponse<Map<String, Object>> complete(@PathVariable Long taskId) {
        return ApiResponse.ok(choreService.complete(taskId));
    }

    /**
     * 删除待办任务。
     *
     * @param taskId 任务 ID
     * @return 删除结果
     */
    @DeleteMapping("/{taskId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long taskId) {
        return ApiResponse.ok(choreService.delete(taskId));
    }
}
