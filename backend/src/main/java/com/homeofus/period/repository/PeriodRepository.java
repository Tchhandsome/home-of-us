package com.homeofus.period.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 月经管理数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class PeriodRepository {

    private final JdbcTemplate jdbcTemplate;

    public PeriodRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询成员周期资料。
     *
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @return 周期资料
     */
    public Optional<Map<String, Object>> findProfile(Long familyId, Long memberId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, member_id, cycle_days, period_days, last_period_start, reminder_enabled, "
                        + "reminder_time, note, created_at, updated_at FROM period_profile "
                        + "WHERE family_id = ? AND member_id = ? AND deleted = 0",
                familyId, memberId);
        return rows.stream().findFirst();
    }

    /**
     * 新增周期资料。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @param cycleDays 周期天数
     * @param periodDays 经期天数
     * @param lastPeriodStart 最近一次开始日期
     * @param reminderEnabled 是否提醒
     * @param reminderTime 提醒时间
     * @param note 备注
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertProfile(Long id, Long familyId, Long memberId, Integer cycleDays, Integer periodDays,
            LocalDate lastPeriodStart, boolean reminderEnabled, String reminderTime, String note, Long operatorId,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO period_profile (id, family_id, member_id, cycle_days, period_days, last_period_start, "
                        + "reminder_enabled, reminder_time, note, created_at, updated_at, created_by, updated_by, "
                        + "deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, memberId, cycleDays, periodDays, lastPeriodStart, reminderEnabled, reminderTime, note,
                now, now, operatorId, operatorId);
    }

    /**
     * 更新周期资料。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @param cycleDays 周期天数
     * @param periodDays 经期天数
     * @param lastPeriodStart 最近一次开始日期
     * @param reminderEnabled 是否提醒
     * @param reminderTime 提醒时间
     * @param note 备注
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updateProfile(Long id, Long familyId, Long memberId, Integer cycleDays, Integer periodDays,
            LocalDate lastPeriodStart, boolean reminderEnabled, String reminderTime, String note, Long operatorId,
            LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE period_profile SET cycle_days = ?, period_days = ?, last_period_start = ?, "
                        + "reminder_enabled = ?, reminder_time = ?, note = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND member_id = ? AND deleted = 0",
                cycleDays, periodDays, lastPeriodStart, reminderEnabled, reminderTime, note, now, operatorId, id,
                familyId, memberId);
    }

    /**
     * 查询周期记录。
     *
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @param limit 查询数量
     * @return 周期记录
     */
    public List<Map<String, Object>> findRecords(Long familyId, Long memberId, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, member_id, start_on, end_on, is_predicted, note, created_at "
                        + "FROM period_record WHERE family_id = ? AND member_id = ? AND deleted = 0 "
                        + "ORDER BY start_on DESC, created_at DESC LIMIT ?",
                familyId, memberId, limit);
    }

    /**
     * 查询最新周期记录。
     *
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @return 最新记录
     */
    public Optional<Map<String, Object>> findLatestRecord(Long familyId, Long memberId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, member_id, start_on, end_on, is_predicted, note, created_at "
                        + "FROM period_record WHERE family_id = ? AND member_id = ? AND deleted = 0 "
                        + "ORDER BY start_on DESC, created_at DESC LIMIT 1",
                familyId, memberId);
        return rows.stream().findFirst();
    }

    /**
     * 新增周期记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @param startOn 开始日期
     * @param endOn 结束日期
     * @param note 备注
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertRecord(Long id, Long familyId, Long memberId, LocalDate startOn, LocalDate endOn, String note,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO period_record (id, family_id, member_id, start_on, end_on, is_predicted, note, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, 0)",
                id, familyId, memberId, startOn, endOn, note, now, now, operatorId, operatorId);
    }
}
