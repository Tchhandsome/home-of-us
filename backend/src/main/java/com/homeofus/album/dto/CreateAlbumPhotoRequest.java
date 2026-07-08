package com.homeofus.album.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建相册照片请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateAlbumPhotoRequest {

    @NotBlank(message = "album.title.required")
    private String title;

    @NotBlank(message = "album.imageUrl.required")
    private String imageUrl;

    private String description;

    private String takenOn;
}
