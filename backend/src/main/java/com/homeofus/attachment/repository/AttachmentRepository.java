package com.homeofus.attachment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 资料库附件数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class AttachmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询附件元数据。
     *
     * @param familyId 家庭 ID
     * @return 附件列表
     */
    public List<Map<String, Object>> findFiles(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, file_name, file_type, storage_path, linked_type, linked_id, created_at "
                        + "FROM attachment_file WHERE family_id = ? AND deleted = 0 ORDER BY created_at DESC",
                familyId);
    }

    /**
     * 新增附件元数据。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param storagePath 访问路径
     * @param linkedType 关联类型
     * @param linkedId 关联 ID
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, String fileName, String fileType, String storagePath, String linkedType,
            Long linkedId, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO attachment_file (id, family_id, file_name, file_type, storage_path, linked_type, "
                        + "linked_id, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, fileName, fileType, storagePath, linkedType, linkedId, now, now, operatorId,
                operatorId);
    }
}
