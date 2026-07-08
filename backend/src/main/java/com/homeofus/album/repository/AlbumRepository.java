package com.homeofus.album.repository;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 共同相册数据访问。
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
     * 新增照片记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param takenOn 拍摄日期
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertPhoto(Long id, Long familyId, CreateAlbumPhotoRequest request, LocalDate takenOn,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO album_photo (id, family_id, title, image_url, description, taken_on, created_at, "
                        + "updated_at, created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), request.getImageUrl(), request.getDescription(), takenOn, now, now,
                operatorId, operatorId);
    }

    /**
     * 查询共同相册。
     *
     * @param familyId 家庭 ID
     * @return 照片列表
     */
    public List<Map<String, Object>> findPhotos(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, title, image_url, description, taken_on, created_at, created_by "
                        + "FROM album_photo WHERE family_id = ? AND deleted = 0 "
                        + "ORDER BY COALESCE(taken_on, DATE(created_at)) DESC, created_at DESC",
                familyId);
    }
}
