package com.homeofus.period.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.period.dto.CreatePeriodRecordRequest;
import com.homeofus.period.dto.SavePeriodProfileRequest;
import com.homeofus.period.repository.PeriodRepository;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 月经管理业务服务。
 *
 * @author tanchaohong
 */
@Service
public class PeriodService {

    private static final String PERIOD_SOURCE_TYPE = "PERIOD_TRACKER";

    private static final int DEFAULT_CYCLE_DAYS = 28;

    private static final int DEFAULT_PERIOD_DAYS = 5;

    private static final String DEFAULT_REMINDER_TIME = "09:00";

    private final PeriodRepository periodRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public PeriodService(PeriodRepository periodRepository, ReminderService reminderService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.periodRepository = periodRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 查询当前登录成员的月经管理概览。
     *
     * @return 周期资料、记录和预测
     */
    public Map<String, Object> getMine() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Optional<Map<String, Object>> profile = periodRepository.findProfile(DefaultFamily.FAMILY_ID,
                currentUser.getMemberId());
        List<Map<String, Object>> records = periodRepository.findRecords(DefaultFamily.FAMILY_ID,
                currentUser.getMemberId(), 18);
        return buildOverview(profile, records);
    }

    /**
     * 保存当前登录成员的周期资料。
     *
     * @param request 保存请求
     * @return 最新概览
     */
    public Map<String, Object> saveProfile(SavePeriodProfileRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Optional<Map<String, Object>> existingProfile = periodRepository.findProfile(DefaultFamily.FAMILY_ID,
                currentUser.getMemberId());
        List<Map<String, Object>> records = periodRepository.findRecords(DefaultFamily.FAMILY_ID,
                currentUser.getMemberId(), 18);

        Integer cycleDays = normalizeCycleDays(request.getCycleDays(),
                existingProfile.map(profile -> integerValue(profile, "cycle_days", DEFAULT_CYCLE_DAYS))
                        .orElse(DEFAULT_CYCLE_DAYS));
        Integer periodDays = normalizePeriodDays(request.getPeriodDays(),
                existingProfile.map(profile -> integerValue(profile, "period_days", DEFAULT_PERIOD_DAYS))
                        .orElse(DEFAULT_PERIOD_DAYS));
        LocalDate lastRecordStart = latestRecordStart(records);
        LocalDate defaultLastPeriodStart = Objects.nonNull(lastRecordStart) ? lastRecordStart
                : existingProfile.map(profile -> dateValue(profile, "last_period_start")).orElse(null);
        LocalDate lastPeriodStart = parseDate(request.getLastPeriodStart(), defaultLastPeriodStart);
        boolean reminderEnabled = Objects.nonNull(request.getReminderEnabled()) ? request.getReminderEnabled()
                : existingProfile.map(profile -> booleanValue(profile, "reminder_enabled", true)).orElse(true);
        String reminderTime = normalizeReminderTime(request.getReminderTime(),
                existingProfile.map(profile -> textValue(profile, "reminder_time")).orElse(DEFAULT_REMINDER_TIME));
        String note = Objects.isNull(request.getNote())
                ? existingProfile.map(profile -> textValue(profile, "note")).orElse("")
                : StringUtils.trimToEmpty(request.getNote());

        LocalDateTime now = timeProvider.now();
        Long profileId;
        if (existingProfile.isPresent()) {
            profileId = numberValue(existingProfile.get(), "id");
            periodRepository.updateProfile(profileId, DefaultFamily.FAMILY_ID, currentUser.getMemberId(), cycleDays,
                    periodDays, lastPeriodStart, reminderEnabled, reminderTime, note, currentUser.getUserId(), now);
        } else {
            profileId = idGenerator.nextId();
            periodRepository.insertProfile(profileId, DefaultFamily.FAMILY_ID, currentUser.getMemberId(), cycleDays,
                    periodDays, lastPeriodStart, reminderEnabled, reminderTime, note, currentUser.getUserId(), now);
        }

        Map<String, Object> profile = buildProfile(profileId, cycleDays, periodDays, lastPeriodStart, reminderEnabled,
                reminderTime, note, now);
        syncReminder(currentUser, cycleDays, lastPeriodStart, reminderEnabled, reminderTime, note);
        return buildOverview(Optional.of(profile), records);
    }

