package com.homeofus.attachment.service;

import com.homeofus.attachment.repository.AttachmentRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
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

    private static final int MAX_IMAGE_EDGE = 1600;

    private static final float JPEG_QUALITY = 0.82f;

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
        ProcessedImage processedImage = processImage(file, contentType);
        String extension = Objects.nonNull(processedImage) ? processedImage.getExtension()
                : resolveExtension(file.getOriginalFilename(), contentType);
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path folder = Path.of(uploadRootDir, monthFolder);
        Path target = folder.resolve(storedName);
        try {
            Files.createDirectories(folder);
            if (Objects.nonNull(processedImage)) {
                Files.write(target, processedImage.getBytes());
                contentType = processedImage.getContentType();
            } else {
                file.transferTo(target);
            }
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
        result.put("size", Objects.nonNull(processedImage) ? processedImage.getBytes().length : file.getSize());
        return result;
    }

    private ProcessedImage processImage(MultipartFile file, String contentType) {
        try {
            byte[] rawBytes = file.getBytes();
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (Objects.isNull(source)) {
                return null;
            }
            boolean keepPng = StringUtils.equalsIgnoreCase(contentType, "image/png")
                    || source.getColorModel().hasAlpha();
            int width = source.getWidth();
            int height = source.getHeight();
            double ratio = Math.min(1D, (double) MAX_IMAGE_EDGE / Math.max(width, height));
            int targetWidth = Math.max(1, (int) Math.round(width * ratio));
            int targetHeight = Math.max(1, (int) Math.round(height * ratio));
            int imageType = keepPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, imageType);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!keepPng) {
                graphics.setBackground(java.awt.Color.WHITE);
                graphics.clearRect(0, 0, targetWidth, targetHeight);
            }
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            graphics.dispose();
            return keepPng ? writePng(scaled) : writeJpeg(scaled);
        } catch (IOException exception) {
            return null;
        }
    }

    private ProcessedImage writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new ProcessedImage(outputStream.toByteArray(), ".png", "image/png");
    }

    private ProcessedImage writeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").hasNext()
                ? ImageIO.getImageWritersByFormatName("jpg").next()
                : null;
        if (Objects.isNull(writer)) {
            return null;
        }
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return new ProcessedImage(outputStream.toByteArray(), ".jpg", "image/jpeg");
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

    /**
     * 处理后的图片结果。
     *
     * @author tanchaohong
     */
    private static final class ProcessedImage {

        private final byte[] bytes;

        private final String extension;

        private final String contentType;

        private ProcessedImage(byte[] bytes, String extension, String contentType) {
            this.bytes = bytes;
            this.extension = extension;
            this.contentType = contentType;
        }

        private byte[] getBytes() {
            return bytes;
        }

        private String getExtension() {
            return extension;
        }

        private String getContentType() {
            return contentType;
        }
    }
}
