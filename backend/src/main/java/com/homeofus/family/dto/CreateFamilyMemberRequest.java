package com.homeofus.family.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建家庭成员请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateFamilyMemberRequest {

    @NotBlank(message = "family.member.displayName.required")
    private String displayName;

    private String roleCode;

    private String avatarColor;

    private String avatarUrl;

    private String bio;

    private String username;

    private String password;
}
