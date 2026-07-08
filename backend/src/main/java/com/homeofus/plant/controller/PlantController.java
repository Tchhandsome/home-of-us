package com.homeofus.plant.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import com.homeofus.plant.service.PlantService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 花卉系统接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/plants")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    /**
     * 创建花卉档案。
     *
     * @param request 创建请求
     * @return 新花卉 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createPlant(@Valid @RequestBody CreatePlantRequest request) {
        return ApiResponse.ok(plantService.createPlant(request));
    }

    /**
     * 查询花卉列表。
     *
     * @return 花卉列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findPlants() {
        return ApiResponse.ok(plantService.findPlants());
    }

    /**
     * 新增花卉养护记录。
     *
     * @param plantId 花卉 ID
     * @param request 创建请求
     * @return 新养护记录 ID
     */
    @PostMapping("/{plantId}/care-records")
    public ApiResponse<Map<String, Object>> createCareRecord(@PathVariable Long plantId,
            @Valid @RequestBody CreatePlantCareRecordRequest request) {
        return ApiResponse.ok(plantService.createCareRecord(plantId, request));
    }

    /**
     * 查询花卉养护记录。
     *
     * @param plantId 花卉 ID
     * @return 养护记录
     */
    @GetMapping("/{plantId}/care-records")
    public ApiResponse<List<Map<String, Object>>> findCareRecords(@PathVariable Long plantId) {
        return ApiResponse.ok(plantService.findCareRecords(plantId));
    }

    /**
     * 删除花卉档案。
     *
     * @param plantId 花卉 ID
     * @return 删除结果
     */
    @DeleteMapping("/{plantId}")
    public ApiResponse<Map<String, Object>> deletePlant(@PathVariable Long plantId) {
        return ApiResponse.ok(plantService.deletePlant(plantId));
    }

    /**
     * 删除花卉养护记录。
     *
     * @param plantId 花卉 ID
     * @param recordId 记录 ID
     * @return 删除结果
     */
    @DeleteMapping("/{plantId}/care-records/{recordId}")
    public ApiResponse<Map<String, Object>> deleteCareRecord(@PathVariable Long plantId, @PathVariable Long recordId) {
        return ApiResponse.ok(plantService.deleteCareRecord(plantId, recordId));
    }
}
