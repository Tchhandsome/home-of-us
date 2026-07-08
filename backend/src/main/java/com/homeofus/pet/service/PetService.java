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
import com.homeofus.pet.repository.PetRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
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

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public PetService(PetRepository petRepository, CurrentUserProvider currentUserProvider, IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.petRepository = petRepository;
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
        petRepository.insertMedicalRecord(id, DefaultFamily.FAMILY_ID, petId, request, recordType,
                parseDateOrToday(request.getRecordDate()), parseDateTime(request.getNextDueAt()),
                currentUser.getUserId(), timeProvider.now());
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

    private void ensurePetExists(Long petId) {
        petRepository.findPet(petId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("PET_NOT_FOUND", "pet.notFound"));
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
}
