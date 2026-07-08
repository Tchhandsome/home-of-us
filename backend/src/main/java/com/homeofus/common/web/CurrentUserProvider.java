package com.homeofus.common.web;

import com.homeofus.auth.repository.AuthRepository;
import com.homeofus.common.exception.BusinessException;
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

    private static final String TOKEN_HEADER = "X-Home-Token";

    private final AuthRepository authRepository;

    public CurrentUserProvider(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * 获取当前用户，未登录时直接拦截，避免请求被错误归属到默认账号。
     *
     * @return 当前用户
     */
    public CurrentUser getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(attributes)) {
            throw new BusinessException("AUTH_REQUIRED", "登录状态已失效，请重新登录");
        }
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isBlank(token)) {
            throw new BusinessException("AUTH_REQUIRED", "登录状态已失效，请重新登录");
        }
        return authRepository.findByToken(token)
                .map(this::toCurrentUser)
                .orElseThrow(() -> new BusinessException("AUTH_REQUIRED", "登录状态已失效，请重新登录"));
    }

    private CurrentUser toCurrentUser(Map<String, Object> row) {
        return new CurrentUser(numberValue(row, "id"), numberValue(row, "family_id"),
                numberValue(row, "family_member_id"), String.valueOf(row.get("username")),
                String.valueOf(row.get("display_name")));
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new BusinessException("AUTH_REQUIRED", "登录状态已失效，请重新登录");
    }
}
