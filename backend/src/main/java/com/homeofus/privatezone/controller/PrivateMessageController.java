package com.homeofus.privatezone.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.privatezone.dto.CreatePrivateMessageRequest;
import com.homeofus.privatezone.dto.UpdatePrivateMessageRequest;
import com.homeofus.privatezone.service.PrivateMessageService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 私密留言接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/private-messages")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    public PrivateMessageController(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    /**
     * 创建私密留言。
     *
     * @param request 创建请求
     * @return 新留言 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreatePrivateMessageRequest request) {
        return ApiResponse.ok(privateMessageService.create(request));
    }

    /**
     * 查询可见留言。
     *
     * @return 留言列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findVisibleMessages() {
        return ApiResponse.ok(privateMessageService.findVisibleMessages());
    }

    /**
     * 标记已读。
     *
     * @param id 留言 ID
     * @return 更新结果
     */
    @PatchMapping("/{id}/read")
    public ApiResponse<Map<String, Object>> markRead(@PathVariable Long id) {
        return ApiResponse.ok(privateMessageService.markRead(id));
    }

    /**
     * 更新留言。
     *
     * @param id 留言 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PatchMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id,
            @Valid @RequestBody UpdatePrivateMessageRequest request) {
        return ApiResponse.ok(privateMessageService.update(id, request));
    }
}
