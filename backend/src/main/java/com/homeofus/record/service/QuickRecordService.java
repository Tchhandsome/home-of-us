package com.homeofus.record.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.record.dto.CreateQuickRecordRequest;
import com.homeofus.record.repository.QuickRecordRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 快速记录业务服务。
 *
 * @author tanchaohong
 */
@Service
public class QuickRecordService {

    private final QuickRecordRepository quickRecordRepository;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    private final CurrentUserProvider currentUserProvider;

    public QuickRecordService(QuickRecordRepository quickRecordRepository, IdGenerator idGenerator,
            TimeProvider timeProvider, CurrentUserProvider currentUserProvider) {
        this.quickRecordRepository = quickRecordRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 创建快速记录。
     *
     * @param request 创建请求
     * @return 新记录 ID
     */
    public Map<String, Object> create(CreateQuickRecordRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String recordType = StringUtils.defaultIfBlank(request.getRecordType(), "GENERAL");
        LocalDate happenedOn = parseHappenedOn(request.getHappenedOn());
        // 快速记录必须先保留原文，结构化能力可以后续逐步增强。
        quickRecordRepository.insert(id, DefaultFamily.FAMILY_ID, request, recordType, happenedOn,
                currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询最近记录。
     *
     * @param limit 查询数量
     * @return 最近记录列表
     */
    public List<Map<String, Object>> findRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return quickRecordRepository.findRecent(DefaultFamily.FAMILY_ID, safeLimit);
    }

    /**
     * 删除快速记录。
     *
     * @param id 记录 ID
     * @return 更新结果
     */
    public Map<String, Object> delete(Long id) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = quickRecordRepository.delete(id, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        if (updated == 0) {
            throw new BusinessException("RECORD_NOT_FOUND", "记录不存在或已删除");
        }
        return Map.of("updated", updated);
    }

    private LocalDate parseHappenedOn(String happenedOn) {
        if (StringUtils.isBlank(happenedOn)) {
            return timeProvider.today();
        }
        try {
            return LocalDate.parse(happenedOn);
        } catch (DateTimeParseException exception) {
            if (Objects.nonNull(exception.getParsedString())) {
                return timeProvider.today();
            }
            return timeProvider.today();
        }
    }
}
