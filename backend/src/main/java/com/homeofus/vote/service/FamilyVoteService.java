package com.homeofus.vote.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.vote.dto.CreateFamilyVoteRequest;
import com.homeofus.vote.dto.SubmitFamilyVoteRequest;
import com.homeofus.vote.repository.FamilyVoteRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 家庭投票业务服务。
 *
 * @author tanchaohong
 */
@Service
public class FamilyVoteService {

    private final FamilyVoteRepository familyVoteRepository;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public FamilyVoteService(FamilyVoteRepository familyVoteRepository, CurrentUserProvider currentUserProvider,
            IdGenerator idGenerator, TimeProvider timeProvider) {
        this.familyVoteRepository = familyVoteRepository;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建家庭投票。
     *
     * @param request 创建请求
     * @return 新投票 ID
     */
    public Map<String, Object> create(CreateFamilyVoteRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long voteId = idGenerator.nextId();
        String voteCategory = resolveVoteCategory(request.getVoteCategory());
        familyVoteRepository.insertVote(voteId, DefaultFamily.FAMILY_ID, request, voteCategory,
                currentUser.getUserId(), timeProvider.now());
        List<String> options = resolveOptions(voteCategory, request.getOptions());
        for (int index = 0; index < options.size(); index++) {
            familyVoteRepository.insertOption(idGenerator.nextId(), voteId, DefaultFamily.FAMILY_ID, options.get(index),
                    index + 1, currentUser.getUserId(), timeProvider.now());
        }
        return Map.of("id", voteId);
    }

    /**
     * 查询投票列表。
     *
     * @return 投票列表
     */
    public List<Map<String, Object>> findVotes() {
        List<Map<String, Object>> votes = familyVoteRepository.findVotes(DefaultFamily.FAMILY_ID);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> vote : votes) {
            result.add(enrichVote(vote));
        }
        return result;
    }

