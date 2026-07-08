package com.homeofus.family.service;

import com.homeofus.auth.repository.AuthRepository;
import com.homeofus.auth.service.AuthService;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.family.dto.CreateFamilyMemberRequest;
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import com.homeofus.family.repository.FamilyRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 家庭空间业务服务。
 *
 * @author tanchaohong
 */
@Service
public class FamilyService {

    private final FamilyRepository familyRepository;

    private final AuthRepository authRepository;

    private final AuthService authService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public FamilyService(FamilyRepository familyRepository, AuthRepository authRepository, AuthService authService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.familyRepository = familyRepository;
        this.authRepository = authRepository;
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 获取默认家庭概览。
     *
     * @return 默认家庭概览
     */
    public Map<String, Object> getDefaultFamilyOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        // 默认家庭用于第一版快速进入真实使用，不在 V1 引入复杂租户选择。
        overview.put("family", familyRepository.findFamily(DefaultFamily.FAMILY_ID));
        overview.put("members", familyRepository.findMembers(DefaultFamily.FAMILY_ID));
        return overview;
    }

    /**
     * 创建默认家庭成员。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    public Map<String, Object> createMember(CreateFamilyMemberRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        ensureUsernameAvailable(request.getUsername(), null);
        Long memberId = idGenerator.nextId();
        familyRepository.insertMember(memberId, DefaultFamily.FAMILY_ID, request, currentUser.getUserId(),
                timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memberId", memberId);
        if (StringUtils.isNotBlank(request.getUsername()) && StringUtils.isNotBlank(request.getPassword())) {
            Long userId = idGenerator.nextId();
            authRepository.insertUser(userId, DefaultFamily.FAMILY_ID, memberId, request.getUsername(),
                    authService.hashPassword(request.getPassword()), request.getDisplayName(), currentUser.getUserId(),
                    timeProvider.now());
            result.put("userId", userId);
        }
        return result;
    }

    /**
     * 更新家庭成员。
     *
     * @param id 成员 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> updateMember(Long id, UpdateFamilyMemberRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        familyRepository.findMember(id, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("FAMILY_MEMBER_NOT_FOUND", "family.member.notFound"));
        ensureUsernameAvailable(request.getUsername(), id);
        int updated = familyRepository.updateMember(id, DefaultFamily.FAMILY_ID, request, currentUser.getUserId(),
                timeProvider.now());
        String passwordHash = StringUtils.isBlank(request.getPassword()) ? "" : authService.hashPassword(request.getPassword());
        int accountUpdated = authRepository.updateUserByMemberId(id, request.getUsername(), passwordHash, request.getDisplayName(),
                currentUser.getUserId(), timeProvider.now());
        if (accountUpdated == 0 && StringUtils.isNotBlank(request.getUsername())
                && StringUtils.isNotBlank(request.getPassword())) {
            Long userId = idGenerator.nextId();
            String displayName = StringUtils.defaultIfBlank(request.getDisplayName(), String.valueOf(id));
            authRepository.insertUser(userId, DefaultFamily.FAMILY_ID, id, request.getUsername(), passwordHash,
                    displayName, currentUser.getUserId(), timeProvider.now());
        }
        return Map.of("updated", updated);
    }

    private void ensureUsernameAvailable(String username, Long memberId) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        authRepository.findByUsername(username)
                .ifPresent(existing -> {
                    Long existingMemberId = ((Number) existing.get("family_member_id")).longValue();
                    if (!Objects.equals(existingMemberId, memberId)) {
                        throw new BusinessException("AUTH_USERNAME_EXISTS", "auth.username.exists");
                    }
                });
    }
}
