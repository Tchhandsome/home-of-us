package com.homeofus.album.repository;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import com.homeofus.album.dto.UpdateMemoryEntryRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 时刻墙数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class AlbumRepository {

    private final JdbcTemplate jdbcTemplate;

    public AlbumRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增时刻墙条目。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param entryType 条目类型
     * @param takenOn 发生日期
     * @param reminderEnabled 是否开启提醒
     * @param reminderDaysBefore 提前天数
     * @param nextRemindAt 下次提醒时间
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertPhoto(Long id, Long familyId, CreateAlbumPhotoRequest request, String entryType, LocalDate takenOn,
            boolean reminderEnabled, Integer reminderDaysBefore, LocalDateTime nextRemindAt, Long operatorId,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO album_photo (id, family_id, entry_type, title, image_url, description, wish_text, "
                        + "reminder_enabled, reminder_days_before, next_remind_at, taken_on, created_at, updated_at, "
                        + "created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, entryType, request.getTitle(), request.getImageUrl(), request.getDescription(),
                request.getWishText(), reminderEnabled, reminderDaysBefore, nextRemindAt, takenOn, now, now,
                operatorId, operatorId);
    }

    /**
     * 查询时刻墙条目列表。
     *
     * @param familyId 家庭 ID
     * @return 条目列表
     */
    public List<Map<String, Object>> findPhotos(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, entry_type, title, image_url, description, wish_text, reminder_enabled, "
                        + "reminder_days_before, next_remind_at, taken_on, created_at, created_by FROM album_photo "
                        + "WHERE family_id = ? AND deleted = 0 "
                        + "ORDER BY COALESCE(taken_on, DATE(created_at)) DESC, created_at DESC",
                familyId);
    }

    /**
     * 查询单个时刻墙条目。
     *
     * @param id 条目 ID
     * @param familyId 家庭 ID
     * @return 条目
     */
    public Optional<Map<String, Object>> findPhoto(Long id, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, entry_type, title, image_url, description, wish_text, reminder_enabled, "
                        + "reminder_days_before, next_remind_at, taken_on FROM album_photo "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                id, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 更新时刻墙条目。
     *
     * @param id 条目 ID
     * @param familyId 家庭 ID
     * @param request 更新请求
     * @param entryType 条目类型
     * @param takenOn 发生日期
     * @param reminderEnabled 是否开启提醒
     * @param reminderDaysBefore 提前天数
     * @param nextRemindAt 下次提醒时间
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updatePhoto(Long id, Long familyId, UpdateMemoryEntryRequest request, String entryType, LocalDate takenOn,
            boolean reminderEnabled, Integer reminderDaysBefore, LocalDateTime nextRemindAt, Long operatorId,
            LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE album_photo SET entry_type = ?, title = ?, image_url = ?, description = ?, wish_text = ?, "
                        + "reminder_enabled = ?, reminder_days_before = ?, next_remind_at = ?, taken_on = ?, "
                        + "updated_at = ?, updated_by = ? WHERE id = ? AND family_id = ? AND deleted = 0",
                entryType, request.getTitle(), request.getImageUrl(), request.getDescription(), request.getWishText(),
                reminderEnabled, reminderDaysBefore, nextRemindAt, takenOn, now, operatorId, id, familyId);
    }

    /**
     * 删除时刻墙条目。
     *
     * @param id 条目 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deletePhoto(Long id, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE album_photo SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, id, familyId);
    }
}
