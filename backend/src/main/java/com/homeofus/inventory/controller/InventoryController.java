package com.homeofus.inventory.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.inventory.dto.CreateInventoryItemRequest;
import com.homeofus.inventory.service.InventoryService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家庭库存接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/inventory/items")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * 创建库存项。
     *
     * @param request 创建请求
     * @return 新库存项 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateInventoryItemRequest request) {
        return ApiResponse.ok(inventoryService.create(request));
    }

    /**
     * 查询库存列表。
     *
     * @return 库存列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findItems() {
        return ApiResponse.ok(inventoryService.findItems());
    }
}

