package com.homeofus.album.controller;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import com.homeofus.album.service.AlbumService;
import com.homeofus.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共同相册接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/album/photos")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    /**
     * 创建照片记录。
     *
     * @param request 创建请求
     * @return 新照片 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createPhoto(@Valid @RequestBody CreateAlbumPhotoRequest request) {
        return ApiResponse.ok(albumService.createPhoto(request));
    }

    /**
     * 查询照片列表。
     *
     * @return 照片列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findPhotos() {
        return ApiResponse.ok(albumService.findPhotos());
    }
}
