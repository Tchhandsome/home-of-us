package com.homeofus.pet.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.pet.dto.CreatePetMedicalRecordRequest;
import com.homeofus.pet.dto.CreatePetPhotoRequest;
import com.homeofus.pet.dto.CreatePetRequest;
import com.homeofus.pet.dto.UpdatePetRequest;
import com.homeofus.pet.repository.PetRepository;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 宠物模块业务服务。
 *
 * @author tanchaohong
 */
@Service
public class PetService {

    private final PetRepository petRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public PetService(PetRepository petRepository, ReminderService reminderService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.petRepository = petRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建宠物档案。
     *
     * @param request 创建请求
     * @return 新宠物 ID
     */
    public Map<String, Object> createPet(CreatePetRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String species = StringUtils.defaultIfBlank(request.getSpecies(), "PET");
        petRepository.insertPet(id, DefaultFamily.FAMILY_ID, request, species, parseDate(request.getBirthday()),
                currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询宠物档案。
     *
     * @return 宠物列表
     */
    public List<Map<String, Object>> findPets() {
        return petRepository.findPets(DefaultFamily.FAMILY_ID);
    }

    /**
     * 更新宠物档案。
     *
     * @param petId 宠物 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> updatePet(Long petId, UpdatePetRequest request) {
        ensurePetExists(petId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String species = StringUtils.defaultIfBlank(request.getSpecies(), "PET");
        int updated = petRepository.updatePet(DefaultFamily.FAMILY_ID, petId, request, species,
                parseDate(request.getBirthday()), currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    /**
     * 创建宠物照片。
     *
     * @param petId 宠物 ID
     * @param request 创建请求
     * @return 新照片 ID
     */
    public Map<String, Object> createPhoto(Long petId, CreatePetPhotoRequest request) {
        ensurePetExists(petId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        petRepository.insertPhoto(id, DefaultFamily.FAMILY_ID, petId, request, parseDate(request.getTakenOn()),
                currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询宠物照片。
     *
     * @param petId 宠物 ID
     * @return 照片列表
     */
    public List<Map<String, Object>> findPhotos(Long petId) {
        ensurePetExists(petId);
        return petRepository.findPhotos(DefaultFamily.FAMILY_ID, petId);
    }

    /**
     * 创建宠物医疗记录。
     *
     * @param petId 宠物 ID
     * @param request 创建请求
     * @return 新记录 ID
     */
    public Map<String, Object> createMedicalRecord(Long petId, CreatePetMedicalRecordRequest request) {
        ensurePetExists(petId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String recordType = StringUtils.defaultIfBlank(request.getRecordType(), "DEWORMING");
        LocalDateTime nextDueAt = parseDateTime(request.getNextDueAt());
        petRepository.insertMedicalRecord(id, DefaultFamily.FAMILY_ID, petId, request, recordType,
                parseDateOrToday(request.getRecordDate()), nextDueAt, currentUser.getUserId(), timeProvider.now());
        createMedicalReminder(petId, id, recordType, request, nextDueAt);
        return Map.of("id", id);
    }

    /**
     * 查询宠物医疗记录。
     *
     * @param petId 宠物 ID
     * @return 医疗记录
     */
    public List<Map<String, Object>> findMedicalRecords(Long petId) {
        ensurePetExists(petId);
        return petRepository.findMedicalRecords(DefaultFamily.FAMILY_ID, petId);
    }

    /**
     * 删除宠物档案。
     *
     * @param petId 宠物 ID
     * @return 删除结果
     */
    public Map<String, Object> deletePet(Long petId) {
        ensurePetExists(petId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        List<Map<String, Object>> medicalRecords = petRepository.findMedicalRecords(DefaultFamily.FAMILY_ID, petId);
        medicalRecords.forEach(record -> reminderService.deleteBySource("PET_MEDICAL", numberValue(record, "id")));
        petRepository.deletePhotosByPet(DefaultFamily.FAMILY_ID, petId, currentUser.getUserId(), timeProvider.now());
        petRepository.deleteMedicalRecordsByPet(DefaultFamily.FAMILY_ID, petId, currentUser.getUserId(),
                timeProvider.now());
        int updated = petRepository.deletePet(DefaultFamily.FAMILY_ID, petId, currentUser.getUserId(),
                timeProvider.now());
        if (updated == 0) {
            throw new BusinessException("PET_NOT_FOUND", "宠物不存在或已删除");
        }
        return Map.of("updated", updated);
    }

    /**
     * 删除宠物照片。
     *
     * @param petId 宠物 ID
     * @param photoId 照片 ID
     * @return 删除结果
     */
    public Map<String, Object> deletePhoto(Long petId, Long photoId) {
        ensurePetExists(petId);
        petRepository.findPhoto(DefaultFamily.FAMILY_ID, petId, photoId)
                .orElseThrow(() -> new BusinessException("PET_PHOTO_NOT_FOUND", "宠物照片不存在或已删除"));
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = petRepository.deletePhoto(DefaultFamily.FAMILY_ID, petId, photoId, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    /**
     * 删除宠物医疗记录。
     *
     * @param petId 宠物 ID
     * @param recordId 记录 ID
     * @return 删除结果
     */
    public Map<String, Object> deleteMedicalRecord(Long petId, Long recordId) {
        ensurePetExists(petId);
        petRepository.findMedicalRecord(DefaultFamily.FAMILY_ID, petId, recordId)
                .orElseThrow(() -> new BusinessException("PET_MEDICAL_RECORD_NOT_FOUND", "宠物医疗记录不存在或已删除"));
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        reminderService.deleteBySource("PET_MEDICAL", recordId);
        int updated = petRepository.deleteMedicalRecord(DefaultFamily.FAMILY_ID, petId, recordId,
                currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    private void ensurePetExists(Long petId) {
        petRepository.findPet(petId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("PET_NOT_FOUND", "pet.notFound"));
    }

    private void createMedicalReminder(Long petId, Long recordId, String recordType, CreatePetMedicalRecordRequest request,
            LocalDateTime nextDueAt) {
        if (Objects.isNull(nextDueAt)) {
            return;
        }
        String petName = String.valueOf(petRepository.findPet(petId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("PET_NOT_FOUND", "pet.notFound"))
                .get("name"));
        String title = petName + "医疗提醒";
        String description = StringUtils.defaultIfBlank(request.getDescription(), recordType);
        reminderService.createFromSource(title, description, "PET_MEDICAL", recordId, nextDueAt);
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDate parseDateOrToday(String value) {
        LocalDate date = parseDate(value);
        if (java.util.Objects.isNull(date)) {
            return timeProvider.today();
        }
        return date;
    }

    private LocalDateTime parseDateTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new BusinessException("PET_MEDICAL_RECORD_NOT_FOUND", "宠物医疗记录不存在或已删除");
    }
}
