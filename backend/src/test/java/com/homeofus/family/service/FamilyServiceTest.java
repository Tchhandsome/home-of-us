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
import com.homeofus.family.dto.UpdateFamilyMemberRequest;
import com.homeofus.family.repository.FamilyRepository;
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

        Map<String, Object> overview = familyService.getDefaultFamilyOverview();
        Object preferencesObject = overview.get("preferences");
        Map<?, ?> preferences = assertInstanceOf(Map.class, preferencesObject);
        Object cardOrderObject = preferences.get("homeCardOrder");
        List<?> cardOrder = assertInstanceOf(List.class, cardOrderObject);

        assertEquals("pets", cardOrder.get(0));
        assertEquals("profile", cardOrder.get(1));
        assertEquals(13, cardOrder.size());
        assertEquals("todo", cardOrder.get(2));
    }
}
