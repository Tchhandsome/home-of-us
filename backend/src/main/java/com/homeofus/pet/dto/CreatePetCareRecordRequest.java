package com.homeofus.pet.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 创建宠物护理记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePetCareRecordRequest {

    private String careType;

    private String recordedAt;

    private String description;

    private String nextDueAt;
}
