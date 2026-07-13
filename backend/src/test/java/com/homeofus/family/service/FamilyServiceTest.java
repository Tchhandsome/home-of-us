package com.homeofus.family.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeofus.auth.repository.AuthRepository;
import com.homeofus.auth.service.AuthService;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.family.dto.UpdateHomeViewModeRequest;
import com.homeofus.family.dto.UpdatePlantCheckInRequest;
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import com.homeofus.family.repository.FamilyRepository;
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
 * 家庭服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private FamilyService familyService;

    @BeforeEach
    void setUp() {
        familyService = new FamilyService(familyRepository, authRepository, authService, currentUserProvider,
                idGenerator, timeProvider, new ObjectMapper());
    }

    @Test
    void shouldIgnorePasswordWhenUpdatingAnotherMember() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 0);
        UpdateFamilyMemberRequest request = new UpdateFamilyMemberRequest();
        request.setDisplayName("丹丹");
        request.setUsername("dandan");
        request.setPassword("12345678");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(familyRepository.findMember(1002L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 1002L)));
        when(authRepository.findByUsername("dandan")).thenReturn(Optional.empty());
        when(authRepository.updateUserByMemberId(1002L, "dandan", "", "丹丹", 9001L, now)).thenReturn(1);

        familyService.updateMember(1002L, request);

        verify(authService, never()).hashPassword(any());
        verify(authRepository).updateUserByMemberId(1002L, "dandan", "", "丹丹", 9001L, now);
        verify(authRepository, never()).insertUser(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnHomeCardOrderInFamilyOverview() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(familyRepository.findFamily(DefaultFamily.FAMILY_ID)).thenReturn(Map.of("id", DefaultFamily.FAMILY_ID));
        when(familyRepository.findMembers(DefaultFamily.FAMILY_ID)).thenReturn(List.of(Map.of("id", 1001L)));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "homeCardOrder"))
                .thenReturn(Optional.of("[\"pets\",\"profile\"]"));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "homeViewMode"))
                .thenReturn(Optional.of("cards"));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInDates"))
                .thenReturn(Optional.of("[\"2026-07-09\"]"));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInEntries"))
                .thenReturn(Optional.of("[{\"plantId\":3001,\"checkInDate\":\"2026-07-10\"}]"));

        Map<String, Object> overview = familyService.getDefaultFamilyOverview();
        Object preferencesObject = overview.get("preferences");
        Map<?, ?> preferences = assertInstanceOf(Map.class, preferencesObject);
        Object cardOrderObject = preferences.get("homeCardOrder");
        List<?> cardOrder = assertInstanceOf(List.class, cardOrderObject);
        Object homeViewModeObject = preferences.get("homeViewMode");
        Object checkInEntriesObject = preferences.get("plantCheckInEntries");
        List<?> checkInEntries = assertInstanceOf(List.class, checkInEntriesObject);
        Map<?, ?> firstCheckInEntry = assertInstanceOf(Map.class, checkInEntries.get(0));

        assertEquals("pets", cardOrder.get(0));
        assertEquals("profile", cardOrder.get(1));
        assertEquals(15, cardOrder.size());
        assertEquals("todo", cardOrder.get(2));
        assertEquals("votes", cardOrder.get(11));
        assertEquals("cards", homeViewModeObject);
        assertEquals(List.of("2026-07-09", "2026-07-10"), preferences.get("plantCheckInDates"));
        assertEquals(3001L, ((Number) firstCheckInEntry.get("plantId")).longValue());
        assertEquals("2026-07-10", firstCheckInEntry.get("checkInDate"));
    }

    @Test
    void shouldDefaultHomeViewModeToCards() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(familyRepository.findFamily(DefaultFamily.FAMILY_ID)).thenReturn(Map.of("id", DefaultFamily.FAMILY_ID));
        when(familyRepository.findMembers(DefaultFamily.FAMILY_ID)).thenReturn(List.of(Map.of("id", 1001L)));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "homeCardOrder"))
                .thenReturn(Optional.empty());
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "homeViewMode"))
                .thenReturn(Optional.empty());
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInDates"))
                .thenReturn(Optional.empty());
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInEntries"))
                .thenReturn(Optional.empty());

        Map<String, Object> overview = familyService.getDefaultFamilyOverview();
        Object preferencesObject = overview.get("preferences");
        Map<?, ?> preferences = assertInstanceOf(Map.class, preferencesObject);

        assertEquals("cards", preferences.get("homeViewMode"));
        assertEquals(List.of(), preferences.get("plantCheckInDates"));
    }

    @Test
    void shouldPersistHomeViewModePreference() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 8, 30);
        UpdateHomeViewModeRequest request = new UpdateHomeViewModeRequest();
        request.setViewMode("cards");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(5001L);
        when(timeProvider.now()).thenReturn(now);

        Map<String, Object> result = familyService.updateHomeViewMode(request);

        assertEquals(1, result.get("updated"));
        assertEquals("cards", result.get("homeViewMode"));
        verify(familyRepository).saveMemberPreference(5001L, DefaultFamily.FAMILY_ID, 1001L, "homeViewMode", "cards",
                9001L, now);
    }

    @Test
    void shouldPersistPlantCheckInDate() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 8, 30);
        UpdatePlantCheckInRequest request = new UpdatePlantCheckInRequest();
        request.setCheckInDate("2026-07-10");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(5002L);
        when(timeProvider.now()).thenReturn(now);
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInDates"))
                .thenReturn(Optional.of("[\"2026-07-09\"]"));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInEntries"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = familyService.updatePlantCheckIn(request);

        assertEquals(1, result.get("updated"));
        assertEquals(List.of("2026-07-09", "2026-07-10"), result.get("plantCheckInDates"));
        verify(familyRepository).saveMemberPreference(5002L, DefaultFamily.FAMILY_ID, 1001L, "plantCheckInDates",
                "[\"2026-07-09\",\"2026-07-10\"]", 9001L, now);
    }

    @Test
    void shouldFallbackToTodayWhenPlantCheckInDateInvalid() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 8, 30);
        UpdatePlantCheckInRequest request = new UpdatePlantCheckInRequest();
        request.setCheckInDate("not-a-date");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(5003L);
        when(timeProvider.now()).thenReturn(now);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 7, 10));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInDates"))
                .thenReturn(Optional.empty());
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInEntries"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = familyService.updatePlantCheckIn(request);

        assertEquals(List.of("2026-07-10"), result.get("plantCheckInDates"));
    }

    @Test
    void shouldPersistPlantCheckInEntryForSpecificPlant() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 8, 30);
        UpdatePlantCheckInRequest request = new UpdatePlantCheckInRequest();
        request.setPlantId(3001L);
        request.setCheckInDate("2026-07-10");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(5004L);
        when(timeProvider.now()).thenReturn(now);
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInDates"))
                .thenReturn(Optional.of("[\"2026-07-09\"]"));
        when(familyRepository.findMemberPreferenceValue(DefaultFamily.FAMILY_ID, 1001L, "plantCheckInEntries"))
                .thenReturn(Optional.of("[{\"plantId\":3001,\"checkInDate\":\"2026-07-08\"}]"));

        Map<String, Object> result = familyService.updatePlantCheckIn(request);
        Object checkInEntriesObject = result.get("plantCheckInEntries");
        List<?> checkInEntries = assertInstanceOf(List.class, checkInEntriesObject);
        Map<?, ?> latestCheckInEntry = assertInstanceOf(Map.class, checkInEntries.get(1));

        assertEquals(1, result.get("updated"));
        assertEquals(List.of("2026-07-08", "2026-07-09", "2026-07-10"), result.get("plantCheckInDates"));
        assertEquals(3001L, ((Number) latestCheckInEntry.get("plantId")).longValue());
        assertEquals("2026-07-10", latestCheckInEntry.get("checkInDate"));
        verify(familyRepository).saveMemberPreference(5004L, DefaultFamily.FAMILY_ID, 1001L, "plantCheckInEntries",
                "[{\"plantId\":3001,\"checkInDate\":\"2026-07-08\"},{\"plantId\":3001,\"checkInDate\":\"2026-07-10\"}]",
                9001L, now);
    }
}
