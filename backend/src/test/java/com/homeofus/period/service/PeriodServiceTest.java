package com.homeofus.period.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.period.dto.CreatePeriodRecordRequest;
import com.homeofus.period.dto.SavePeriodProfileRequest;
import com.homeofus.period.repository.PeriodRepository;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 月经管理服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    @Mock
    private PeriodRepository periodRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private PeriodService periodService;

    @BeforeEach
    void setUp() {
        periodService = new PeriodService(periodRepository, reminderService, currentUserProvider, idGenerator,
                timeProvider);
    }

    @Test
    void shouldSaveProfileAndReturnPrediction() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 8, 30);
        SavePeriodProfileRequest request = new SavePeriodProfileRequest();
        request.setCycleDays(30);
        request.setPeriodDays(6);
        request.setLastPeriodStart("2026-07-01");
        request.setReminderEnabled(true);
        request.setReminderTime("08:45");
        request.setNote("关注状态");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(periodRepository.findProfile(DefaultFamily.FAMILY_ID, 1001L)).thenReturn(Optional.empty());
        when(periodRepository.findRecords(DefaultFamily.FAMILY_ID, 1001L, 18)).thenReturn(List.of());
        when(idGenerator.nextId()).thenReturn(8801L);
        when(timeProvider.now()).thenReturn(now);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 7, 9));

        Map<String, Object> result = periodService.saveProfile(request);

        Map<String, Object> profile = cast(result.get("profile"));
        Map<String, Object> prediction = cast(result.get("prediction"));

        assertEquals(30, profile.get("cycle_days"));
        assertEquals(LocalDate.of(2026, 7, 31), prediction.get("next_start"));
        assertEquals(22L, prediction.get("days_until_next"));
        verify(periodRepository).insertProfile(8801L, DefaultFamily.FAMILY_ID, 1001L, 30, 6,
                LocalDate.of(2026, 7, 1), true, "08:45", "关注状态", 9001L, now);
        verify(reminderService).deleteBySource("PERIOD_TRACKER", 1001L);
        verify(reminderService).createFromSource("经期提醒", "关注状态", "PERIOD_TRACKER", 1001L,
                LocalDateTime.of(2026, 7, 31, 8, 45));
    }

    @Test
    void shouldCreateRecordAndUpdateExistingProfile() {
        CurrentUser currentUser = new CurrentUser(9002L, DefaultFamily.FAMILY_ID, 1002L, "dandan", "丹丹");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 12, 0);
        CreatePeriodRecordRequest request = new CreatePeriodRecordRequest();
        request.setStartOn("2026-07-09");
        request.setEndOn("2026-07-13");
        request.setNote("本次正常");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(idGenerator.nextId()).thenReturn(8811L);
        when(periodRepository.findProfile(DefaultFamily.FAMILY_ID, 1002L)).thenReturn(Optional.of(Map.of(
                "id", 7711L,
                "cycle_days", 28,
                "period_days", 5,
                "reminder_enabled", 1,
                "reminder_time", "09:00",
                "note", "旧备注")));

        Map<String, Object> result = periodService.createRecord(request);

        assertEquals(8811L, result.get("id"));
        verify(periodRepository).insertRecord(8811L, DefaultFamily.FAMILY_ID, 1002L, LocalDate.of(2026, 7, 9),
                LocalDate.of(2026, 7, 13), "本次正常", 9002L, now);
        verify(periodRepository).updateProfile(7711L, DefaultFamily.FAMILY_ID, 1002L, 28, 5,
                LocalDate.of(2026, 7, 9), true, "09:00", "旧备注", 9002L, now);
        verify(reminderService).createFromSource("经期提醒", "旧备注", "PERIOD_TRACKER", 1002L,
                LocalDateTime.of(2026, 8, 6, 9, 0));
    }

    @Test
    void shouldReturnDefaultProfileWhenNoProfileExists() {
        CurrentUser currentUser = new CurrentUser(9003L, DefaultFamily.FAMILY_ID, 1003L, "guest", "访客");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(periodRepository.findProfile(DefaultFamily.FAMILY_ID, 1003L)).thenReturn(Optional.empty());
        when(periodRepository.findRecords(DefaultFamily.FAMILY_ID, 1003L, 18)).thenReturn(List.of());

        Map<String, Object> result = periodService.getMine();
        Map<String, Object> profile = cast(result.get("profile"));

        assertEquals(28, profile.get("cycle_days"));
        assertTrue(Boolean.FALSE.equals(profile.get("has_profile")) || "false".equals(String.valueOf(profile.get("has_profile"))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }
}
