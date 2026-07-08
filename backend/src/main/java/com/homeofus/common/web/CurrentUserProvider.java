package com.homeofus.common.web;

import com.homeofus.auth.repository.AuthRepository;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前用户解析器。
 *
 * @author tanchaohong
 */
@Component
public class CurrentUserProvider {

    private static final CurrentUser DEFAULT_USER = new CurrentUser(1001L, 1L, 1001L, "小谭", "小谭");

    private static final String TOKEN_HEADER = "X-Home-Token";

    private final AuthRepository authRepository;

    public CurrentUserProvider(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * 获取当前用户，未登录时回退默认用户。
     *
     * @return 当前用户
     */
    public CurrentUser getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(attributes)) {
            return DEFAULT_USER;
        }
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isBlank(token)) {
            return DEFAULT_USER;
        }
        return authRepository.findByToken(token)
                .map(this::toCurrentUser)
                .orElse(DEFAULT_USER);
    }

    private CurrentUser toCurrentUser(Map<String, Object> row) {
        return new CurrentUser(numberValue(row, "id", DEFAULT_USER.getUserId()),
                numberValue(row, "family_id", DEFAULT_USER.getFamilyId()),
                numberValue(row, "family_member_id", DEFAULT_USER.getMemberId()), String.valueOf(row.get("username")),
                String.valueOf(row.get("display_name")));
    }

    private Long numberValue(Map<String, Object> row, String key, Long fallback) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return fallback;
    }
}
