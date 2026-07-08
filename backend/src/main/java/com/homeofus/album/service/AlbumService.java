package com.homeofus.album.service;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import com.homeofus.album.repository.AlbumRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 共同相册业务服务。
 *
 * @author tanchaohong
 */
@Service
public class AlbumService {

    private final AlbumRepository albumRepository;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public AlbumService(AlbumRepository albumRepository, CurrentUserProvider currentUserProvider,
            IdGenerator idGenerator, TimeProvider timeProvider) {
        this.albumRepository = albumRepository;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建共同相册照片。
     *
     * @param request 创建请求
     * @return 新照片 ID
     */
    public Map<String, Object> createPhoto(CreateAlbumPhotoRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        albumRepository.insertPhoto(id, DefaultFamily.FAMILY_ID, request, parseDate(request.getTakenOn()),
                currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询共同相册照片。
     *
     * @return 照片列表
     */
    public List<Map<String, Object>> findPhotos() {
        return albumRepository.findPhotos(DefaultFamily.FAMILY_ID);
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
}
