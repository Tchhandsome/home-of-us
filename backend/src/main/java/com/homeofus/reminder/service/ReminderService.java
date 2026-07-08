package com.homeofus.reminder.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.reminder.dto.CreateReminderRequest;
import com.homeofus.reminder.repository.ReminderRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 提醒业务服务。
 *
 * @author tanchaohong
 */
@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public ReminderService(ReminderRepository reminderRepository, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.reminderRepository = reminderRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建提醒。
     *
     * @param request 创建请求
     * @return 新提醒 ID
     */
    public Map<String, Object> create(CreateReminderRequest request) {
        Long id = idGenerator.nextId();
        LocalDateTime dueAt = parseDueAt(request.getDueAt());
        String sourceType = StringUtils.defaultIfBlank(request.getSourceType(), "MANUAL");
        reminderRepository.insert(id, DefaultFamily.FAMILY_ID, request, sourceType, dueAt, timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 从业务来源创建提醒。
     *
     * @param title 标题
     * @param description 描述
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param dueAt 到期时间
     * @return 提醒 ID
     */
    public Long createFromSource(String title, String description, String sourceType, Long sourceId,
            LocalDateTime dueAt) {
        Long id = idGenerator.nextId();
        // 来源提醒让业务模块只关心语义，不重复维护提醒表写入细节。
        reminderRepository.insertFromSource(id, DefaultFamily.FAMILY_ID, title, description, sourceType, sourceId,
                dueAt, timeProvider.now());
        return id;
    }

    /**
     * 查询提醒列表。
     *
     * @param limit 查询数量
     * @return 提醒列表
     */
    public List<Map<String, Object>> findReminders(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return reminderRepository.findReminders(DefaultFamily.FAMILY_ID, safeLimit);
    }

    /**
     * 完成提醒。
     *
     * @param id 提醒 ID
     * @return 更新结果
     */
    public Map<String, Object> complete(Long id) {
        int updated = reminderRepository.complete(id, timeProvider.now());
        return Map.of("updated", updated);
    }

    private LocalDateTime parseDueAt(String dueAt) {
        try {
            return LocalDateTime.parse(dueAt);
        } catch (DateTimeParseException exception) {
            return timeProvider.now().plusDays(1);
        }
    }
}

