package com.homeofus.album.controller;

import com.homeofus.album.dto.CreateAlbumPhotoRequest;
import com.homeofus.album.dto.UpdateMemoryEntryRequest;
import com.homeofus.album.service.AlbumService;
import com.homeofus.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 时刻墙接口。
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
     * 创建时刻墙条目。
     *
     * @param request 创建请求
     * @return 新条目 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createPhoto(@Valid @RequestBody CreateAlbumPhotoRequest request) {
        return ApiResponse.ok(albumService.createPhoto(request));
    }

    /**
     * 查询时刻墙条目列表。
     *
     * @return 条目列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findPhotos() {
        return ApiResponse.ok(albumService.findPhotos());
    }

    /**
     * 更新时刻墙条目。
     *
     * @param id 条目 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PatchMapping("/{id}")
    public ApiResponse<Map<String, Object>> updatePhoto(@PathVariable Long id,
            @Valid @RequestBody UpdateMemoryEntryRequest request) {
        return ApiResponse.ok(albumService.updatePhoto(id, request));
    }

    /**
     * 删除时刻墙条目。
     *
     * @param id 条目 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deletePhoto(@PathVariable Long id) {
        return ApiResponse.ok(albumService.deletePhoto(id));
    }
}
