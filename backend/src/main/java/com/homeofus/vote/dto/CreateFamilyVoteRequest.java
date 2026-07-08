package com.homeofus.vote.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建家庭投票请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateFamilyVoteRequest {

    @NotBlank(message = "vote.title.required")
    private String title;

    private String voteCategory;

    private List<String> options;
}
