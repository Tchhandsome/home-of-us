package com.homeofus.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "auth.username.required")
    private String username;

    @NotBlank(message = "auth.password.required")
    private String password;
}
