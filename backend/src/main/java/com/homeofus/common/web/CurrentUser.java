package com.homeofus.common.web;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 当前登录用户上下文。
 *
 * @author tanchaohong
 */
@Getter
@AllArgsConstructor
public class CurrentUser {

    private final Long userId;

    private final Long familyId;

    private final Long memberId;

    private final String username;

    private final String displayName;
}
