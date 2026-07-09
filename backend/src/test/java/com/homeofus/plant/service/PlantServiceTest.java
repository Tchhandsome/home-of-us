package com.homeofus.plant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import com.homeofus.plant.repository.PlantRepository;
import com.homeofus.reminder.repository.ReminderRepository;
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
 * 花卉服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private PlantService plantService;

    @BeforeEach
    void setUp() {
        plantService = new PlantService(plantRepository, reminderRepository, reminderService, currentUserProvider,
                idGenerator, timeProvider);
    }

    @Test
    void shouldUpdatePlantProfileWithCoverUrl() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 13, 0);
        CreatePlantRequest request = new CreatePlantRequest();
        request.setName("月季");
        request.setFlowerColor("粉色");
        request.setLocation("阳台");
        request.setCarePreference("通风");
        request.setCoverUrl("https://example.com/rose.jpg");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(plantRepository.findPlant(7301L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 7301L)));
        when(plantRepository.updatePlant(7301L, DefaultFamily.FAMILY_ID, request, "GROWING", null, 9001L, now))
                .thenReturn(1);

        Map<String, Object> result = plantService.updatePlant(7301L, request);

        assertEquals(1, result.get("updated"));
        verify(plantRepository).updatePlant(7301L, DefaultFamily.FAMILY_ID, request, "GROWING", null, 9001L, now);
    }

    @Test
    void shouldCreateReminderFromManualNextCareAt() {
        CurrentUser currentUser = new CurrentUser(9002L, DefaultFamily.FAMILY_ID, 1002L, "dandan", "丹丹");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 0);
        CreatePlantCareRecordRequest request = new CreatePlantCareRecordRequest();
        request.setCareType("WATER");
        request.setDetail("7.6日已浇水，7.18日再次浇水");
        request.setRawText("7.6日已浇水，7.18日再次浇水");
        request.setCareDate("2026-07-09");
        request.setNextCareAt("2026-07-18T09:00");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(7401L);
        when(timeProvider.now()).thenReturn(now);
        when(plantRepository.findPlant(7101L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 7101L)));
        when(plantRepository.findPlantName(7101L)).thenReturn("多肉");

        Map<String, Object> result = plantService.createCareRecord(7101L, request);

        assertEquals(7401L, result.get("id"));
        verify(plantRepository).insertCareRecord(7401L, DefaultFamily.FAMILY_ID, 7101L, request,
                LocalDate.of(2026, 7, 9), LocalDateTime.of(2026, 7, 18, 9, 0), 9002L, now);
        verify(reminderService).createFromSource("多肉下次养护", "7.6日已浇水，7.18日再次浇水", "PLANT_CARE", 7401L,
                LocalDateTime.of(2026, 7, 18, 9, 0));
    }

    @Test
    void shouldNotParseNextCareAtFromDetailWhenManualFieldIsBlank() {
        CurrentUser currentUser = new CurrentUser(9003L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 10, 0);
        CreatePlantCareRecordRequest request = new CreatePlantCareRecordRequest();
        request.setCareType("WATER");
        request.setDetail("7.6日已经浇水，7.18日再次浇水");
        request.setRawText("7.6日已经浇水，7.18日再次浇水");
        request.setCareDate("2026-07-09");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(7402L);
        when(timeProvider.now()).thenReturn(now);
        when(plantRepository.findPlant(7102L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 7102L)));

        Map<String, Object> result = plantService.createCareRecord(7102L, request);

        assertEquals(7402L, result.get("id"));
        verify(plantRepository).insertCareRecord(7402L, DefaultFamily.FAMILY_ID, 7102L, request,
                LocalDate.of(2026, 7, 9), null, 9003L, now);
        verifyNoInteractions(reminderService);
    }

    @Test
    void shouldRepairHistoricalCareScheduleWhenLegacyYearIsWrong() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 11, 0);
        when(timeProvider.now()).thenReturn(now);
        when(plantRepository.findCareRecordsForRepair(DefaultFamily.FAMILY_ID)).thenReturn(List.of(Map.of(
                "id", 8101L,
                "family_id", DefaultFamily.FAMILY_ID,
                "plant_id", 7101L,
                "care_date", LocalDate.of(2026, 7, 9),
                "detail", "7.6日已浇水，7.18日再次浇水，用通用肥料",
                "raw_text", "7.6日已浇水，7.18日再次浇水，用通用肥料",
                "next_care_at", LocalDateTime.of(2027, 7, 6, 9, 0))));

        int repairedCount = plantService.repairHistoricalCareSchedules();

        assertEquals(1, repairedCount);
        verify(plantRepository).updateCareRecordNextCareAt(8101L, DefaultFamily.FAMILY_ID,
                LocalDateTime.of(2026, 7, 18, 9, 0), null, now);
        verify(reminderRepository).updateDueAtBySource(DefaultFamily.FAMILY_ID, "PLANT_CARE", 8101L,
                LocalDateTime.of(2027, 7, 6, 9, 0), LocalDateTime.of(2026, 7, 18, 9, 0), null, now);
    }

    @Test
    void shouldRepairHistoricalCareScheduleWhenStoredDateFallsOnCareDay() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 11, 30);
        when(timeProvider.now()).thenReturn(now);
        when(plantRepository.findCareRecordsForRepair(DefaultFamily.FAMILY_ID)).thenReturn(List.of(Map.of(
                "id", 8102L,
                "family_id", DefaultFamily.FAMILY_ID,
                "plant_id", 7101L,
                "care_date", LocalDate.of(2026, 7, 8),
                "detail", "7.3风雨兰已用通融型肥料 7.10号看叶子多不多，多的话用花卉型",
                "raw_text", "7.3风雨兰已用通融型肥料 7.10号看叶子多不多，多的话用花卉型",
                "next_care_at", LocalDateTime.of(2026, 7, 8, 17, 20))));

        int repairedCount = plantService.repairHistoricalCareSchedules();

        assertEquals(1, repairedCount);
        verify(plantRepository).updateCareRecordNextCareAt(8102L, DefaultFamily.FAMILY_ID,
                LocalDateTime.of(2026, 7, 10, 9, 0), null, now);
        verify(reminderRepository).updateDueAtBySource(DefaultFamily.FAMILY_ID, "PLANT_CARE", 8102L,
                LocalDateTime.of(2026, 7, 8, 17, 20), LocalDateTime.of(2026, 7, 10, 9, 0), null, now);
    }
}
