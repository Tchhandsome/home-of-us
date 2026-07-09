package com.homeofus.family.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.family.dto.CreateFamilyMemberRequest;
import com.homeofus.family.dto.UpdateHomeCardOrderRequest;
import com.homeofus.family.dto.UpdatePlantCheckInRequest;
import com.homeofus.family.dto.UpdateHomeViewModeRequest;
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import com.homeofus.family.service.FamilyService;
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
 * 家庭空间接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/families")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    /**
     * 查询默认家庭空间。
     *
     * @return 默认家庭空间
     */
    @GetMapping("/default")
    public ApiResponse<Map<String, Object>> getDefaultFamily() {
        return ApiResponse.ok(familyService.getDefaultFamilyOverview());
    }

    /**
     * 创建家庭成员。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/default/members")
    public ApiResponse<Map<String, Object>> createMember(@Valid @RequestBody CreateFamilyMemberRequest request) {
        return ApiResponse.ok(familyService.createMember(request));
    }

    /**
     * 更新家庭成员。
     *
     * @param id 成员 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PatchMapping("/default/members/{id}")
    public ApiResponse<Map<String, Object>> updateMember(@PathVariable Long id,
            @RequestBody UpdateFamilyMemberRequest request) {
        return ApiResponse.ok(familyService.updateMember(id, request));
    }

    /**
     * 更新当前登录成员的首页卡片顺序。
     *
     * @param request 顺序请求
     * @return 更新结果
     */
    @PatchMapping("/default/preferences/home-card-order")
    public ApiResponse<Map<String, Object>> updateHomeCardOrder(@RequestBody UpdateHomeCardOrderRequest request) {
        return ApiResponse.ok(familyService.updateHomeCardOrder(request));
    }

    /**
     * 更新当前登录成员的首页展示模式。
     *
     * @param request 展示模式请求
     * @return 更新结果
     */
    @PatchMapping("/default/preferences/home-view-mode")
    public ApiResponse<Map<String, Object>> updateHomeViewMode(@RequestBody UpdateHomeViewModeRequest request) {
        return ApiResponse.ok(familyService.updateHomeViewMode(request));
    }

    /**
     * 更新当前登录成员的花花签到日期。
     *
     * @param request 签到请求
     * @return 更新结果
     */
    @PatchMapping("/default/preferences/plant-check-in")
    public ApiResponse<Map<String, Object>> updatePlantCheckIn(@RequestBody UpdatePlantCheckInRequest request) {
        return ApiResponse.ok(familyService.updatePlantCheckIn(request));
    }
}
