package com.homeofus.plant.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import com.homeofus.plant.repository.PlantRepository;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 花卉业务服务。
 *
 * @author tanchaohong
 */
@Service
public class PlantService {

    private static final Pattern MONTH_DAY_PATTERN = Pattern
            .compile("(?<!\\d)(\\d{1,2})(?:月|[./-])(\\d{1,2})日?(?!\\d)");

    private final PlantRepository plantRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public PlantService(PlantRepository plantRepository, ReminderService reminderService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.plantRepository = plantRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建花卉档案。
     *
     * @param request 创建请求
     * @return 新花卉 ID
     */
    public Map<String, Object> createPlant(CreatePlantRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String status = StringUtils.defaultIfBlank(request.getStatus(), "GROWING");
        LocalDate acquiredOn = parseDate(request.getAcquiredOn(), null);
        plantRepository.insertPlant(id, DefaultFamily.FAMILY_ID, request, status, acquiredOn, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询花卉列表。
     *
     * @return 花卉列表
     */
    public List<Map<String, Object>> findPlants() {
        return plantRepository.findPlants(DefaultFamily.FAMILY_ID);
    }

    /**
     * 更新花卉档案。
     *
     * @param plantId 花卉 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> updatePlant(Long plantId, CreatePlantRequest request) {
        ensurePlantExists(plantId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String status = StringUtils.defaultIfBlank(request.getStatus(), "GROWING");
        LocalDate acquiredOn = parseDate(request.getAcquiredOn(), null);
        int updated = plantRepository.updatePlant(plantId, DefaultFamily.FAMILY_ID, request, status, acquiredOn,
                currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    /**
     * 新增养护记录。
     *
     * @param plantId 花卉 ID
     * @param request 创建请求
     * @return 新养护记录 ID
     */
    public Map<String, Object> createCareRecord(Long plantId, CreatePlantCareRecordRequest request) {
        ensurePlantExists(plantId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        LocalDate careDate = parseDate(request.getCareDate(), timeProvider.today());
        LocalDateTime nextCareAt = resolveNextCareAt(request, careDate);
        plantRepository.insertCareRecord(id, DefaultFamily.FAMILY_ID, plantId, request, careDate, nextCareAt,
                currentUser.getUserId(), timeProvider.now());
        createCareReminder(plantId, id, request, nextCareAt);
        return Map.of("id", id);
    }

    /**
     * 查询花卉养护记录。
     *
     * @param plantId 花卉 ID
     * @return 养护记录
     */
    public List<Map<String, Object>> findCareRecords(Long plantId) {
        ensurePlantExists(plantId);
        return plantRepository.findCareRecords(plantId);
    }

    /**
     * 删除花卉档案。
     *
     * @param plantId 花卉 ID
     * @return 删除结果
     */
    public Map<String, Object> deletePlant(Long plantId) {
        ensurePlantExists(plantId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        List<Map<String, Object>> careRecords = plantRepository.findCareRecords(plantId);
        // 先删除派生提醒，避免页面继续看到已经失效的后续养护待办。
        careRecords.forEach(record -> reminderService.deleteBySource("PLANT_CARE", numberValue(record, "id")));
        plantRepository.deleteCareRecordsByPlant(plantId, currentUser.getUserId(), timeProvider.now());
        int updated = plantRepository.deletePlant(plantId, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        if (updated == 0) {
            throw new BusinessException("PLANT_NOT_FOUND", "花卉不存在或已删除");
        }
        return Map.of("updated", updated);
    }

    /**
     * 删除花卉养护记录。
     *
     * @param plantId 花卉 ID
     * @param recordId 记录 ID
     * @return 删除结果
     */
    public Map<String, Object> deleteCareRecord(Long plantId, Long recordId) {
        ensurePlantExists(plantId);
        plantRepository.findCareRecord(plantId, recordId)
                .orElseThrow(() -> new BusinessException("PLANT_CARE_RECORD_NOT_FOUND", "养护记录不存在或已删除"));
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        reminderService.deleteBySource("PLANT_CARE", recordId);
        int updated = plantRepository.deleteCareRecord(plantId, recordId, currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    private void createCareReminder(Long plantId, Long careRecordId, CreatePlantCareRecordRequest request,
            LocalDateTime nextCareAt) {
        if (Objects.isNull(nextCareAt)) {
            return;
        }
        String plantName = plantRepository.findPlantName(plantId);
        String title = plantName + "下次养护";
        String description = StringUtils.defaultIfBlank(request.getDetail(), request.getRawText());
        // 养护记录派生提醒，保证用户输入一次即可进入后续待办。
        reminderService.createFromSource(title, description, "PLANT_CARE", careRecordId, nextCareAt);
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return timeProvider.now().plusDays(7);
        }
    }

    private LocalDateTime resolveNextCareAt(CreatePlantCareRecordRequest request, LocalDate careDate) {
        LocalDateTime nextCareAt = parseDateTime(request.getNextCareAt());
        if (Objects.nonNull(nextCareAt)) {
            return nextCareAt;
        }
        String sourceText = StringUtils.defaultIfBlank(request.getDetail(), request.getRawText());
        return parseNextCareAtFromText(sourceText, careDate);
    }

    private LocalDateTime parseNextCareAtFromText(String sourceText, LocalDate careDate) {
        if (StringUtils.isBlank(sourceText)) {
            return null;
        }
        Matcher matcher = MONTH_DAY_PATTERN.matcher(sourceText);
        if (!matcher.find()) {
            return null;
        }
        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        try {
            LocalDate baseDate = Objects.isNull(careDate) ? timeProvider.today() : careDate;
            LocalDate candidate = LocalDate.of(baseDate.getYear(), month, day);
            if (candidate.isBefore(baseDate)) {
                candidate = candidate.plusYears(1);
            }
            return LocalDateTime.of(candidate, LocalTime.of(9, 0));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void ensurePlantExists(Long plantId) {
        plantRepository.findPlant(plantId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("PLANT_NOT_FOUND", "花卉不存在或已删除"));
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new BusinessException("PLANT_CARE_RECORD_NOT_FOUND", "养护记录不存在或已删除");
    }
}
