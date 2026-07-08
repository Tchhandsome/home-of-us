package com.homeofus.plant.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建花卉档案请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePlantRequest {

    @NotBlank(message = "plant.name.required")
    private String name;

    private String variety;

    private String flowerColor;

    private String location;

    private String status;

    private String carePreference;

    private String acquiredOn;

    private String coverUrl;
}

