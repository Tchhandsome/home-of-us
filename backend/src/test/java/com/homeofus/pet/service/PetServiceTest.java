package com.homeofus.pet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.pet.dto.CreatePetCareRecordRequest;
import com.homeofus.pet.dto.CreatePetWeightRecordRequest;
import com.homeofus.pet.dto.UpdatePetRequest;
import com.homeofus.pet.repository.PetRepository;
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
 * 宠物服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, reminderService, currentUserProvider, idGenerator, timeProvider);
    }

    @Test
    void shouldUpdatePetProfile() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 30);
        UpdatePetRequest request = new UpdatePetRequest();
        request.setName("煤球");
        request.setSpecies("CAT");
        request.setBreed("英短");
        request.setGender("MALE");
        request.setBirthday("2024-05-01");
        request.setAvatarUrl("https://example.com/pet.png");
        request.setNote("爱晒太阳");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(petRepository.findPet(3001L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 3001L)));
        when(petRepository.updatePet(DefaultFamily.FAMILY_ID, 3001L, request, "CAT", LocalDate.of(2024, 5, 1),
                9001L, now)).thenReturn(1);

        Map<String, Object> result = petService.updatePet(3001L, request);

        assertEquals(1, result.get("updated"));
        verify(petRepository).updatePet(DefaultFamily.FAMILY_ID, 3001L, request, "CAT", LocalDate.of(2024, 5, 1),
                9001L, now);
    }

    @Test
    void shouldCreatePetCareReminder() {
        CurrentUser currentUser = new CurrentUser(9002L, DefaultFamily.FAMILY_ID, 1002L, "dandan", "丹丹");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 18, 20);
        CreatePetCareRecordRequest request = new CreatePetCareRecordRequest();
        request.setCareType("BATH");
        request.setRecordedAt("2026-07-09T18:20");
        request.setDescription("今晚洗澡，吹干完成");
        request.setNextDueAt("2026-07-19T20:00");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(4101L);
        when(timeProvider.now()).thenReturn(now);
        when(petRepository.findPet(3201L, DefaultFamily.FAMILY_ID))
                .thenReturn(Optional.of(Map.of("id", 3201L, "name", "六六")));

        Map<String, Object> result = petService.createCareRecord(3201L, request);

        assertEquals(4101L, result.get("id"));
        verify(petRepository).insertCareRecord(4101L, DefaultFamily.FAMILY_ID, 3201L, request, "BATH",
                LocalDateTime.of(2026, 7, 9, 18, 20), LocalDateTime.of(2026, 7, 19, 20, 0), 9002L, now);
        verify(reminderService).createFromSource("六六下次洗澡", "今晚洗澡，吹干完成", "PET_CARE", 4101L,
                LocalDateTime.of(2026, 7, 19, 20, 0));
    }

    @Test
    void shouldCreatePetWeightRecord() {
        CurrentUser currentUser = new CurrentUser(9003L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 19, 0);
        CreatePetWeightRecordRequest request = new CreatePetWeightRecordRequest();
        request.setWeightKg(new BigDecimal("4.68"));
        request.setRecordedOn("2026-07-09");
        request.setNote("状态不错");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(4102L);
        when(timeProvider.now()).thenReturn(now);
        when(petRepository.findPet(3202L, DefaultFamily.FAMILY_ID))
                .thenReturn(Optional.of(Map.of("id", 3202L, "name", "六六")));

        Map<String, Object> result = petService.createWeightRecord(3202L, request);

        assertEquals(4102L, result.get("id"));
        verify(petRepository).insertWeightRecord(4102L, DefaultFamily.FAMILY_ID, 3202L, request,
                LocalDate.of(2026, 7, 9), 9003L, now);
    }
}
