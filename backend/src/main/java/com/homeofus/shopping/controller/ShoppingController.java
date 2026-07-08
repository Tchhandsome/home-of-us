package com.homeofus.shopping.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.shopping.dto.CompleteShoppingItemRequest;
import com.homeofus.shopping.dto.CreateShoppingItemRequest;
import com.homeofus.shopping.service.ShoppingService;
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
 * 购物清单接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/shopping/items")
public class ShoppingController {

    private final ShoppingService shoppingService;

    public ShoppingController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    /**
     * 创建购物项。
     *
     * @param request 创建请求
     * @return 新购物项 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateShoppingItemRequest request) {
        return ApiResponse.ok(shoppingService.create(request));
    }

    /**
     * 查询购物清单。
     *
     * @return 购物清单
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findItems() {
        return ApiResponse.ok(shoppingService.findItems());
    }

    /**
     * 完成购物项。
     *
     * @param id 购物项 ID
     * @param request 完成请求
     * @return 更新结果
     */
    @PatchMapping("/{id}/check")
    public ApiResponse<Map<String, Object>> check(@PathVariable Long id,
            @Valid @RequestBody CompleteShoppingItemRequest request) {
        return ApiResponse.ok(shoppingService.check(id, request));
    }

    /**
     * 删除购物项。
     *
     * @param id 购物项 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id) {
        return ApiResponse.ok(shoppingService.delete(id));
    }
}
