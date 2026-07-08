package com.homeofus.pet.repository;

import com.homeofus.pet.dto.CreatePetMedicalRecordRequest;
import com.homeofus.pet.dto.CreatePetPhotoRequest;
import com.homeofus.pet.dto.CreatePetRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 宠物模块数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class PetRepository {

    private final JdbcTemplate jdbcTemplate;

    public PetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增宠物档案。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param species 种类
     * @param birthday 生日
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertPet(Long id, Long familyId, CreatePetRequest request, String species, LocalDate birthday,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO pet (id, family_id, name, species, breed, gender, birthday, avatar_url, note, status, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, 0)",
                id, familyId, request.getName(), species, request.getBreed(), request.getGender(), birthday,
                request.getAvatarUrl(), request.getNote(), now, now, operatorId, operatorId);
    }

    /**
     * 查询宠物档案。
     *
     * @param familyId 家庭 ID
     * @return 宠物列表
     */
    public List<Map<String, Object>> findPets(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, name, species, breed, gender, birthday, avatar_url, note, status, created_at "
                        + "FROM pet WHERE family_id = ? AND deleted = 0 ORDER BY created_at DESC",
                familyId);
    }

    /**
     * 查询宠物。
     *
     * @param id 宠物 ID
     * @param familyId 家庭 ID
     * @return 宠物
     */
    public Optional<Map<String, Object>> findPet(Long id, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, name FROM pet WHERE id = ? AND family_id = ? AND deleted = 0",
                id, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 新增宠物照片。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param request 创建请求
     * @param takenOn 拍摄日期
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertPhoto(Long id, Long familyId, Long petId, CreatePetPhotoRequest request, LocalDate takenOn,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO pet_photo (id, family_id, pet_id, image_url, description, taken_on, created_at, "
                        + "updated_at, created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, petId, request.getImageUrl(), request.getDescription(), takenOn, now, now, operatorId,
                operatorId);
    }

    /**
     * 查询宠物照片。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @return 照片列表
     */
    public List<Map<String, Object>> findPhotos(Long familyId, Long petId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, pet_id, image_url, description, taken_on, created_at "
                        + "FROM pet_photo WHERE family_id = ? AND pet_id = ? AND deleted = 0 "
                        + "ORDER BY COALESCE(taken_on, DATE(created_at)) DESC, created_at DESC",
                familyId, petId);
    }

    /**
     * 新增宠物医疗记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param request 创建请求
     * @param recordType 记录类型
     * @param recordDate 记录日期
     * @param nextDueAt 下次提醒时间
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertMedicalRecord(Long id, Long familyId, Long petId, CreatePetMedicalRecordRequest request,
            String recordType, LocalDate recordDate, LocalDateTime nextDueAt, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO pet_medical_record (id, family_id, pet_id, record_type, record_date, hospital, medicine, "
                        + "description, next_due_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, petId, recordType, recordDate, request.getHospital(), request.getMedicine(),
                request.getDescription(), nextDueAt, now, now, operatorId, operatorId);
    }

    /**
     * 查询宠物医疗记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @return 医疗记录
     */
    public List<Map<String, Object>> findMedicalRecords(Long familyId, Long petId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, pet_id, record_type, record_date, hospital, medicine, description, "
                        + "next_due_at, created_at FROM pet_medical_record "
                        + "WHERE family_id = ? AND pet_id = ? AND deleted = 0 ORDER BY record_date DESC, created_at DESC",
                familyId, petId);
    }
}
