package com.homeofus.period.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 保存月经周期资料请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class SavePeriodProfileRequest {

    private Integer cycleDays;

    private Integer periodDays;

    private String lastPeriodStart;

    private Boolean reminderEnabled;

    private String reminderTime;

    private String note;
}
