package com.homeofus.attachment.service;

import com.homeofus.attachment.repository.AttachmentRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资料库业务服务。
 *
 * @author tanchaohong
 */
@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    @Value("${home-of-us.upload.root-dir:data/uploads}")
    private String uploadRootDir;

    public AttachmentService(AttachmentRepository attachmentRepository, CurrentUserProvider currentUserProvider,
            IdGenerator idGenerator, TimeProvider timeProvider) {
        this.attachmentRepository = attachmentRepository;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 查询附件元数据。
     *
     * @return 附件列表
     */
    public List<Map<String, Object>> findFiles() {
        return attachmentRepository.findFiles(DefaultFamily.FAMILY_ID);
    }

    /**
     * 上传图片。
     *
     * @param file 图片文件
     * @param linkedType 关联类型
     * @param linkedId 关联 ID
     * @return 上传结果
     */
    public Map<String, Object> uploadImage(MultipartFile file, String linkedType, Long linkedId) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new BusinessException("ATTACHMENT_FILE_REQUIRED", "attachment.file.required");
        }
        String contentType = StringUtils.defaultString(file.getContentType());
        if (!StringUtils.startsWithIgnoreCase(contentType, "image/")) {
            throw new BusinessException("ATTACHMENT_IMAGE_REQUIRED", "attachment.image.required");
        }
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String monthFolder = timeProvider.today().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path folder = Path.of(uploadRootDir, monthFolder);
        Path target = folder.resolve(storedName);
        try {
            Files.createDirectories(folder);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException("ATTACHMENT_UPLOAD_FAILED", "attachment.upload.failed");
        }
        String url = "/api/uploads/" + monthFolder + "/" + storedName;
        Long id = idGenerator.nextId();
        String fileName = StringUtils.defaultIfBlank(file.getOriginalFilename(), storedName);
        attachmentRepository.insert(id, DefaultFamily.FAMILY_ID, fileName, contentType, url, linkedType, linkedId,
                currentUser.getUserId(), timeProvider.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("url", url);
        result.put("fileName", fileName);
        result.put("contentType", contentType);
        result.put("size", file.getSize());
        return result;
    }

    private String resolveExtension(String originalFileName, String contentType) {
        String fileName = StringUtils.defaultString(originalFileName);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            if (StringUtils.isNotBlank(extension)) {
                return "." + extension;
            }
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
