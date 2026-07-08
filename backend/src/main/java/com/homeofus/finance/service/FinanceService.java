package com.homeofus.finance.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.finance.dto.CreateFinanceRecordRequest;
import com.homeofus.finance.repository.FinanceRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 家庭账本业务服务。
 *
 * @author tanchaohong
 */
@Service
public class FinanceService {

    private final FinanceRepository financeRepository;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    private final CurrentUserProvider currentUserProvider;

    public FinanceService(FinanceRepository financeRepository, IdGenerator idGenerator, TimeProvider timeProvider,
            CurrentUserProvider currentUserProvider) {
        this.financeRepository = financeRepository;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 创建账本记录。
     *
     * @param request 创建请求
     * @return 新账本记录 ID
     */
    public Map<String, Object> create(CreateFinanceRecordRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String direction = StringUtils.defaultIfBlank(request.getDirection(), "EXPENSE");
        Long ownerId = Objects.isNull(request.getOwnerId()) ? currentUser.getMemberId() : request.getOwnerId();
        financeRepository.insert(id, DefaultFamily.FAMILY_ID, request, direction, ownerId, currentUser.getUserId(),
                parseDate(request.getOccurredOn()), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询账本记录。
     *
     * @return 账本记录
     */
    public List<Map<String, Object>> findRecords() {
        return financeRepository.findRecords(DefaultFamily.FAMILY_ID);
    }

    /**
     * 查询月度账本摘要。
     *
     * @param month 月份
     * @return 摘要列表
     */
    public List<Map<String, Object>> summarizeMonth(String month) {
        String safeMonth = StringUtils.defaultIfBlank(month, timeProvider.today().toString().substring(0, 7));
        return financeRepository.summarizeMonth(DefaultFamily.FAMILY_ID, safeMonth);
    }

    /**
     * 查询账本摘要。
     *
     * @return 摘要
     */
    public Map<String, Object> summary() {
        LocalDate today = timeProvider.today();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String monthPrefix = today.toString().substring(0, 7);
        return financeRepository.loadSummary(DefaultFamily.FAMILY_ID, monthPrefix, weekStart, weekEnd);
    }

    /**
     * 查询默认账本分类。
     *
     * @return 分类列表
     */
    public List<Map<String, Object>> findCategories() {
        return List.of(
                category("MEAL", "餐饮"),
                category("DAILY", "日用"),
                category("FLOWER", "花卉"),
                category("TRAFFIC", "交通"),
                category("HOME", "居家"),
                category("ENTERTAINMENT", "娱乐"),
                category("DIGITAL", "数码"),
                category("CLOTHES", "服饰"),
                category("OTHER", "其他"));
    }

    /**
     * 删除账本记录。
     *
     * @param id 记录 ID
     * @return 删除结果
     */
    public Map<String, Object> delete(Long id) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = financeRepository.delete(id, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        if (updated == 0) {
            throw new BusinessException("FINANCE_RECORD_NOT_FOUND", "账本记录不存在或已删除");
        }
        return Map.of("updated", updated);
    }

    private Map<String, Object> category(String code, String name) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("name", name);
        return result;
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return timeProvider.today();
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return timeProvider.today();
        }
    }
}
