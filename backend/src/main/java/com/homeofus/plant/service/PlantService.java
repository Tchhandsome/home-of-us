package com.homeofus.plant.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import com.homeofus.plant.repository.PlantRepository;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 花卉业务服务。
 *
 * @author tanchaohong
 */
@Service
public class PlantService {

    private final PlantRepository plantRepository;

    private final ReminderService reminderService;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public PlantService(PlantRepository plantRepository, ReminderService reminderService, IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.plantRepository = plantRepository;
        this.reminderService = reminderService;
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
        Long id = idGenerator.nextId();
        String status = StringUtils.defaultIfBlank(request.getStatus(), "GROWING");
        LocalDate acquiredOn = parseDate(request.getAcquiredOn(), null);
        plantRepository.insertPlant(id, DefaultFamily.FAMILY_ID, request, status, acquiredOn, timeProvider.now());
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
     * 新增养护记录。
     *
     * @param plantId 花卉 ID
     * @param request 创建请求
     * @return 新养护记录 ID
     */
    public Map<String, Object> createCareRecord(Long plantId, CreatePlantCareRecordRequest request) {
        Long id = idGenerator.nextId();
        LocalDate careDate = parseDate(request.getCareDate(), timeProvider.today());
        LocalDateTime nextCareAt = parseDateTime(request.getNextCareAt());
        plantRepository.insertCareRecord(id, DefaultFamily.FAMILY_ID, plantId, request, careDate, nextCareAt,
                timeProvider.now());
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
        return plantRepository.findCareRecords(plantId);
    }

    private void createCareReminder(Long plantId, Long careRecordId, CreatePlantCareRecordRequest request,
            LocalDateTime nextCareAt) {
        if (StringUtils.isBlank(request.getNextCareAt())) {
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
}

