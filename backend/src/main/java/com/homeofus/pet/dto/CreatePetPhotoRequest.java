package com.homeofus.pet.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建宠物照片请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePetPhotoRequest {

    @NotBlank(message = "pet.photo.imageUrl.required")
    private String imageUrl;

    private String description;

    private String takenOn;
}
