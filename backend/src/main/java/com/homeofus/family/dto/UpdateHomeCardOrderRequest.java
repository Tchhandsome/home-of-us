package com.homeofus.family.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新首页卡片顺序请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class UpdateHomeCardOrderRequest {

    private List<String> cardKeys;
}
