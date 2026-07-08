package com.homeofus.chore.service;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import com.homeofus.chore.repository.ChoreRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 家务任务业务服务。
 *
 * @author tanchaohong
 */
@Service
public class ChoreService {

    private final ChoreRepository choreRepository;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public ChoreService(ChoreRepository choreRepository, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.choreRepository = choreRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建家务任务。
     *
     * @param request 创建请求
     * @return 新任务 ID
     */
    public Map<String, Object> create(CreateChoreTaskRequest request) {
        Long id = idGenerator.nextId();
        String taskType = StringUtils.defaultIfBlank(request.getTaskType(), "TEMPORARY");
        choreRepository.insert(id, DefaultFamily.FAMILY_ID, request, taskType, parseDueAt(request.getDueAt()),
                timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询家务任务。
     *
     * @return 家务任务列表
     */
    public List<Map<String, Object>> findTasks() {
        return choreRepository.findTasks(DefaultFamily.FAMILY_ID);
    }

    private LocalDateTime parseDueAt(String dueAt) {
        if (StringUtils.isBlank(dueAt)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dueAt);
        } catch (DateTimeParseException exception) {
            return timeProvider.now().plusDays(1);
        }
    }
}