    /**
     * 新增月经记录。
     *
     * @param request 新增请求
     * @return 新记录 ID
     */
    public Map<String, Object> createRecord(CreatePeriodRecordRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        LocalDate startOn = parseRequiredDate(request.getStartOn(), "PERIOD_START_DATE_INVALID",
                "period.startOn.invalid");
        LocalDate endOn = parseDate(request.getEndOn(), null);
        if (Objects.nonNull(endOn) && endOn.isBefore(startOn)) {
            throw new BusinessException("PERIOD_END_DATE_INVALID", "period.endOn.invalid");
        }

        LocalDateTime now = timeProvider.now();
        Long recordId = idGenerator.nextId();
        periodRepository.insertRecord(recordId, DefaultFamily.FAMILY_ID, currentUser.getMemberId(), startOn, endOn,
                StringUtils.trimToEmpty(request.getNote()), currentUser.getUserId(), now);

        Optional<Map<String, Object>> existingProfile = periodRepository.findProfile(DefaultFamily.FAMILY_ID,
                currentUser.getMemberId());
        Integer cycleDays = existingProfile.map(profile -> integerValue(profile, "cycle_days", DEFAULT_CYCLE_DAYS))
                .orElse(DEFAULT_CYCLE_DAYS);
        Integer periodDays = existingProfile.map(profile -> integerValue(profile, "period_days", DEFAULT_PERIOD_DAYS))
                .orElse(DEFAULT_PERIOD_DAYS);
        boolean reminderEnabled = existingProfile.map(profile -> booleanValue(profile, "reminder_enabled", true))
                .orElse(true);
        String reminderTime = normalizeReminderTime(null,
                existingProfile.map(profile -> textValue(profile, "reminder_time")).orElse(DEFAULT_REMINDER_TIME));
        String note = existingProfile.map(profile -> textValue(profile, "note")).orElse("");

        if (existingProfile.isPresent()) {
            periodRepository.updateProfile(numberValue(existingProfile.get(), "id"), DefaultFamily.FAMILY_ID,
                    currentUser.getMemberId(), cycleDays, periodDays, startOn, reminderEnabled, reminderTime, note,
                    currentUser.getUserId(), now);
        } else {
            periodRepository.insertProfile(idGenerator.nextId(), DefaultFamily.FAMILY_ID, currentUser.getMemberId(),
                    cycleDays, periodDays, startOn, reminderEnabled, reminderTime, note, currentUser.getUserId(), now);
        }

        syncReminder(currentUser, cycleDays, startOn, reminderEnabled, reminderTime, note);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", recordId);
        return result;
    }

    private Map<String, Object> buildOverview(Optional<Map<String, Object>> profile, List<Map<String, Object>> records) {
        Map<String, Object> result = new LinkedHashMap<>();
        Integer cycleDays = profile.map(row -> integerValue(row, "cycle_days", DEFAULT_CYCLE_DAYS))
                .orElse(DEFAULT_CYCLE_DAYS);
        Integer periodDays = profile.map(row -> integerValue(row, "period_days", DEFAULT_PERIOD_DAYS))
                .orElse(DEFAULT_PERIOD_DAYS);
        LocalDate referenceStart = latestRecordStart(records);
        if (Objects.isNull(referenceStart)) {
            referenceStart = profile.map(row -> dateValue(row, "last_period_start")).orElse(null);
        }
        result.put("profile", profile.map(this::normalizeProfile).orElseGet(() -> defaultProfile(cycleDays, periodDays)));
        result.put("records", records);
        result.put("prediction", buildPrediction(referenceStart, cycleDays, periodDays));
        return result;
    }

    private Map<String, Object> normalizeProfile(Map<String, Object> profile) {
        Map<String, Object> result = new LinkedHashMap<>(profile);
        result.put("cycle_days", integerValue(profile, "cycle_days", DEFAULT_CYCLE_DAYS));
        result.put("period_days", integerValue(profile, "period_days", DEFAULT_PERIOD_DAYS));
        result.put("reminder_enabled", booleanValue(profile, "reminder_enabled", true));
        result.put("reminder_time", normalizeReminderTime(textValue(profile, "reminder_time"), DEFAULT_REMINDER_TIME));
        result.put("has_profile", true);
        return result;
    }

