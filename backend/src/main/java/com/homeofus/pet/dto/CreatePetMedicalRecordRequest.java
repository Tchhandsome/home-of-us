package com.homeofus.pet.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建宠物医疗记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePetMedicalRecordRequest {

    private String recordType;

    private String recordDate;

    private String hospital;

    private String medicine;

    @NotBlank(message = "pet.medical.description.required")
    private String description;

    private String nextDueAt;
}
