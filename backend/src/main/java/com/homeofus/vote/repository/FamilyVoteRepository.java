package com.homeofus.vote.repository;

import com.homeofus.vote.dto.CreateFamilyVoteRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 家庭投票数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class FamilyVoteRepository {

    private final JdbcTemplate jdbcTemplate;

    public FamilyVoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增投票。
     *
     * @param id 投票 ID
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param voteCategory 分类
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertVote(Long id, Long familyId, CreateFamilyVoteRequest request, String voteCategory,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO family_vote (id, family_id, title, vote_category, status, decided_option_id, decided_at, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, 'OPEN', NULL, NULL, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), voteCategory, now, now, operatorId, operatorId);
    }

    /**
     * 新增投票选项。
     *
     * @param id 选项 ID
     * @param voteId 投票 ID
     * @param familyId 家庭 ID
     * @param optionText 选项内容
     * @param sortOrder 排序
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertOption(Long id, Long voteId, Long familyId, String optionText, int sortOrder, Long operatorId,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO family_vote_option (id, vote_id, family_id, option_text, sort_order, created_at, "
                        + "updated_at, created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, voteId, familyId, optionText, sortOrder, now, now, operatorId, operatorId);
    }

    /**
     * 查询投票列表。
     *
     * @param familyId 家庭 ID
     * @return 投票列表
     */
    public List<Map<String, Object>> findVotes(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT vote.id, vote.family_id, vote.title, vote.vote_category, vote.status, vote.decided_option_id, "
                        + "vote.decided_at, vote.created_at FROM family_vote vote "
                        + "WHERE vote.family_id = ? AND vote.deleted = 0 "
                        + "ORDER BY CASE WHEN vote.status = 'OPEN' THEN 0 ELSE 1 END ASC, vote.created_at DESC",
                familyId);
    }

    /**
     * 查询单个投票。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @return 投票
     */
    public Optional<Map<String, Object>> findVote(Long familyId, Long voteId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, title, vote_category, status, decided_option_id, decided_at "
                        + "FROM family_vote WHERE id = ? AND family_id = ? AND deleted = 0",
                voteId, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 查询投票选项。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @return 选项列表
     */
    public List<Map<String, Object>> findOptions(Long familyId, Long voteId) {
        return jdbcTemplate.queryForList(
                "SELECT id, vote_id, option_text, sort_order FROM family_vote_option "
                        + "WHERE family_id = ? AND vote_id = ? AND deleted = 0 ORDER BY sort_order ASC, id ASC",
                familyId, voteId);
    }

    /**
     * 查询单个投票选项。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @param optionId 选项 ID
     * @return 选项
     */
    public Optional<Map<String, Object>> findOption(Long familyId, Long voteId, Long optionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, vote_id, option_text, sort_order FROM family_vote_option "
                        + "WHERE id = ? AND family_id = ? AND vote_id = ? AND deleted = 0",
                optionId, familyId, voteId);
        return rows.stream().findFirst();
    }

    /**
     * 查询投票结果。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @return 投票明细
     */
    public List<Map<String, Object>> findChoices(Long familyId, Long voteId) {
        return jdbcTemplate.queryForList(
                "SELECT choice.id, choice.vote_id, choice.member_id, choice.option_id, member.display_name AS member_name "
                        + "FROM family_vote_choice choice LEFT JOIN family_member member "
                        + "ON member.id = choice.member_id AND member.deleted = 0 "
                        + "WHERE choice.family_id = ? AND choice.vote_id = ? AND choice.deleted = 0 "
                        + "ORDER BY choice.created_at ASC",
                familyId, voteId);
    }

    /**
     * 保存成员投票。
     *
     * @param id 记录 ID
     * @param voteId 投票 ID
     * @param familyId 家庭 ID
     * @param memberId 成员 ID
     * @param optionId 选项 ID
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void upsertChoice(Long id, Long voteId, Long familyId, Long memberId, Long optionId, Long operatorId,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO family_vote_choice (id, vote_id, family_id, member_id, option_id, created_at, updated_at, "
                        + "created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0) "
                        + "ON DUPLICATE KEY UPDATE option_id = VALUES(option_id), updated_at = VALUES(updated_at), "
                        + "updated_by = VALUES(updated_by), deleted = 0",
                id, voteId, familyId, memberId, optionId, now, now, operatorId, operatorId);
    }

    /**
     * 确认投票结果。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @param optionId 结果选项 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int decide(Long familyId, Long voteId, Long optionId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE family_vote SET status = 'DECIDED', decided_option_id = ?, decided_at = ?, updated_at = ?, "
                        + "updated_by = ? WHERE id = ? AND family_id = ? AND deleted = 0",
                optionId, now, now, operatorId, voteId, familyId);
    }

    /**
     * 删除投票。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteVote(Long familyId, Long voteId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE family_vote SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, voteId, familyId);
    }

    /**
     * 删除投票选项。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteOptions(Long familyId, Long voteId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE family_vote_option SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND vote_id = ? AND deleted = 0",
                now, operatorId, familyId, voteId);
    }

    /**
     * 删除投票选择。
     *
     * @param familyId 家庭 ID
     * @param voteId 投票 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteChoices(Long familyId, Long voteId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE family_vote_choice SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND vote_id = ? AND deleted = 0",
                now, operatorId, familyId, voteId);
    }
}