    private Map<String, Object> defaultProfile(Integer cycleDays, Integer periodDays) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", null);
        result.put("cycle_days", cycleDays);
        result.put("period_days", periodDays);
        result.put("last_period_start", null);
        result.put("reminder_enabled", true);
        result.put("reminder_time", DEFAULT_REMINDER_TIME);
        result.put("note", "");
        result.put("has_profile", false);
        return result;
    }

    private Map<String, Object> buildProfile(Long id, Integer cycleDays, Integer periodDays, LocalDate lastPeriodStart,
            boolean reminderEnabled, String reminderTime, String note, LocalDateTime updatedAt) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", id);
        profile.put("cycle_days", cycleDays);
        profile.put("period_days", periodDays);
        profile.put("last_period_start", lastPeriodStart);
        profile.put("reminder_enabled", reminderEnabled);
        profile.put("reminder_time", reminderTime);
        profile.put("note", note);
        profile.put("updated_at", updatedAt);
        profile.put("has_profile", true);
        return profile;
    }

    private Map<String, Object> buildPrediction(LocalDate referenceStart, Integer cycleDays, Integer periodDays) {
        Map<String, Object> prediction = new LinkedHashMap<>();
        prediction.put("next_start", null);
        prediction.put("next_end", null);
        prediction.put("ovulation_on", null);
        prediction.put("fertile_start", null);
        prediction.put("fertile_end", null);
        prediction.put("days_until_next", null);
        if (Objects.isNull(referenceStart)) {
            return prediction;
        }
        LocalDate nextStart = referenceStart;
        LocalDate today = timeProvider.today();
        while (nextStart.isBefore(today) || nextStart.isEqual(today)) {
            nextStart = nextStart.plusDays(cycleDays);
        }
        LocalDate nextEnd = nextStart.plusDays(Math.max(periodDays - 1, 0));
        LocalDate ovulationOn = nextStart.minusDays(14);
        prediction.put("next_start", nextStart);
        prediction.put("next_end", nextEnd);
        prediction.put("ovulation_on", ovulationOn);
        prediction.put("fertile_start", ovulationOn.minusDays(5));
        prediction.put("fertile_end", ovulationOn.plusDays(1));
        prediction.put("days_until_next", ChronoUnit.DAYS.between(today, nextStart));
        return prediction;
    }

    private void syncReminder(CurrentUser currentUser, Integer cycleDays, LocalDate lastPeriodStart,
            boolean reminderEnabled, String reminderTime, String note) {
        reminderService.deleteBySource(PERIOD_SOURCE_TYPE, currentUser.getMemberId());
        if (!reminderEnabled || Objects.isNull(lastPeriodStart)) {
            return;
        }
        LocalDateTime candidate = LocalDateTime.of(lastPeriodStart, parseReminderTime(reminderTime));
        LocalDateTime now = timeProvider.now();
        while (candidate.isBefore(now)) {
            candidate = candidate.plusDays(cycleDays);
        }
        reminderService.createFromSource("经期提醒", StringUtils.defaultIfBlank(note, "记得记录本次经期开始时间"), PERIOD_SOURCE_TYPE,
                currentUser.getMemberId(), candidate);
    }

    private Integer normalizeCycleDays(Integer cycleDays, Integer defaultValue) {
        int safeValue = Objects.isNull(cycleDays) ? defaultValue : cycleDays;
        if (safeValue < 15 || safeValue > 90) {
            return defaultValue;
        }
        return safeValue;
    }

    private Integer normalizePeriodDays(Integer periodDays, Integer defaultValue) {
        int safeValue = Objects.isNull(periodDays) ? defaultValue : periodDays;
        if (safeValue < 1 || safeValue > 15) {
            return defaultValue;
        }
        return safeValue;
    }

    private String normalizeReminderTime(String reminderTime, String fallback) {
        String safeFallback = StringUtils.defaultIfBlank(fallback, DEFAULT_REMINDER_TIME);
        if (StringUtils.isBlank(reminderTime)) {
            return safeFallback;
        }
        try {
            return LocalTime.parse(reminderTime).toString().substring(0, 5);
        } catch (DateTimeParseException exception) {
            return safeFallback;
        }
    }

    private LocalTime parseReminderTime(String reminderTime) {
        try {
            return LocalTime.parse(normalizeReminderTime(reminderTime, DEFAULT_REMINDER_TIME));
        } catch (DateTimeParseException exception) {
            return LocalTime.of(9, 0);
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private LocalDate parseRequiredDate(String value, String code, String message) {
        LocalDate parsed = parseDate(value, null);
        if (Objects.isNull(parsed)) {
            throw new BusinessException(code, message);
        }
        return parsed;
    }

    private LocalDate latestRecordStart(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return null;
        }
        return dateValue(records.get(0), "start_on");
    }

    private Integer integerValue(Map<String, Object> row, String key, Integer defaultValue) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (Objects.isNull(value)) {
            return "";
        }
        return String.valueOf(value);
    }

    private boolean booleanValue(Map<String, Object> row, String key, boolean defaultValue) {
        Object value = row.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof String) {
            return "1".equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
        }
        return defaultValue;
    }

    private LocalDate dateValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof String && StringUtils.isNotBlank((String) value)) {
            try {
                return LocalDate.parse((String) value);
            } catch (DateTimeParseException exception) {
                return null;
            }
        }
        return null;
    }
}
