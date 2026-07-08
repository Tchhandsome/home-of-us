package com.homeofus.chore.controller;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import com.homeofus.chore.service.ChoreService;
import com.homeofus.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家务任务接口。
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
     * 创建家务任务。
     *
     * @param request 创建请求
     * @return 新任务 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateChoreTaskRequest request) {
        return ApiResponse.ok(choreService.create(request));
    }

    /**
     * 查询家务任务。
     *
     * @return 家务任务
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findTasks() {
        return ApiResponse.ok(choreService.findTasks());
    }
}

