package com.homeofus.attachment.controller;

import com.homeofus.attachment.service.AttachmentService;
import com.homeofus.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资料库接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 查询附件元数据。
     *
     * @return 附件列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findFiles() {
        return ApiResponse.ok(attachmentService.findFiles());
    }

    /**
     * 上传图片。
     *
     * @param file 图片文件
     * @param linkedType 关联类型
     * @param linkedId 关联 ID
     * @return 上传结果
     */
    @PostMapping("/images")
    public ApiResponse<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String linkedType, @RequestParam(required = false) Long linkedId) {
        return ApiResponse.ok(attachmentService.uploadImage(file, linkedType, linkedId));
    }
}