    /**
     * 提交投票。
     *
     * @param voteId 投票 ID
     * @param request 提交请求
     * @return 更新结果
     */
    public Map<String, Object> submitVote(Long voteId, SubmitFamilyVoteRequest request) {
        Map<String, Object> vote = ensureVoteExists(voteId);
        if (!StringUtils.equals(textValue(vote, "status"), "OPEN")) {
            throw new BusinessException("FAMILY_VOTE_CLOSED", "投票已结束，不能继续投票");
        }
        familyVoteRepository.findOption(DefaultFamily.FAMILY_ID, voteId, request.getOptionId())
                .orElseThrow(() -> new BusinessException("FAMILY_VOTE_OPTION_NOT_FOUND", "投票选项不存在"));
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        familyVoteRepository.upsertChoice(idGenerator.nextId(), voteId, DefaultFamily.FAMILY_ID,
                currentUser.getMemberId(), request.getOptionId(), currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", 1);
    }

    /**
     * 随机确认投票结果。
     *
     * @param voteId 投票 ID
     * @return 确认结果
     */
    public Map<String, Object> decide(Long voteId) {
        Map<String, Object> vote = ensureVoteExists(voteId);
        if (!StringUtils.equals(textValue(vote, "status"), "OPEN")) {
            throw new BusinessException("FAMILY_VOTE_CLOSED", "投票已结束");
        }
        List<Map<String, Object>> options = familyVoteRepository.findOptions(DefaultFamily.FAMILY_ID, voteId);
        if (options.isEmpty()) {
            throw new BusinessException("FAMILY_VOTE_OPTION_NOT_FOUND", "投票选项不存在");
        }
        List<Map<String, Object>> choices = familyVoteRepository.findChoices(DefaultFamily.FAMILY_ID, voteId);
        Long decidedOptionId = chooseOptionId(options, choices);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = familyVoteRepository.decide(DefaultFamily.FAMILY_ID, voteId, decidedOptionId,
                currentUser.getUserId(), timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("decidedOptionId", decidedOptionId);
        result.put("vote", enrichVote(ensureVoteExists(voteId)));
        return result;
    }

    /**
     * 删除投票。
     *
     * @param voteId 投票 ID
     * @return 删除结果
     */
    public Map<String, Object> delete(Long voteId) {
        ensureVoteExists(voteId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        familyVoteRepository.deleteChoices(DefaultFamily.FAMILY_ID, voteId, currentUser.getUserId(), timeProvider.now());
        familyVoteRepository.deleteOptions(DefaultFamily.FAMILY_ID, voteId, currentUser.getUserId(), timeProvider.now());
        int updated = familyVoteRepository.deleteVote(DefaultFamily.FAMILY_ID, voteId, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    private Map<String, Object> enrichVote(Map<String, Object> vote) {
        Long voteId = numberValue(vote, "id");
        List<Map<String, Object>> options = familyVoteRepository.findOptions(DefaultFamily.FAMILY_ID, voteId);
        List<Map<String, Object>> choices = familyVoteRepository.findChoices(DefaultFamily.FAMILY_ID, voteId);
        Map<Long, Long> countMap = choices.stream().collect(Collectors.groupingBy(item -> numberValue(item, "option_id"),
                LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> optionViews = new ArrayList<>();
        for (Map<String, Object> option : options) {
            Map<String, Object> optionView = new LinkedHashMap<>(option);
            optionView.put("voteCount", countMap.getOrDefault(numberValue(option, "id"), 0L));
            optionViews.add(optionView);
        }
        Map<String, Object> voteView = new LinkedHashMap<>(vote);
        voteView.put("options", optionViews);
        voteView.put("choices", choices);
        Long decidedOptionId = vote.containsKey("decided_option_id") ? numberValue(vote, "decided_option_id") : 0L;
        if (decidedOptionId > 0) {
            voteView.put("decidedOptionText", findOptionText(options, decidedOptionId));
        }
        return voteView;
    }

    private String findOptionText(List<Map<String, Object>> options, Long optionId) {
        for (Map<String, Object> option : options) {
            if (numberValue(option, "id").equals(optionId)) {
                return textValue(option, "option_text");
            }
        }
        return "";
    }

    private Long chooseOptionId(List<Map<String, Object>> options, List<Map<String, Object>> choices) {
        Map<Long, Long> countMap = choices.stream().collect(Collectors.groupingBy(item -> numberValue(item, "option_id"),
                LinkedHashMap::new, Collectors.counting()));
        long maxCount = countMap.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> option : options) {
            Long optionId = numberValue(option, "id");
            long count = countMap.getOrDefault(optionId, 0L);
            if (maxCount == 0L || count == maxCount) {
                candidates.add(option);
            }
        }
        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return numberValue(candidates.get(index), "id");
    }

    private Map<String, Object> ensureVoteExists(Long voteId) {
        return familyVoteRepository.findVote(DefaultFamily.FAMILY_ID, voteId)
                .orElseThrow(() -> new BusinessException("FAMILY_VOTE_NOT_FOUND", "投票不存在或已删除"));
    }

    private List<String> resolveOptions(String voteCategory, List<String> rawOptions) {
        List<String> options = new ArrayList<>();
        if (Objects.nonNull(rawOptions)) {
            for (String rawOption : rawOptions) {
                if (StringUtils.isNotBlank(rawOption)) {
                    options.add(rawOption.trim());
                }
            }
        }
        if (!options.isEmpty()) {
            return options;
        }
        if (StringUtils.equals(voteCategory, "EAT")) {
            return List.of("火锅", "串串", "中餐", "烧烤", "炒菜");
        }
        if (StringUtils.equals(voteCategory, "PLAY")) {
            return List.of("喝酒", "看电影", "散步", "桌游", "宅家休息");
        }
        return List.of("选项一", "选项二");
    }

    private String resolveVoteCategory(String voteCategory) {
        if (StringUtils.equalsIgnoreCase(voteCategory, "EAT")) {
            return "EAT";
        }
        if (StringUtils.equalsIgnoreCase(voteCategory, "PLAY")) {
            return "PLAY";
        }
        return "CUSTOM";
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (Objects.isNull(value)) {
            return "";
        }
        return String.valueOf(value);
    }
}
