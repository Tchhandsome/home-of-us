package com.homeofus.album.service;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import com.homeofus.album.dto.UpdateMemoryEntryRequest;
import com.homeofus.album.repository.AlbumRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 时刻墙业务服务。
 *
 * @author tanchaohong
 */
@Service
public class AlbumService {

    private static final String MEMORY_SOURCE_TYPE = "MEMORY_EVENT";

    private final AlbumRepository albumRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public AlbumService(AlbumRepository albumRepository, ReminderService reminderService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.albumRepository = albumRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建时刻墙条目。
     *
     * @param request 创建请求
     * @return 新条目 ID
     */
    public Map<String, Object> createPhoto(CreateAlbumPhotoRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String entryType = resolveEntryType(request.getEntryType());
        validatePayload(entryType, request.getImageUrl());
        LocalDate takenOn = parseDate(request.getTakenOn());
        Integer reminderDaysBefore = normalizeReminderDays(request.getReminderDaysBefore());
        boolean reminderEnabled = Boolean.TRUE.equals(request.getReminderEnabled());
        LocalDateTime nextRemindAt = calculateNextRemindAt(entryType, takenOn, reminderEnabled, reminderDaysBefore);
        albumRepository.insertPhoto(id, DefaultFamily.FAMILY_ID, request, entryType, takenOn, reminderEnabled,
                reminderDaysBefore, nextRemindAt, currentUser.getUserId(), timeProvider.now());
        syncReminder(id, entryType, request.getTitle(), request.getDescription(), request.getWishText(), nextRemindAt);
        return Map.of("id", id);
    }

    /**
     * 查询时刻墙条目。
     *
     * @return 条目列表
     */
    public List<Map<String, Object>> findPhotos() {
        return albumRepository.findPhotos(DefaultFamily.FAMILY_ID);
    }

    /**
     * 更新时刻墙条目。
     *
     * @param id 条目 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> updatePhoto(Long id, UpdateMemoryEntryRequest request) {
        ensurePhotoExists(id);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String entryType = resolveEntryType(request.getEntryType());
        validatePayload(entryType, request.getImageUrl());
        LocalDate takenOn = parseDate(request.getTakenOn());
        Integer reminderDaysBefore = normalizeReminderDays(request.getReminderDaysBefore());
        boolean reminderEnabled = Boolean.TRUE.equals(request.getReminderEnabled());
        LocalDateTime nextRemindAt = calculateNextRemindAt(entryType, takenOn, reminderEnabled, reminderDaysBefore);
        int updated = albumRepository.updatePhoto(id, DefaultFamily.FAMILY_ID, request, entryType, takenOn,
                reminderEnabled, reminderDaysBefore, nextRemindAt, currentUser.getUserId(), timeProvider.now());
        syncReminder(id, entryType, request.getTitle(), request.getDescription(), request.getWishText(), nextRemindAt);
        return Map.of("updated", updated);
    }

    /**
     * 删除时刻墙条目。
     *
     * @param id 条目 ID
     * @return 删除结果
     */
    public Map<String, Object> deletePhoto(Long id) {
        ensurePhotoExists(id);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        reminderService.deleteBySource(MEMORY_SOURCE_TYPE, id);
        int updated = albumRepository.deletePhoto(id, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        if (updated == 0) {
            throw new BusinessException("ALBUM_PHOTO_NOT_FOUND", "时刻墙条目不存在或已删除");
        }
        return Map.of("updated", updated);
    }

    private void syncReminder(Long id, String entryType, String title, String description, String wishText,
            LocalDateTime nextRemindAt) {
        reminderService.deleteBySource(MEMORY_SOURCE_TYPE, id);
        if (Objects.isNull(nextRemindAt)) {
            return;
        }
        String reminderTitle = buildReminderTitle(entryType, title);
        String reminderDescription = StringUtils.defaultIfBlank(description, wishText);
        reminderService.createFromSource(reminderTitle, reminderDescription, MEMORY_SOURCE_TYPE, id, nextRemindAt);
    }

    private String buildReminderTitle(String entryType, String title) {
        if (StringUtils.equals(entryType, "BIRTHDAY")) {
            return title + "生日提醒";
        }
        return title + "纪念提醒";
    }

    private void validatePayload(String entryType, String imageUrl) {
        if (StringUtils.equals(entryType, "PHOTO") && StringUtils.isBlank(imageUrl)) {
            throw new BusinessException("ALBUM_IMAGE_REQUIRED", "照片条目需要上传图片");
        }
    }

    private void ensurePhotoExists(Long id) {
        albumRepository.findPhoto(id, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("ALBUM_PHOTO_NOT_FOUND", "时刻墙条目不存在或已删除"));
    }

    private String resolveEntryType(String entryType) {
        if (StringUtils.equalsIgnoreCase(entryType, "MOOD")) {
            return "MOOD";
        }
        if (StringUtils.equalsIgnoreCase(entryType, "ANNIVERSARY")) {
            return "ANNIVERSARY";
        }
        if (StringUtils.equalsIgnoreCase(entryType, "BIRTHDAY")) {
            return "BIRTHDAY";
        }
        return "PHOTO";
    }

    private Integer normalizeReminderDays(Integer reminderDaysBefore) {
        if (Objects.isNull(reminderDaysBefore) || reminderDaysBefore < 0) {
            return 0;
        }
        return reminderDaysBefore;
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return timeProvider.today();
        }
    }

    private LocalDateTime calculateNextRemindAt(String entryType, LocalDate takenOn, boolean reminderEnabled,
            Integer reminderDaysBefore) {
        if (!reminderEnabled || Objects.isNull(takenOn)) {
            return null;
        }
        if (!StringUtils.equals(entryType, "ANNIVERSARY") && !StringUtils.equals(entryType, "BIRTHDAY")) {
            return null;
        }
        LocalDateTime now = timeProvider.now();
        int year = timeProvider.today().getYear();
        LocalDate candidate = buildOccurrenceDate(takenOn, year);
        LocalDateTime remindAt = LocalDateTime.of(candidate.minusDays(reminderDaysBefore), LocalTime.of(9, 0));
        while (remindAt.isBefore(now)) {
            year = year + 1;
            candidate = buildOccurrenceDate(takenOn, year);
            remindAt = LocalDateTime.of(candidate.minusDays(reminderDaysBefore), LocalTime.of(9, 0));
        }
        return remindAt;
    }

    private LocalDate buildOccurrenceDate(LocalDate takenOn, int year) {
        YearMonth yearMonth = YearMonth.of(year, takenOn.getMonthValue());
        int safeDay = Math.min(takenOn.getDayOfMonth(), yearMonth.lengthOfMonth());
        return LocalDate.of(year, takenOn.getMonthValue(), safeDay);
    }
}
