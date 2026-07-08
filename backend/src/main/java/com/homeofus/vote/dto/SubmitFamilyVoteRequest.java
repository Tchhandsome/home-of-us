package com.homeofus.vote.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 提交家庭投票请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class SubmitFamilyVoteRequest {

    @NotNull(message = "vote.option.required")
    private Long optionId;
}
