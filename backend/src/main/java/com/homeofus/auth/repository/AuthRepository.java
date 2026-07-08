package com.homeofus.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 账号认证数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按登录名查询有效账号。
     *
     * @param username 登录名
     * @return 账号信息
     */
    public Optional<Map<String, Object>> findByUsername(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, family_member_id, username, password_hash, display_name, session_token "
                        + "FROM app_user WHERE username = ? AND status = 'ACTIVE' AND deleted = 0",
                username);
        return rows.stream().findFirst();
    }

    /**
     * 按会话 token 查询有效账号。
     *
     * @param token 会话 token
     * @return 账号信息
     */
    public Optional<Map<String, Object>> findByToken(String token) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, family_member_id, username, display_name, session_token "
                        + "FROM app_user WHERE session_token = ? AND status = 'ACTIVE' AND deleted = 0",
                token);
        return rows.stream().findFirst();
    }

    /**
     * 按成员 ID 查询账号。
     *
     * @param memberId 成员 ID
     * @return 账号信息
     */
    public Optional<Map<String, Object>> findByMemberId(Long memberId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, family_member_id, username, display_name FROM app_user "
                        + "WHERE family_member_id = ? AND status = 'ACTIVE' AND deleted = 0",
                memberId);
        return rows.stream().findFirst();
    }

    /**
     * 更新会话 token。
     *
     * @param id 用户 ID
     * @param token 会话 token
     * @param now 当前时间
     */
    public void updateSessionToken(Long id, String token, LocalDateTime now) {
        jdbcTemplate.update(
                "UPDATE app_user SET session_token = ?, last_login_at = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND deleted = 0",
                token, now, now, id, id);
    }

    /**
     * 新增登录账号。
     *
     * @param id 用户 ID
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @param username 登录名
     * @param passwordHash 密码摘要
     * @param displayName 显示名称
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertUser(Long id, Long familyId, Long memberId, String username, String passwordHash,
            String displayName, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO app_user (id, family_id, family_member_id, username, password_hash, display_name, "
                        + "session_token, last_login_at, status, created_at, updated_at, created_by, updated_by, "
                        + "deleted) VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, 'ACTIVE', ?, ?, ?, ?, 0)",
                id, familyId, memberId, username, passwordHash, displayName, now, now, operatorId, operatorId);
    }

    /**
     * 更新成员绑定账号。
     *
     * @param memberId 成员 ID
     * @param username 登录名
     * @param passwordHash 密码摘要
     * @param displayName 显示名
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updateUserByMemberId(Long memberId, String username, String passwordHash, String displayName,
            Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE app_user SET username = COALESCE(NULLIF(?, ''), username), "
                        + "password_hash = COALESCE(NULLIF(?, ''), password_hash), "
                        + "display_name = COALESCE(NULLIF(?, ''), display_name), updated_at = ?, updated_by = ? "
                        + "WHERE family_member_id = ? AND status = 'ACTIVE' AND deleted = 0",
                username, passwordHash, displayName, now, operatorId, memberId);
    }
}
