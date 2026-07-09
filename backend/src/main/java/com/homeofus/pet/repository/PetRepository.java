package com.homeofus.pet.repository;

import com.homeofus.pet.dto.CreatePetCareRecordRequest;
import com.homeofus.pet.dto.CreatePetMedicalRecordRequest;
import com.homeofus.pet.dto.CreatePetPhotoRequest;
import com.homeofus.pet.dto.CreatePetRequest;
import com.homeofus.pet.dto.CreatePetWeightRecordRequest;
import com.homeofus.pet.dto.UpdatePetRequest;
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
     * 更新宠物档案。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param request 更新请求
     * @param species 宠物种类
     * @param birthday 生日
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updatePet(Long familyId, Long petId, UpdatePetRequest request, String species, LocalDate birthday,
            Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet SET name = ?, species = ?, breed = ?, gender = ?, birthday = ?, avatar_url = ?, note = ?, "
                        + "updated_at = ?, updated_by = ? WHERE id = ? AND family_id = ? AND deleted = 0",
                request.getName(), species, request.getBreed(), request.getGender(), birthday, request.getAvatarUrl(),
                request.getNote(), now, operatorId, petId, familyId);
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
     * 查询宠物照片。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param photoId 照片 ID
     * @return 照片
     */
    public Optional<Map<String, Object>> findPhoto(Long familyId, Long petId, Long photoId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, pet_id FROM pet_photo WHERE id = ? AND pet_id = ? AND family_id = ? AND deleted = 0",
                photoId, petId, familyId);
        return rows.stream().findFirst();
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

    /**
     * 新增宠物护理记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param request 创建请求
     * @param careType 护理类型
     * @param recordedAt 记录时间
     * @param nextDueAt 下次提醒时间
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertCareRecord(Long id, Long familyId, Long petId, CreatePetCareRecordRequest request,
            String careType, LocalDateTime recordedAt, LocalDateTime nextDueAt, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO pet_care_record (id, family_id, pet_id, care_type, recorded_at, description, next_due_at, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, petId, careType, recordedAt, request.getDescription(), nextDueAt, now, now, operatorId,
                operatorId);
    }

    /**
     * 查询宠物护理记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @return 护理记录
     */
    public List<Map<String, Object>> findCareRecords(Long familyId, Long petId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, pet_id, care_type, recorded_at, description, next_due_at, created_at "
                        + "FROM pet_care_record WHERE family_id = ? AND pet_id = ? AND deleted = 0 "
                        + "ORDER BY recorded_at DESC, created_at DESC",
                familyId, petId);
    }

    /**
     * 新增宠物体重记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param request 创建请求
     * @param recordedOn 记录日期
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertWeightRecord(Long id, Long familyId, Long petId, CreatePetWeightRecordRequest request,
            LocalDate recordedOn, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO pet_weight_record (id, family_id, pet_id, weight_kg, recorded_on, note, created_at, "
                        + "updated_at, created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, petId, request.getWeightKg(), recordedOn, request.getNote(), now, now, operatorId,
                operatorId);
    }

    /**
     * 查询宠物体重记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @return 体重记录
     */
    public List<Map<String, Object>> findWeightRecords(Long familyId, Long petId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, pet_id, weight_kg, recorded_on, note, created_at "
                        + "FROM pet_weight_record WHERE family_id = ? AND pet_id = ? AND deleted = 0 "
                        + "ORDER BY recorded_on ASC, created_at ASC",
                familyId, petId);
    }

    /**
     * 查询宠物医疗记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param recordId 医疗记录 ID
     * @return 医疗记录
     */
    public Optional<Map<String, Object>> findMedicalRecord(Long familyId, Long petId, Long recordId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, pet_id FROM pet_medical_record WHERE id = ? AND pet_id = ? AND family_id = ? "
                        + "AND deleted = 0",
                recordId, petId, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 删除宠物档案。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deletePet(Long familyId, Long petId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, petId, familyId);
    }

    /**
     * 删除宠物全部照片。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deletePhotosByPet(Long familyId, Long petId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet_photo SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND pet_id = ? AND deleted = 0",
                now, operatorId, familyId, petId);
    }

    /**
     * 删除宠物单张照片。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param photoId 照片 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deletePhoto(Long familyId, Long petId, Long photoId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet_photo SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND pet_id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, photoId, petId, familyId);
    }

    /**
     * 删除宠物全部医疗记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteMedicalRecordsByPet(Long familyId, Long petId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet_medical_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND pet_id = ? AND deleted = 0",
                now, operatorId, familyId, petId);
    }

    /**
     * 删除宠物全部护理记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteCareRecordsByPet(Long familyId, Long petId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet_care_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND pet_id = ? AND deleted = 0",
                now, operatorId, familyId, petId);
    }

    /**
     * 删除宠物全部体重记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteWeightRecordsByPet(Long familyId, Long petId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet_weight_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND pet_id = ? AND deleted = 0",
                now, operatorId, familyId, petId);
    }

    /**
     * 删除单条宠物医疗记录。
     *
     * @param familyId 家庭 ID
     * @param petId 宠物 ID
     * @param recordId 医疗记录 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteMedicalRecord(Long familyId, Long petId, Long recordId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE pet_medical_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND pet_id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, recordId, petId, familyId);
    }
}
