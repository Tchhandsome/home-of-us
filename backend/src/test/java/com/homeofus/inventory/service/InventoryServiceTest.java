package com.homeofus.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
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
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 库存服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, reminderService, currentUserProvider, idGenerator,
                timeProvider);
    }

    @Test
    void shouldCreateLowStockAndExpireReminder() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 0);
        CreateInventoryItemRequest request = new CreateInventoryItemRequest();
        request.setName("猫粮");
        request.setItemType("FOOD");
        request.setQuantity(BigDecimal.ONE);
        request.setLowStockThreshold(new BigDecimal("2"));
        request.setExpiresOn("2026-07-15");
        request.setReminderDaysBefore(2);
        request.setNote("注意补货");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(9201L);
        when(timeProvider.now()).thenReturn(now);

        Map<String, Object> result = inventoryService.create(request);

        assertEquals(9201L, result.get("id"));
        verify(inventoryRepository).insert(9201L, DefaultFamily.FAMILY_ID, request, "FOOD", "LOW",
                LocalDate.of(2026, 7, 15), 2, 9001L, now);
        verify(reminderService).createFromSource("猫粮库存不足", "注意补货", "INVENTORY_LOW", 9201L, now);
        verify(reminderService).createFromSource("猫粮临近过期", "注意补货", "INVENTORY_EXPIRE", 9201L,
                LocalDateTime.of(2026, 7, 13, 9, 0));
    }

    @Test
    void shouldClearLowStockReminderAfterUpdate() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 30);
        UpdateInventoryItemRequest request = new UpdateInventoryItemRequest();
        request.setName("洗衣液");
        request.setQuantity(new BigDecimal("6"));
        request.setLowStockThreshold(new BigDecimal("2"));

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(inventoryRepository.findItem(9202L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 9202L)));
        when(inventoryRepository.update(9202L, DefaultFamily.FAMILY_ID, request, "SUPPLY", "NORMAL", null, 0, 9001L,
                now)).thenReturn(1);

        Map<String, Object> result = inventoryService.update(9202L, request);

        assertEquals(1, result.get("updated"));
        verify(reminderService).deleteBySource("INVENTORY_LOW", 9202L);
        verify(reminderService).deleteBySource("INVENTORY_EXPIRE", 9202L);
        verify(reminderService, never()).createFromSource("洗衣液库存不足", "请及时补货", "INVENTORY_LOW", 9202L, now);
    }
}
