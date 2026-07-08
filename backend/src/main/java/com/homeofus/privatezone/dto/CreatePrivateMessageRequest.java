package com.homeofus.privatezone.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建私密留言请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePrivateMessageRequest {

    @NotBlank(message = "privateMessage.content.required")
    private String content;

    private String visibility;

    private Long receiverMemberId;
}
