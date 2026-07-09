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
import com.homeofus.family.dto.UpdateHomeViewModeRequest;
import com.homeofus.family.dto.UpdatePlantCheckInRequest;
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import com.homeofus.family.repository.FamilyRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static final String HOME_VIEW_MODE_KEY = "homeViewMode";

    private static final String PLANT_CHECK_IN_DATES_KEY = "plantCheckInDates";

    private static final int MAX_PLANT_CHECK_IN_HISTORY = 400;

    private static final List<String> DEFAULT_HOME_CARD_ORDER = List.of("todo", "plants", "care", "shopping",
            "finance", "reminders", "period", "members", "album", "pets", "votes", "inventory", "recipes",
            "private", "profile");

    private static final String DEFAULT_HOME_VIEW_MODE = "cards";

    private static final List<String> HOME_VIEW_MODES = List.of("calendar", "cards");

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

    /**
     * 更新当前成员首页展示模式。
     *
     * @param request 展示模式请求
     * @return 更新结果
     */
    public Map<String, Object> updateHomeViewMode(UpdateHomeViewModeRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String normalizedMode = normalizeHomeViewMode(Objects.isNull(request) ? "" : request.getViewMode());
        familyRepository.saveMemberPreference(idGenerator.nextId(), DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                HOME_VIEW_MODE_KEY, normalizedMode, currentUser.getUserId(), timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", 1);
        result.put(HOME_VIEW_MODE_KEY, normalizedMode);
        return result;
    }

    /**
     * 更新当前成员花花签到日期。
     *
     * @param request 签到请求
     * @return 更新结果
     */
    public Map<String, Object> updatePlantCheckIn(UpdatePlantCheckInRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String checkInDate = normalizePlantCheckInDate(Objects.isNull(request) ? "" : request.getCheckInDate());
        List<String> existingDates = findPlantCheckInDates(currentUser.getMemberId());
        List<String> mergedDates = normalizePlantCheckInDates(existingDates, checkInDate);
        familyRepository.saveMemberPreference(idGenerator.nextId(), DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                PLANT_CHECK_IN_DATES_KEY, writeStringList(mergedDates), currentUser.getUserId(), timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", 1);
        result.put(PLANT_CHECK_IN_DATES_KEY, mergedDates);
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
        preferences.put(HOME_VIEW_MODE_KEY, findHomeViewMode(memberId));
        preferences.put(PLANT_CHECK_IN_DATES_KEY, findPlantCheckInDates(memberId));
        return preferences;
    }

    private List<String> findHomeCardOrder(Long memberId) {
        return familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, memberId, HOME_CARD_ORDER_KEY)
                .map(this::readStringList)
                .orElseGet(() -> new ArrayList<>(DEFAULT_HOME_CARD_ORDER));
    }

    private String findHomeViewMode(Long memberId) {
        return familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, memberId, HOME_VIEW_MODE_KEY)
                .map(this::normalizeHomeViewMode)
                .orElse(DEFAULT_HOME_VIEW_MODE);
    }

    private List<String> findPlantCheckInDates(Long memberId) {
        return familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, memberId, PLANT_CHECK_IN_DATES_KEY)
                .map(this::readPlantCheckInDateList)
                .orElseGet(ArrayList::new);
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

    private String normalizeHomeViewMode(String rawMode) {
        if (HOME_VIEW_MODES.contains(rawMode)) {
            return rawMode;
        }
        return DEFAULT_HOME_VIEW_MODE;
    }

    private String normalizePlantCheckInDate(String rawDate) {
        if (StringUtils.isBlank(rawDate)) {
            return timeProvider.today().toString();
        }
        try {
            return LocalDate.parse(rawDate).toString();
        } catch (DateTimeParseException exception) {
            return timeProvider.today().toString();
        }
    }

    private List<String> normalizePlantCheckInDates(List<String> rawDates, String extraDate) {
        List<LocalDate> parsedDates = new ArrayList<>();
        if (Objects.nonNull(rawDates)) {
            rawDates.stream()
                    .filter(StringUtils::isNotBlank)
                    .forEach(value -> parsePlantCheckInDate(value).ifPresent(parsedDates::add));
        }
        parsePlantCheckInDate(extraDate).ifPresent(parsedDates::add);
        List<LocalDate> normalizedDates = parsedDates.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        int startIndex = Math.max(0, normalizedDates.size() - MAX_PLANT_CHECK_IN_HISTORY);
        return normalizedDates.subList(startIndex, normalizedDates.size()).stream()
                .map(LocalDate::toString)
                .toList();
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

    private List<String> readPlantCheckInDateList(String value) {
        try {
            List<String> parsed = objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
            return normalizePlantCheckInDates(parsed, "");
        } catch (JsonProcessingException exception) {
            return new ArrayList<>();
        }
    }

    private Optional<LocalDate> parsePlantCheckInDate(String rawDate) {
        if (StringUtils.isBlank(rawDate)) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(rawDate));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }
}
