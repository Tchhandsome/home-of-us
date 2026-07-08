package com.homeofus.plant.repository;

import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
     * @param now 当前时间
     */
    public void insertPlant(Long id, Long familyId, CreatePlantRequest request, String status, LocalDate acquiredOn,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO plant (id, family_id, name, variety, flower_color, location, status, care_preference, "
                        + "acquired_on, cover_url, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getName(), request.getVariety(), request.getFlowerColor(),
                request.getLocation(), status, request.getCarePreference(), acquiredOn, request.getCoverUrl(), now,
                now, 1001L, 1001L);
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
     * 新增养护记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param plantId 花卉 ID
     * @param request 创建请求
     * @param careDate 养护日期
     * @param nextCareAt 下次养护时间
     * @param now 当前时间
     */
    public void insertCareRecord(Long id, Long familyId, Long plantId, CreatePlantCareRecordRequest request,
            LocalDate careDate, LocalDateTime nextCareAt, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO plant_care_record (id, family_id, plant_id, care_type, care_date, detail, raw_text, "
                        + "next_care_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, plantId, request.getCareType(), careDate, request.getDetail(), request.getRawText(),
                nextCareAt, now, now, 1001L, 1001L);
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

