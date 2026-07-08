package com.homeofus.inventory.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.inventory.dto.CreateInventoryItemRequest;
import com.homeofus.inventory.dto.UpdateInventoryItemRequest;
import com.homeofus.inventory.repository.InventoryRepository;
import com.homeofus.reminder.service.ReminderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 家庭库存业务服务。
 *
 * @author tanchaohong
 */
@Service
public class InventoryService {

    private static final String INVENTORY_LOW_SOURCE_TYPE = "INVENTORY_LOW";

    private static final String INVENTORY_EXPIRE_SOURCE_TYPE = "INVENTORY_EXPIRE";

    private final InventoryRepository inventoryRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public InventoryService(InventoryRepository inventoryRepository, ReminderService reminderService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.inventoryRepository = inventoryRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建库存项。
     *
     * @param request 创建请求
     * @return 新库存项 ID
     */
    public Map<String, Object> create(CreateInventoryItemRequest request) {
        normalizeQuantity(request);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String status = resolveStatus(request.getQuantity(), request.getLowStockThreshold());
        LocalDate expiresOn = parseDate(request.getExpiresOn());
        Integer reminderDaysBefore = normalizeReminderDays(request.getReminderDaysBefore());
        inventoryRepository.insert(id, DefaultFamily.FAMILY_ID, request, resolveItemType(request.getItemType()),
                status, expiresOn, reminderDaysBefore, currentUser.getUserId(), timeProvider.now());
        syncReminders(id, request.getName(), request.getNote(), status, expiresOn, reminderDaysBefore);
        return Map.of("id", id);
    }

    /**
     * 查询库存列表。
     *
     * @return 库存列表
     */
    public List<Map<String, Object>> findItems() {
        return inventoryRepository.findItems(DefaultFamily.FAMILY_ID);
    }

    /**
     * 更新库存项。
     *
     * @param itemId 库存项 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> update(Long itemId, UpdateInventoryItemRequest request) {
        ensureItemExists(itemId);
        normalizeQuantity(request);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String status = resolveStatus(request.getQuantity(), request.getLowStockThreshold());
        LocalDate expiresOn = parseDate(request.getExpiresOn());
        Integer reminderDaysBefore = normalizeReminderDays(request.getReminderDaysBefore());
        int updated = inventoryRepository.update(itemId, DefaultFamily.FAMILY_ID, request,
                resolveItemType(request.getItemType()), status, expiresOn, reminderDaysBefore,
                currentUser.getUserId(), timeProvider.now());
        syncReminders(itemId, request.getName(), request.getNote(), status, expiresOn, reminderDaysBefore);
        return Map.of("updated", updated);
    }

    /**
     * 删除库存项。
     *
     * @param itemId 库存项 ID
     * @return 删除结果
     */
    public Map<String, Object> delete(Long itemId) {
        ensureItemExists(itemId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        deleteReminders(itemId);
        int updated = inventoryRepository.delete(itemId, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    private void syncReminders(Long itemId, String name, String note, String status, LocalDate expiresOn,
            Integer reminderDaysBefore) {
        deleteReminders(itemId);
        // 库存提醒统一走提醒中心，数量恢复或日期变更时由这里回收后重建。
        if (StringUtils.equals(status, "LOW")) {
            reminderService.createFromSource(name + "库存不足", StringUtils.defaultIfBlank(note, "请及时补货"),
                    INVENTORY_LOW_SOURCE_TYPE, itemId, timeProvider.now());
        }
        LocalDateTime expireRemindAt = calculateExpireRemindAt(expiresOn, reminderDaysBefore);
        if (Objects.nonNull(expireRemindAt)) {
            reminderService.createFromSource(name + "临近过期", StringUtils.defaultIfBlank(note, "请尽快处理或食用"),
                    INVENTORY_EXPIRE_SOURCE_TYPE, itemId, expireRemindAt);
        }
    }

    private void deleteReminders(Long itemId) {
        reminderService.deleteBySource(INVENTORY_LOW_SOURCE_TYPE, itemId);
        reminderService.deleteBySource(INVENTORY_EXPIRE_SOURCE_TYPE, itemId);
    }

    private void ensureItemExists(Long itemId) {
        inventoryRepository.findItem(itemId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("INVENTORY_ITEM_NOT_FOUND", "库存项不存在或已删除"));
    }

    private String resolveItemType(String itemType) {
        if (StringUtils.equalsIgnoreCase(itemType, "FOOD")) {
            return "FOOD";
        }
        return "SUPPLY";
    }

    private String resolveStatus(BigDecimal quantity, BigDecimal lowStockThreshold) {
        if (Objects.nonNull(lowStockThreshold) && quantity.compareTo(lowStockThreshold) <= 0) {
            return "LOW";
        }
        return "NORMAL";
    }

    private Integer normalizeReminderDays(Integer reminderDaysBefore) {
        if (Objects.isNull(reminderDaysBefore) || reminderDaysBefore < 0) {
            return 0;
        }
        return reminderDaysBefore;
    }

    private LocalDateTime calculateExpireRemindAt(LocalDate expiresOn, Integer reminderDaysBefore) {
        if (Objects.isNull(expiresOn)) {
            return null;
        }
        LocalDateTime remindAt = LocalDateTime.of(expiresOn.minusDays(reminderDaysBefore), LocalTime.of(9, 0));
        if (remindAt.isBefore(timeProvider.now())) {
            return timeProvider.now();
        }
        return remindAt;
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return timeProvider.today();
        }
    }

    private void normalizeQuantity(CreateInventoryItemRequest request) {
        if (Objects.isNull(request.getQuantity())) {
            request.setQuantity(BigDecimal.ZERO);
        }
    }

    private void normalizeQuantity(UpdateInventoryItemRequest request) {
        if (Objects.isNull(request.getQuantity())) {
            request.setQuantity(BigDecimal.ZERO);
        }
    }
}
