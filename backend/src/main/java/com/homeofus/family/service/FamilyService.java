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

    private static final String PLANT_CHECK_IN_ENTRIES_KEY = "plantCheckInEntries";

    private static final int MAX_PLANT_CHECK_IN_HISTORY = 400;

    private static final int MAX_PLANT_CHECK_IN_ENTRY_HISTORY = 800;

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

    private record PlantCheckInEntryValue(Long plantId, String checkInDate) {
    }

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
        Long plantId = Objects.isNull(request) ? null : normalizePlantCheckInPlantId(request.getPlantId());
        List<String> legacyDates = findLegacyPlantCheckInDates(currentUser.getMemberId());
        List<PlantCheckInEntryValue> existingEntries = findPlantCheckInEntries(currentUser.getMemberId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", 1);
        if (Objects.nonNull(plantId)) {
            List<PlantCheckInEntryValue> mergedEntries = normalizePlantCheckInEntries(existingEntries, plantId, checkInDate);
            familyRepository.saveMemberPreference(idGenerator.nextId(), DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                    PLANT_CHECK_IN_ENTRIES_KEY, writePlantCheckInEntryList(mergedEntries), currentUser.getUserId(),
                    timeProvider.now());
            result.put(PLANT_CHECK_IN_ENTRIES_KEY, toPlantCheckInEntryMaps(mergedEntries));
            result.put(PLANT_CHECK_IN_DATES_KEY, mergePlantCheckInDates(legacyDates, mergedEntries));
            return result;
        }
        List<String> mergedDates = normalizePlantCheckInDates(legacyDates, checkInDate);
        familyRepository.saveMemberPreference(idGenerator.nextId(), DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                PLANT_CHECK_IN_DATES_KEY, writeStringList(mergedDates), currentUser.getUserId(), timeProvider.now());
        result.put(PLANT_CHECK_IN_ENTRIES_KEY, toPlantCheckInEntryMaps(existingEntries));
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
        List<String> legacyDates = findLegacyPlantCheckInDates(memberId);
        List<PlantCheckInEntryValue> entries = findPlantCheckInEntries(memberId);
        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put(HOME_CARD_ORDER_KEY, findHomeCardOrder(memberId));
        preferences.put(HOME_VIEW_MODE_KEY, findHomeViewMode(memberId));
        preferences.put(PLANT_CHECK_IN_DATES_KEY, mergePlantCheckInDates(legacyDates, entries));
        preferences.put(PLANT_CHECK_IN_ENTRIES_KEY, toPlantCheckInEntryMaps(entries));
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

    private List<String> findLegacyPlantCheckInDates(Long memberId) {
        return familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, memberId, PLANT_CHECK_IN_DATES_KEY)
                .map(this::readPlantCheckInDateList)
                .orElseGet(ArrayList::new);
    }

    private List<PlantCheckInEntryValue> findPlantCheckInEntries(Long memberId) {
        return familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, memberId, PLANT_CHECK_IN_ENTRIES_KEY)
                .map(this::readPlantCheckInEntryList)
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

    private Long normalizePlantCheckInPlantId(Long rawPlantId) {
        if (Objects.isNull(rawPlantId) || rawPlantId <= 0) {
            return null;
        }
        return rawPlantId;
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

    private List<PlantCheckInEntryValue> normalizePlantCheckInEntries(List<PlantCheckInEntryValue> rawEntries, Long extraPlantId,
            String extraDate) {
        Map<String, PlantCheckInEntryValue> normalized = new LinkedHashMap<>();
        if (Objects.nonNull(rawEntries)) {
            rawEntries.stream()
                    .map(this::normalizePlantCheckInEntry)
                    .flatMap(Optional::stream)
                    .forEach(entry -> normalized.put(entry.plantId() + "-" + entry.checkInDate(), entry));
        }
        normalizePlantCheckInEntry(new PlantCheckInEntryValue(extraPlantId, extraDate))
                .ifPresent(entry -> normalized.put(entry.plantId() + "-" + entry.checkInDate(), entry));
        List<PlantCheckInEntryValue> sortedEntries = normalized.values().stream()
                .sorted(Comparator.comparing(PlantCheckInEntryValue::checkInDate)
                        .thenComparing(PlantCheckInEntryValue::plantId))
                .toList();
        int startIndex = Math.max(0, sortedEntries.size() - MAX_PLANT_CHECK_IN_ENTRY_HISTORY);
        return sortedEntries.subList(startIndex, sortedEntries.size());
    }

    private Optional<PlantCheckInEntryValue> normalizePlantCheckInEntry(PlantCheckInEntryValue entry) {
        if (Objects.isNull(entry)) {
            return Optional.empty();
        }
        Long plantId = normalizePlantCheckInPlantId(entry.plantId());
        Optional<LocalDate> checkInDate = parsePlantCheckInDate(entry.checkInDate());
        if (Objects.isNull(plantId) || checkInDate.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PlantCheckInEntryValue(plantId, checkInDate.get().toString()));
    }

    private List<String> mergePlantCheckInDates(List<String> legacyDates, List<PlantCheckInEntryValue> entries) {
        List<String> mergedDates = new ArrayList<>();
        if (Objects.nonNull(legacyDates)) {
            mergedDates.addAll(legacyDates);
        }
        if (Objects.nonNull(entries)) {
            entries.stream()
                    .map(PlantCheckInEntryValue::checkInDate)
                    .forEach(mergedDates::add);
        }
        return normalizePlantCheckInDates(mergedDates, "");
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("FAMILY_PREFERENCE_SERIALIZE_FAILED", "family.preference.serialize.failed");
        }
    }

    private String writePlantCheckInEntryList(List<PlantCheckInEntryValue> entries) {
        try {
            return objectMapper.writeValueAsString(toPlantCheckInEntryMaps(entries));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("FAMILY_PREFERENCE_SERIALIZE_FAILED", "family.preference.serialize.failed");
        }
    }

    private List<Map<String, Object>> toPlantCheckInEntryMaps(List<PlantCheckInEntryValue> entries) {
        if (Objects.isNull(entries) || entries.isEmpty()) {
            return new ArrayList<>();
        }
        return entries.stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("plantId", entry.plantId());
                    row.put("checkInDate", entry.checkInDate());
                    return row;
                })
                .toList();
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

    private List<PlantCheckInEntryValue> readPlantCheckInEntryList(String value) {
        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(value, new TypeReference<List<Map<String, Object>>>() {
            });
            List<PlantCheckInEntryValue> rawEntries = parsed.stream()
                    .map(this::parsePlantCheckInEntry)
                    .flatMap(Optional::stream)
                    .toList();
            return normalizePlantCheckInEntries(rawEntries, null, "");
        } catch (JsonProcessingException exception) {
            return new ArrayList<>();
        }
    }

    private Optional<PlantCheckInEntryValue> parsePlantCheckInEntry(Map<String, Object> row) {
        if (Objects.isNull(row)) {
            return Optional.empty();
        }
        Long plantId = parsePositiveLong(row.get("plantId"));
        String checkInDate = Objects.toString(row.get("checkInDate"), "");
        return normalizePlantCheckInEntry(new PlantCheckInEntryValue(plantId, checkInDate));
    }

    private Long parsePositiveLong(Object rawValue) {
        if (Objects.isNull(rawValue)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(String.valueOf(rawValue));
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
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
