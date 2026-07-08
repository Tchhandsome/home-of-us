package com.homeofus.auth.service;

import com.homeofus.auth.dto.LoginRequest;
import com.homeofus.auth.repository.AuthRepository;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.family.repository.FamilyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 登录认证业务服务。
 *
 * @author tanchaohong
 */
@Service
public class AuthService {

    private static final String PASSWORD_SALT = "home-of-us:";

    private final AuthRepository authRepository;

    private final CurrentUserProvider currentUserProvider;

    private final TimeProvider timeProvider;

    private final FamilyRepository familyRepository;

    public AuthService(AuthRepository authRepository, CurrentUserProvider currentUserProvider,
            TimeProvider timeProvider, FamilyRepository familyRepository) {
        this.authRepository = authRepository;
        this.currentUserProvider = currentUserProvider;
        this.timeProvider = timeProvider;
        this.familyRepository = familyRepository;
    }

    /**
     * 登录并生成本地会话。
     *
     * @param request 登录请求
     * @return token 和用户信息
     */
    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> userRow = authRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("AUTH_LOGIN_FAILED", "auth.login.failed"));
        String passwordHash = hashPassword(request.getPassword());
        if (!Objects.equals(passwordHash, userRow.get("password_hash"))) {
            throw new BusinessException("AUTH_LOGIN_FAILED", "auth.login.failed");
        }
        Long userId = ((Number) userRow.get("id")).longValue();
        String token = UUID.randomUUID().toString().replace("-", "");
        authRepository.updateSessionToken(userId, token, timeProvider.now());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("user", toUserMap(userRow));
        return response;
    }

    /**
     * 查询当前登录用户。
     *
     * @return 当前用户信息
     */
    public Map<String, Object> me() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", currentUser.getUserId());
        result.put("familyId", currentUser.getFamilyId());
        result.put("memberId", currentUser.getMemberId());
        result.put("username", currentUser.getUsername());
        result.put("displayName", currentUser.getDisplayName());
        familyRepository.findMember(currentUser.getMemberId(), DefaultFamily.FAMILY_ID)
                .ifPresent(member -> {
                    result.put("avatarColor", member.get("avatar_color"));
                    result.put("avatarUrl", member.get("avatar_url"));
                    result.put("bio", member.get("bio"));
                    result.put("roleCode", member.get("role_code"));
                });
        return result;
    }

    /**
     * 生成密码摘要，供成员创建时复用。
     *
     * @param password 明文密码
     * @return 摘要
     */
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((PASSWORD_SALT + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", Byte.toUnsignedInt(value)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("AUTH_HASH_FAILED", "auth.hash.failed");
        }
    }

    private Map<String, Object> toUserMap(Map<String, Object> userRow) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", userRow.get("id"));
        user.put("familyId", userRow.get("family_id"));
        user.put("memberId", userRow.get("family_member_id"));
        user.put("username", userRow.get("username"));
        user.put("displayName", userRow.get("display_name"));
        return user;
    }
}
