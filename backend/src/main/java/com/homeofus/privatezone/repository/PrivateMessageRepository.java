package com.homeofus.privatezone.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 私密留言数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class PrivateMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public PrivateMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增私密留言。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param senderMemberId 发送成员 ID
     * @param receiverMemberId 接收成员 ID
     * @param visibility 可见范围
     * @param content 内容
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, Long senderMemberId, Long receiverMemberId, String visibility,
            String content, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO private_message (id, family_id, sender_member_id, receiver_member_id, visibility, "
                        + "content, read_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, 0)",
                id, familyId, senderMemberId, receiverMemberId, visibility, content, now, now, operatorId,
                operatorId);
    }

    /**
     * 查询当前成员可见留言。
     *
     * @param familyId 家庭 ID
     * @param memberId 当前成员 ID
     * @return 留言列表
     */
    public List<Map<String, Object>> findVisibleMessages(Long familyId, Long memberId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, sender_member_id, receiver_member_id, visibility, content, read_at, created_at "
                        + "FROM private_message WHERE family_id = ? AND deleted = 0 AND "
                        + "(visibility = 'SHARED' OR sender_member_id = ? OR receiver_member_id = ?) "
                        + "ORDER BY created_at DESC",
                familyId, memberId, memberId);
    }

    /**
     * 标记留言已读。
     *
     * @param id 留言 ID
     * @param familyId 家庭 ID
     * @param receiverMemberId 接收成员 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int markRead(Long id, Long familyId, Long receiverMemberId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE private_message SET read_at = COALESCE(read_at, ?), updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND receiver_member_id = ? AND deleted = 0",
                now, now, operatorId, id, familyId, receiverMemberId);
    }
}
