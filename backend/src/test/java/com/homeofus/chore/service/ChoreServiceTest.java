package com.homeofus.chore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import com.homeofus.chore.dto.UpdateChoreTaskRequest;
import com.homeofus.chore.repository.ChoreRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 待办服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class ChoreServiceTest {

    @Mock
    private ChoreRepository choreRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private ChoreService choreService;

    @BeforeEach
    void setUp() {
        choreService = new ChoreService(choreRepository, reminderService, currentUserProvider, idGenerator, timeProvider);
    }

    @Test
    void shouldCreatePersonalTaskForCurrentMember() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 10, 0);
        CreateChoreTaskRequest request = new CreateChoreTaskRequest();
        request.setTitle("缴纳物业费");
        request.setTaskScope("PERSONAL");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(8001L);
        when(timeProvider.now()).thenReturn(now);

        Map<String, Object> result = choreService.create(request);

        assertEquals(8001L, result.get("id"));
        verify(choreRepository).insert(8001L, DefaultFamily.FAMILY_ID, request, "PERSONAL", 1001L, 1001L,
                "TEMPORARY", null, 9001L, now);
        verify(reminderService).deleteBySource("TODO_TASK", 8001L);
        verify(reminderService, never()).createFromSource(eq("缴纳物业费"), eq("家庭待办"), eq("TODO_TASK"), eq(8001L),
                eq(now));
    }

    @Test
    void shouldClaimSharedTaskWithCurrentMember() {
        CurrentUser currentUser = new CurrentUser(9002L, DefaultFamily.FAMILY_ID, 1002L, "dandan", "丹丹");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 10, 30);

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(choreRepository.findTask(DefaultFamily.FAMILY_ID, 8101L))
                .thenReturn(Optional.of(Map.of("id", 8101L, "task_scope", "SHARED")));
        when(choreRepository.claim(DefaultFamily.FAMILY_ID, 8101L, 1002L, 9002L, now)).thenReturn(1);

        Map<String, Object> result = choreService.claim(8101L);

        assertEquals(1, result.get("updated"));
        verify(choreRepository).claim(DefaultFamily.FAMILY_ID, 8101L, 1002L, 9002L, now);
    }

    @Test
    void shouldUpdateReminderWhenUpdatingTask() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 11, 0);
        UpdateChoreTaskRequest request = new UpdateChoreTaskRequest();
        request.setTitle("周末大扫除");
        request.setTaskScope("SHARED");
        request.setNote("周六上午一起做");
        request.setDueAt("2026-07-12T09:00");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(choreRepository.findTask(DefaultFamily.FAMILY_ID, 8201L))
                .thenReturn(Optional.of(Map.of("id", 8201L, "task_scope", "SHARED")));
        when(choreRepository.update(DefaultFamily.FAMILY_ID, 8201L, request, "SHARED", 1001L, null, "TEMPORARY",
                LocalDateTime.of(2026, 7, 12, 9, 0), 9001L, now)).thenReturn(1);

        Map<String, Object> result = choreService.update(8201L, request);

        assertEquals(1, result.get("updated"));
        verify(reminderService).deleteBySource("TODO_TASK", 8201L);
        verify(reminderService, never()).createFromSource("周末大扫除", "周六上午一起做", "TODO_TASK", 8201L,
                LocalDateTime.of(2026, 7, 12, 9, 0));
    }

    @Test
    void shouldCompleteTaskAndClearReminder() {
        CurrentUser currentUser = new CurrentUser(9002L, DefaultFamily.FAMILY_ID, 1002L, "dandan", "丹丹");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 12, 0);

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(choreRepository.findTask(DefaultFamily.FAMILY_ID, 8301L))
                .thenReturn(Optional.of(Map.of("id", 8301L, "task_scope", "SHARED")));
        when(choreRepository.complete(DefaultFamily.FAMILY_ID, 8301L, 1002L, 9002L, now)).thenReturn(1);

        Map<String, Object> result = choreService.complete(8301L);

        assertEquals(1, result.get("updated"));
        verify(choreRepository).complete(DefaultFamily.FAMILY_ID, 8301L, 1002L, 9002L, now);
        verify(reminderService).deleteBySource("TODO_TASK", 8301L);
    }
}
