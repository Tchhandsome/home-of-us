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

    private String imageUrl;

    private String entryType;

    private String description;

    private String wishText;

    private Boolean reminderEnabled;

    private Integer reminderDaysBefore;

    private String takenOn;
}
