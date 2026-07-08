package com.homeofus.album.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import com.homeofus.album.repository.AlbumRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
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
 * 时刻墙服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private AlbumService albumService;

    @BeforeEach
    void setUp() {
        albumService = new AlbumService(albumRepository, reminderService, currentUserProvider, idGenerator, timeProvider);
    }

    @Test
    void shouldCreateAnniversaryReminder() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 8, 0);
        CreateAlbumPhotoRequest request = new CreateAlbumPhotoRequest();
        request.setTitle("结婚纪念日");
        request.setEntryType("ANNIVERSARY");
        request.setDescription("订花");
        request.setWishText("一起吃大餐");
        request.setTakenOn("2024-07-20");
        request.setReminderEnabled(true);
        request.setReminderDaysBefore(3);

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(9101L);
        when(timeProvider.now()).thenReturn(now);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 7, 9));

        Map<String, Object> result = albumService.createPhoto(request);

        assertEquals(9101L, result.get("id"));
        verify(albumRepository).insertPhoto(9101L, DefaultFamily.FAMILY_ID, request, "ANNIVERSARY",
                LocalDate.of(2024, 7, 20), true, 3, LocalDateTime.of(2026, 7, 17, 9, 0), 9001L, now);
        verify(reminderService).createFromSource("结婚纪念日纪念提醒", "订花", "MEMORY_EVENT", 9101L,
                LocalDateTime.of(2026, 7, 17, 9, 0));
    }

    @Test
    void shouldAllowMoodEntryWithoutImage() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 8, 30);
        CreateAlbumPhotoRequest request = new CreateAlbumPhotoRequest();
        request.setTitle("今天心情不错");
        request.setEntryType("MOOD");
        request.setDescription("下班一起吃饭");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(idGenerator.nextId()).thenReturn(9102L);
        when(timeProvider.now()).thenReturn(now);

        albumService.createPhoto(request);

        verify(albumRepository).insertPhoto(9102L, DefaultFamily.FAMILY_ID, request, "MOOD", null, false, 0, null,
                9001L, now);
        verify(reminderService).deleteBySource("MEMORY_EVENT", 9102L);
        verify(reminderService, never()).createFromSource("今天心情不错纪念提醒", "下班一起吃饭", "MEMORY_EVENT", 9102L, now);
    }

    @Test
    void shouldDeleteEntryAndReminderTogether() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 9, 0);

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(albumRepository.findPhoto(9103L, DefaultFamily.FAMILY_ID)).thenReturn(Optional.of(Map.of("id", 9103L)));
        when(albumRepository.deletePhoto(9103L, DefaultFamily.FAMILY_ID, 9001L, now)).thenReturn(1);

        Map<String, Object> result = albumService.deletePhoto(9103L);

        assertEquals(1, result.get("updated"));
        verify(reminderService).deleteBySource("MEMORY_EVENT", 9103L);
        verify(albumRepository).deletePhoto(9103L, DefaultFamily.FAMILY_ID, 9001L, now);
    }
}
