package com.homeofus.family.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新花花签到请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class UpdatePlantCheckInRequest {

    private Long plantId;

    private String checkInDate;
}
