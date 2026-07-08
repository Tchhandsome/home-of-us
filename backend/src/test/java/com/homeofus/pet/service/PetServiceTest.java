package com.homeofus.pet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.pet.dto.UpdatePetRequest;
import com.homeofus.pet.repository.PetRepository;
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
}
