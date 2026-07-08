package com.homeofus.vote.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.vote.dto.CreateFamilyVoteRequest;
import com.homeofus.vote.repository.FamilyVoteRepository;
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
 * 家庭投票服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class FamilyVoteServiceTest {

    @Mock
    private FamilyVoteRepository familyVoteRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private FamilyVoteService familyVoteService;

    @BeforeEach
    void setUp() {
        familyVoteService = new FamilyVoteService(familyVoteRepository, currentUserProvider, idGenerator, timeProvider);
    }

    @Test
    void shouldCreateDefaultEatVoteOptions() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 13, 0);
        CreateFamilyVoteRequest request = new CreateFamilyVoteRequest();
        request.setTitle("今晚吃什么");
        request.setVoteCategory("EAT");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(idGenerator.nextId()).thenReturn(9301L, 9302L, 9303L, 9304L, 9305L, 9306L);

        Map<String, Object> result = familyVoteService.create(request);

        assertEquals(9301L, result.get("id"));
        verify(familyVoteRepository).insertVote(9301L, DefaultFamily.FAMILY_ID, request, "EAT", 9001L, now);
        verify(familyVoteRepository, times(5)).insertOption(org.mockito.ArgumentMatchers.anyLong(), eq(9301L),
                eq(DefaultFamily.FAMILY_ID), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                eq(9001L), eq(now));
    }

    @Test
    void shouldDecideByHighestVoteOption() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 13, 30);

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(familyVoteRepository.findVote(DefaultFamily.FAMILY_ID, 9308L))
                .thenReturn(Optional.of(Map.of("id", 9308L, "status", "OPEN")),
                        Optional.of(Map.of("id", 9308L, "status", "DECIDED", "decided_option_id", 2L)));
        when(familyVoteRepository.findOptions(DefaultFamily.FAMILY_ID, 9308L))
                .thenReturn(List.of(
                        Map.of("id", 1L, "option_text", "火锅", "sort_order", 1),
                        Map.of("id", 2L, "option_text", "串串", "sort_order", 2),
                        Map.of("id", 3L, "option_text", "烧烤", "sort_order", 3)));
        when(familyVoteRepository.findChoices(DefaultFamily.FAMILY_ID, 9308L))
                .thenReturn(List.of(
                        Map.of("id", 1L, "option_id", 2L, "member_id", 1001L),
                        Map.of("id", 2L, "option_id", 2L, "member_id", 1002L),
                        Map.of("id", 3L, "option_id", 1L, "member_id", 1003L)));
        when(familyVoteRepository.decide(DefaultFamily.FAMILY_ID, 9308L, 2L, 9001L, now)).thenReturn(1);

        Map<String, Object> result = familyVoteService.decide(9308L);

        assertEquals(2L, result.get("decidedOptionId"));
        verify(familyVoteRepository).decide(DefaultFamily.FAMILY_ID, 9308L, 2L, 9001L, now);
    }
}
