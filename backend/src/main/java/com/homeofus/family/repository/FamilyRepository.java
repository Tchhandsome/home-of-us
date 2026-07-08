package com.homeofus.family.repository;

import com.homeofus.family.dto.CreateFamilyMemberRequest;
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 家庭空间数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class FamilyRepository {

    private final JdbcTemplate jdbcTemplate;

    public FamilyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询家庭信息。
     *
     * @param familyId 家庭 ID
     * @return 家庭信息
     */
    public Map<String, Object> findFamily(Long familyId) {
        return jdbcTemplate.queryForMap(
                "SELECT id, name, description, created_at, updated_at FROM family WHERE id = ? AND deleted = 0",
                familyId);
    }

    /**
     * 查询家庭成员。
     *
     * @param familyId 家庭 ID
     * @return 成员列表
     */
    public List<Map<String, Object>> findMembers(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT fm.id, fm.family_id, fm.display_name, fm.role_code, fm.avatar_color, fm.avatar_url, fm.bio, "
                        + "au.username, fm.created_at FROM family_member fm "
                        + "LEFT JOIN app_user au ON au.family_member_id = fm.id AND au.deleted = 0 "
                        + "WHERE fm.family_id = ? AND fm.deleted = 0 ORDER BY fm.id",
                familyId);
    }

    /**
     * 查询成员。
     *
     * @param id 成员 ID
     * @param familyId 家庭 ID
     * @return 成员信息
     */
    public Optional<Map<String, Object>> findMember(Long id, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, display_name, role_code, avatar_color, avatar_url, bio, created_at "
                        + "FROM family_member WHERE id = ? AND family_id = ? AND deleted = 0",
                id, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 新增家庭成员。
     *
     * @param id 成员 ID
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertMember(Long id, Long familyId, CreateFamilyMemberRequest request, Long operatorId,
            LocalDateTime now) {
        String roleCode = StringUtils.defaultIfBlank(request.getRoleCode(), "PARTNER");
        jdbcTemplate.update(
                "INSERT INTO family_member (id, family_id, display_name, role_code, avatar_color, avatar_url, bio, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getDisplayName(), roleCode, request.getAvatarColor(), request.getAvatarUrl(),
                request.getBio(), now, now, operatorId, operatorId);
    }

    /**
     * 更新家庭成员。
     *
     * @param id 成员 ID
     * @param familyId 家庭 ID
     * @param request 更新请求
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updateMember(Long id, Long familyId, UpdateFamilyMemberRequest request, Long operatorId,
            LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE family_member SET display_name = COALESCE(NULLIF(?, ''), display_name), "
                        + "role_code = COALESCE(NULLIF(?, ''), role_code), "
                        + "avatar_color = COALESCE(NULLIF(?, ''), avatar_color), "
                        + "avatar_url = COALESCE(NULLIF(?, ''), avatar_url), "
                        + "bio = COALESCE(?, bio), updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                request.getDisplayName(), request.getRoleCode(), request.getAvatarColor(), request.getAvatarUrl(),
                request.getBio(), now, operatorId, id, familyId);
    }
}
