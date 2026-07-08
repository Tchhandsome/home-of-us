package com.homeofus.pet.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建宠物档案请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePetRequest {

    @NotBlank(message = "pet.name.required")
    private String name;

    private String species;

    private String breed;

    private String gender;

    private String birthday;

    private String avatarUrl;

    private String note;
}
