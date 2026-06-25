package jxnu.hc_re.controller;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jxnu.hc_re.service.FileService;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    @Autowired
    private FileService fileService;

    /** 下载文件 */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam String filepath) throws IOException {
        File file = fileService.resolveFile(filepath);
        String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.probeContentType(file)))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .body(new InputStreamResource(fileService.openStream(file)));
    }

    /** 预览文件 */
    @GetMapping("/preview")
    public ResponseEntity<InputStreamResource> preview(@RequestParam String filepath) throws IOException {
        File file = fileService.resolveFile(filepath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileService.probeContentType(file)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(fileService.openStream(file)));
    }

    /** 上传文件 */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String savedName = UUID.randomUUID().toString() + ext;

        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dir = Paths.get(uploadPath, dateDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(savedName));

        Map<String, Object> result = new HashMap<>();
        result.put("filepath", "/uploads/" + dateDir + "/" + savedName);
        result.put("originalFilename", originalName);
        return result;
    }
}
