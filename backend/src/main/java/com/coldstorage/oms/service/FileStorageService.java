package com.coldstorage.oms.service;

import com.coldstorage.oms.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!isAllowed(ext)) {
            throw new BusinessException("仅支持图片：jpg/jpeg/png/gif/webp");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("单张图片不超过 5MB");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(name));
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
        return "/uploads/" + name;
    }

    public Path resolve(String filename) {
        Path p = root.resolve(filename).normalize();
        if (!p.startsWith(root)) {
            throw new BusinessException("非法路径");
        }
        if (!Files.exists(p)) {
            throw new BusinessException("文件不存在");
        }
        return p;
    }

    private boolean isAllowed(String ext) {
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext)
                || ".gif".equals(ext) || ".webp".equals(ext);
    }
}
