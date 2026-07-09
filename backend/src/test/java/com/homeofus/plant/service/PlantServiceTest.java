package com.homeofus.plant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.plant.dto.CreatePlantCareRecordRequest;
import com.homeofus.plant.dto.CreatePlantRequest;
import com.homeofus.plant.repository.PlantRepository;
import com.homeofus.reminder.service.ReminderService;
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
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private PlantService plantService;

    @BeforeEach
    void setUp() {
        plantService = new PlantService(plantRepository, reminderService, currentUserProvider, idGenerator, timeProvider);
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
    void shouldCreateReminderFromCareTextMonthDay() {
        CurrentUser currentUser = new CurrentUser(9002L, DefaultFamily.FAMILY_ID, 1002L, "dandan", "丹丹");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 0);
        CreatePlantCareRecordRequest request = new CreatePlantCareRecordRequest();
        request.setCareType("WATER");
        request.setDetail("今天已经浇水，7.18再次浇水");
        request.setRawText("今天已经浇水，7.18再次浇水");
        request.setCareDate("2026-07-09");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(7401L);
        when(timeProvider.now()).thenReturn(now);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 7, 9));
        when(plantRepository.findPlant(7101L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 7101L)));
        when(plantRepository.findPlantName(7101L)).thenReturn("多肉");

        Map<String, Object> result = plantService.createCareRecord(7101L, request);

        assertEquals(7401L, result.get("id"));
        verify(plantRepository).insertCareRecord(7401L, DefaultFamily.FAMILY_ID, 7101L, request,
                LocalDate.of(2026, 7, 9), LocalDateTime.of(2026, 7, 18, 9, 0), 9002L, now);
        verify(reminderService).createFromSource("多肉下次养护", "今天已经浇水，7.18再次浇水", "PLANT_CARE", 7401L,
                LocalDateTime.of(2026, 7, 18, 9, 0));
    }
}
