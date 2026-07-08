package com.homeofus.pet.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.pet.dto.CreatePetMedicalRecordRequest;
import com.homeofus.pet.dto.CreatePetPhotoRequest;
import com.homeofus.pet.dto.CreatePetRequest;
import com.homeofus.pet.service.PetService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 宠物模块接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * 创建宠物档案。
     *
     * @param request 创建请求
     * @return 新宠物 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createPet(@Valid @RequestBody CreatePetRequest request) {
        return ApiResponse.ok(petService.createPet(request));
    }

    /**
     * 查询宠物档案。
     *
     * @return 宠物列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findPets() {
        return ApiResponse.ok(petService.findPets());
    }

    /**
     * 创建宠物照片。
     *
     * @param petId 宠物 ID
     * @param request 创建请求
     * @return 新照片 ID
     */
    @PostMapping("/{petId}/photos")
    public ApiResponse<Map<String, Object>> createPhoto(@PathVariable Long petId,
            @Valid @RequestBody CreatePetPhotoRequest request) {
        return ApiResponse.ok(petService.createPhoto(petId, request));
    }

    /**
     * 查询宠物照片。
     *
     * @param petId 宠物 ID
     * @return 照片列表
     */
    @GetMapping("/{petId}/photos")
    public ApiResponse<List<Map<String, Object>>> findPhotos(@PathVariable Long petId) {
        return ApiResponse.ok(petService.findPhotos(petId));
    }

    /**
     * 创建宠物医疗记录。
     *
     * @param petId 宠物 ID
     * @param request 创建请求
     * @return 新记录 ID
     */
    @PostMapping("/{petId}/medical-records")
    public ApiResponse<Map<String, Object>> createMedicalRecord(@PathVariable Long petId,
            @Valid @RequestBody CreatePetMedicalRecordRequest request) {
        return ApiResponse.ok(petService.createMedicalRecord(petId, request));
    }

    /**
     * 查询宠物医疗记录。
     *
     * @param petId 宠物 ID
     * @return 医疗记录
     */
    @GetMapping("/{petId}/medical-records")
    public ApiResponse<List<Map<String, Object>>> findMedicalRecords(@PathVariable Long petId) {
        return ApiResponse.ok(petService.findMedicalRecords(petId));
    }
}
