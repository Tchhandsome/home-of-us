package com.homeofus.vote.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.vote.dto.CreateFamilyVoteRequest;
import com.homeofus.vote.dto.SubmitFamilyVoteRequest;
import com.homeofus.vote.service.FamilyVoteService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家庭投票接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/family-votes")
public class FamilyVoteController {

    private final FamilyVoteService familyVoteService;

    public FamilyVoteController(FamilyVoteService familyVoteService) {
        this.familyVoteService = familyVoteService;
    }

    /**
     * 创建家庭投票。
     *
     * @param request 创建请求
     * @return 新投票 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateFamilyVoteRequest request) {
        return ApiResponse.ok(familyVoteService.create(request));
    }

    /**
     * 查询家庭投票。
     *
     * @return 投票列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findVotes() {
        return ApiResponse.ok(familyVoteService.findVotes());
    }

    /**
     * 提交投票。
     *
     * @param voteId 投票 ID
     * @param request 投票请求
     * @return 更新结果
     */
    @PatchMapping("/{voteId}/vote")
    public ApiResponse<Map<String, Object>> submitVote(@PathVariable Long voteId,
            @Valid @RequestBody SubmitFamilyVoteRequest request) {
        return ApiResponse.ok(familyVoteService.submitVote(voteId, request));
    }

    /**
     * 随机确认投票结果。
     *
     * @param voteId 投票 ID
     * @return 更新结果
     */
    @PatchMapping("/{voteId}/decide")
    public ApiResponse<Map<String, Object>> decide(@PathVariable Long voteId) {
        return ApiResponse.ok(familyVoteService.decide(voteId));
    }

    /**
     * 删除投票。
     *
     * @param voteId 投票 ID
     * @return 删除结果
     */
    @DeleteMapping("/{voteId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long voteId) {
        return ApiResponse.ok(familyVoteService.delete(voteId));
    }
}
