package com.homeofus.family.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新家庭成员请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class UpdateFamilyMemberRequest {

    private String displayName;

    private String roleCode;

    private String avatarColor;

    private String avatarUrl;

    private String bio;

    private String username;

    private String password;
}
