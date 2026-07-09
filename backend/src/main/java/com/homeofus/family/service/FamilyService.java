package com.homeofus.family.service;

import com.homeofus.auth.repository.AuthRepository;
import com.homeofus.auth.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.family.dto.CreateFamilyMemberRequest;
import com.homeofus.family.dto.UpdateHomeCardOrderRequest;
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import com.homeofus.family.repository.FamilyRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static final String HOME_CARD_ORDER_KEY = "homeCardOrder";

    private static final List<String> DEFAULT_HOME_CARD_ORDER = List.of("todo", "plants", "care", "shopping",
            "finance", "reminders", "period", "members", "album", "pets", "inventory", "recipes", "private",
            "profile");

    private final FamilyRepository familyRepository;

    private final AuthRepository authRepository;

    private final AuthService authService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    private final ObjectMapper objectMapper;

    public FamilyService(FamilyRepository familyRepository, AuthRepository authRepository, AuthService authService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider,
            ObjectMapper objectMapper) {
        this.familyRepository = familyRepository;
        this.authRepository = authRepository;
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取默认家庭概览。
     *
     * @return 默认家庭概览
     */
    public Map<String, Object> getDefaultFamilyOverview() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Map<String, Object> overview = new LinkedHashMap<>();
        // 默认家庭用于第一版快速进入真实使用，不在 V1 引入复杂租户选择。
        overview.put("family", familyRepository.findFamily(DefaultFamily.FAMILY_ID));
        overview.put("members", familyRepository.findMembers(DefaultFamily.FAMILY_ID));
        overview.put("preferences", loadCurrentMemberPreferences(currentUser.getMemberId()));
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
        String safePassword = buildSafePassword(request.getPassword(), currentUser.getMemberId(), id);
        String passwordHash = StringUtils.isBlank(safePassword) ? "" : authService.hashPassword(safePassword);
        int accountUpdated = authRepository.updateUserByMemberId(id, request.getUsername(), passwordHash, request.getDisplayName(),
                currentUser.getUserId(), timeProvider.now());
        if (accountUpdated == 0 && StringUtils.isNotBlank(request.getUsername())
                && StringUtils.isNotBlank(safePassword)) {
            Long userId = idGenerator.nextId();
            String displayName = StringUtils.defaultIfBlank(request.getDisplayName(), String.valueOf(id));
            authRepository.insertUser(userId, DefaultFamily.FAMILY_ID, id, request.getUsername(), passwordHash,
                    displayName, currentUser.getUserId(), timeProvider.now());
        }
        return Map.of("updated", updated);
    }

    /**
     * 更新当前成员首页卡片顺序。
     *
     * @param request 顺序请求
     * @return 更新结果
     */
    public Map<String, Object> updateHomeCardOrder(UpdateHomeCardOrderRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        List<String> normalizedOrder = normalizeHomeCardOrder(Objects.isNull(request) ? null : request.getCardKeys());
        familyRepository.saveMemberPreference(idGenerator.nextId(), DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                HOME_CARD_ORDER_KEY, writeStringList(normalizedOrder), currentUser.getUserId(), timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", 1);
        result.put(HOME_CARD_ORDER_KEY, normalizedOrder);
        return result;
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

    private String buildSafePassword(String password, Long currentMemberId, Long targetMemberId) {
        if (StringUtils.isBlank(password)) {
            return "";
        }
        if (!Objects.equals(currentMemberId, targetMemberId)) {
            return "";
        }
        return password;
    }

    private Map<String, Object> loadCurrentMemberPreferences(Long memberId) {
        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put(HOME_CARD_ORDER_KEY, findHomeCardOrder(memberId));
        return preferences;
    }

    private List<String> findHomeCardOrder(Long memberId) {
        return familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, memberId, HOME_CARD_ORDER_KEY)
                .map(this::readStringList)
                .orElseGet(() -> new ArrayList<>(DEFAULT_HOME_CARD_ORDER));
    }

    private List<String> normalizeHomeCardOrder(List<String> rawCardKeys) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (Objects.nonNull(rawCardKeys)) {
            rawCardKeys.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(this::normalizeCardAlias)
                    .filter(DEFAULT_HOME_CARD_ORDER::contains)
                    .forEach(normalized::add);
        }
        DEFAULT_HOME_CARD_ORDER.forEach(normalized::add);
        return new ArrayList<>(normalized);
    }

    private String normalizeCardAlias(String rawCardKey) {
        if (StringUtils.equals(rawCardKey, "record")) {
            return "todo";
        }
        return rawCardKey;
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("FAMILY_PREFERENCE_SERIALIZE_FAILED", "family.preference.serialize.failed");
        }
    }

    private List<String> readStringList(String value) {
        try {
            List<String> parsed = objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
            return normalizeHomeCardOrder(parsed);
        } catch (JsonProcessingException exception) {
            return new ArrayList<>(DEFAULT_HOME_CARD_ORDER);
        }
    }
}
