package com.homeofus.plant.repository;

import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 花卉数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class PlantRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增花卉档案。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param status 状态
     * @param acquiredOn 入手日期
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertPlant(Long id, Long familyId, CreatePlantRequest request, String status, LocalDate acquiredOn,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO plant (id, family_id, name, variety, flower_color, location, status, care_preference, "
                        + "acquired_on, cover_url, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getName(), request.getVariety(), request.getFlowerColor(),
                request.getLocation(), status, request.getCarePreference(), acquiredOn, request.getCoverUrl(), now,
                now, operatorId, operatorId);
    }

    /**
     * 查询花卉列表。
     *
     * @param familyId 家庭 ID
     * @return 花卉列表
     */
    public List<Map<String, Object>> findPlants(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, name, variety, flower_color, location, status, care_preference, acquired_on, "
                        + "cover_url, created_at FROM plant WHERE family_id = ? AND deleted = 0 ORDER BY created_at DESC",
                familyId);
    }

    /**
     * 查询花卉名称。
     *
     * @param plantId 花卉 ID
     * @return 花卉名称
     */
    public String findPlantName(Long plantId) {
        return jdbcTemplate.queryForObject("SELECT name FROM plant WHERE id = ? AND deleted = 0", String.class,
                plantId);
    }

    /**
     * 查询花卉。
     *
     * @param plantId 花卉 ID
     * @param familyId 家庭 ID
     * @return 花卉
     */
    public Optional<Map<String, Object>> findPlant(Long plantId, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, name, flower_color, location, status, care_preference, acquired_on, cover_url "
                        + "FROM plant WHERE id = ? AND family_id = ? AND deleted = 0",
                plantId, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 更新花卉档案。
     *
     * @param plantId 花卉 ID
     * @param familyId 家庭 ID
     * @param request 更新请求
     * @param status 状态
     * @param acquiredOn 入手日期
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updatePlant(Long plantId, Long familyId, CreatePlantRequest request, String status, LocalDate acquiredOn,
            Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE plant SET name = ?, variety = ?, flower_color = ?, location = ?, status = ?, "
                        + "care_preference = ?, acquired_on = ?, cover_url = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                request.getName(), request.getVariety(), request.getFlowerColor(), request.getLocation(), status,
                request.getCarePreference(), acquiredOn, request.getCoverUrl(), now, operatorId, plantId, familyId);
    }

    /**
     * 新增养护记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param plantId 花卉 ID
     * @param request 创建请求
     * @param careDate 养护日期
     * @param nextCareAt 下次养护时间
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertCareRecord(Long id, Long familyId, Long plantId, CreatePlantCareRecordRequest request,
            LocalDate careDate, LocalDateTime nextCareAt, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO plant_care_record (id, family_id, plant_id, care_type, care_date, detail, raw_text, "
                        + "next_care_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, plantId, request.getCareType(), careDate, request.getDetail(), request.getRawText(),
                nextCareAt, now, now, operatorId, operatorId);
    }

    /**
     * 查询花卉养护记录。
     *
     * @param plantId 花卉 ID
     * @return 养护记录
     */
    public List<Map<String, Object>> findCareRecords(Long plantId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, plant_id, care_type, care_date, detail, raw_text, next_care_at, created_at "
                        + "FROM plant_care_record WHERE plant_id = ? AND deleted = 0 ORDER BY care_date DESC, created_at DESC",
                plantId);
    }

    /**
     * 查询养护记录。
     *
     * @param plantId 花卉 ID
     * @param recordId 记录 ID
     * @return 养护记录
     */
    public Optional<Map<String, Object>> findCareRecord(Long plantId, Long recordId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, plant_id FROM plant_care_record WHERE id = ? AND plant_id = ? AND deleted = 0",
                recordId, plantId);
        return rows.stream().findFirst();
    }

    /**
     * 查询需要修复的养护记录。
     *
     * @param familyId 家庭 ID
     * @return 养护记录
     */
    public List<Map<String, Object>> findCareRecordsForRepair(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, plant_id, care_date, detail, raw_text, next_care_at "
                        + "FROM plant_care_record WHERE family_id = ? AND deleted = 0 AND next_care_at IS NOT NULL",
                familyId);
    }

    /**
     * 更新养护记录的下次养护时间。
     *
     * @param recordId 记录 ID
     * @param familyId 家庭 ID
     * @param nextCareAt 下次养护时间
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updateCareRecordNextCareAt(Long recordId, Long familyId, LocalDateTime nextCareAt, Long operatorId,
            LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE plant_care_record SET next_care_at = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                nextCareAt, now, operatorId, recordId, familyId);
    }

    /**
     * 删除花卉。
     *
     * @param plantId 花卉 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deletePlant(Long plantId, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE plant SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, plantId, familyId);
    }

    /**
     * 删除花卉的全部养护记录。
     *
     * @param plantId 花卉 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteCareRecordsByPlant(Long plantId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE plant_care_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE plant_id = ? AND deleted = 0",
                now, operatorId, plantId);
    }

    /**
     * 删除单条养护记录。
     *
     * @param plantId 花卉 ID
     * @param recordId 记录 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteCareRecord(Long plantId, Long recordId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE plant_care_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND plant_id = ? AND deleted = 0",
                now, operatorId, recordId, plantId);
    }

    /**
     * 查询花卉数量。
     *
     * @param familyId 家庭 ID
     * @return 花卉数量
     */
    public Integer countPlants(Long familyId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM plant WHERE family_id = ? AND deleted = 0",
                Integer.class, familyId);
    }
}
