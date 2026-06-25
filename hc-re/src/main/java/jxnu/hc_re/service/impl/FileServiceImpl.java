package jxnu.hc_re.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jxnu.hc_re.service.FileService;

@Service
public class FileServiceImpl implements FileService {

    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public File resolveFile(String filepath) {
        String relative = filepath.replace("\\", "/");
        if (relative.startsWith("/uploads/")) {
            relative = relative.substring(9);
        } else if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        Path base = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base)) {
            throw new RuntimeException("非法的文件路径");
        }
        File file = target.toFile();
        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + filepath);
        }
        return file;
    }

    @Override
    public String probeContentType(File file) throws IOException {
        String mime = Files.probeContentType(file.toPath());
        return mime != null ? mime : "application/octet-stream";
    }

    @Override
    public InputStream openStream(File file) throws IOException {
        return new FileInputStream(file);
    }
}
