package com.park.welstory.wooriportal.filemanager;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileManagerService {

    @Value("D:/WebStorage")
    private String rootPath;

    // 비밀번호 상수
    private static final String LOCK_PASSWORD = "2004";

    // 숨김 폴더 목록
    private static final String[] HIDDEN_FOLDERS = {
        "보안자료", "시험자료", "WebServer", "PC재원", "프로그램"
    };

    // 파일/폴더 목록 조회
    public List<Map<String, Object>> getFiles(String path, String password) throws IOException {
        List<Map<String, Object>> files = new ArrayList<>();
        Path targetPath = getLocalPath(path);
        boolean isUnlocked = verifyPassword(password);

        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            return files;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath)) {
            for (Path filePath : stream) {
                try {
                    String fileName = filePath.getFileName().toString();
                    
                    // 숨김 폴더 체크
                    if (Files.isDirectory(filePath) && isHiddenFolder(fileName) && !isUnlocked) {
                        continue; // 잠금 해제 안됐으면 숨김
                    }
                    
                    Map<String, Object> fileInfo = createFileInfo(filePath, path);
                    files.add(fileInfo);
                } catch (Exception e) {
                    continue;
                }
            }
        }
        
        return files;
    }

    // 폴더 트리 조회 (jsTree 형식)
    public List<Map<String, Object>> getTree(String path, String password) throws IOException {
        List<Map<String, Object>> treeData = new ArrayList<>();
        Path dirPath = getLocalPath(path);
        boolean isUnlocked = verifyPassword(password);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return treeData;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path filePath : stream) {
                if (Files.isDirectory(filePath)) {
                    String name = filePath.getFileName().toString();
                    
                    // 숨김 폴더 체크
                    if (isHiddenFolder(name) && !isUnlocked) {
                        continue; // 잠금 해제 안됐으면 숨김
                    }
                    
                    String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
                    if (!fullPath.startsWith("/")) {
                        fullPath = "/" + fullPath;
                    }

                    Map<String, Object> node = new HashMap<>();
                    node.put("id", fullPath);
                    node.put("text", name);
                    node.put("type", "default");
                    
                    // 하위 폴더 존재 여부 확인
                    boolean hasChildren = hasSubFolders(filePath, password);
                    node.put("children", hasChildren);

                    Map<String, Object> data = new HashMap<>();
                    data.put("directory", true);
                    node.put("data", data);

                    treeData.add(node);
                }
            }
        }

        return treeData;
    }

    // 하위 폴더 존재 여부 확인
    private boolean hasSubFolders(Path dir, String password) {
        boolean isUnlocked = verifyPassword(password);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String name = entry.getFileName().toString();
                    if (isHiddenFolder(name) && !isUnlocked) {
                        continue;
                    }
                    return true; // 하나라도 하위 폴더가 있으면 true
                }
            }
        } catch (IOException e) {
            // 무시
        }
        return false;
    }

    // 숨김 폴더인지 확인
    private boolean isHiddenFolder(String folderName) {
        return Arrays.asList(HIDDEN_FOLDERS).contains(folderName);
    }

    // 비밀번호 검증
    public boolean verifyPassword(String password) {
        return LOCK_PASSWORD.equals(password);
    }

    // 폴더 생성
    public void createFolder(String parentPath, String name) throws IOException {
        Path parent = getLocalPath(parentPath);
        Path newFolder = parent.resolve(name);
        
        if (Files.exists(newFolder)) {
            throw new IOException("Folder already exists: " + name);
        }
        
        Files.createDirectories(newFolder);
    }

    // 파일 생성
    public void createFile(String parentPath, String name) throws IOException {
        Path parent = getLocalPath(parentPath);
        Path newFile = parent.resolve(name);
        
        if (Files.exists(newFile)) {
            throw new IOException("File already exists: " + name);
        }
        
        Files.createFile(newFile);
    }

    // 이름 변경
    public void renameItem(String path, String newName) throws IOException {
        Path oldPath = getLocalPath(path);
        Path newPath = oldPath.getParent().resolve(newName);
        
        if (Files.exists(newPath)) {
            throw new IOException("File or folder already exists: " + newName);
        }
        
        Files.move(oldPath, newPath);
    }

    // 이동
    public void moveItem(String sourcePath, String targetPath) throws IOException {
        Path source = getLocalPath(sourcePath);
        Path target = getLocalPath(targetPath);
        
        if (!Files.exists(source)) {
            throw new IOException("Source not found: " + sourcePath);
        }
        
        if (!Files.isDirectory(target)) {
            throw new IOException("Target is not a directory: " + targetPath);
        }
        
        Path dest = target.resolve(source.getFileName());
        if (Files.exists(dest)) {
            throw new IOException("Destination already exists");
        }
        
        Files.move(source, dest);
    }

    // 삭제
    public void deleteItem(String path) throws IOException {
        Path itemPath = getLocalPath(path);
        
        if (!Files.exists(itemPath)) {
            throw new IOException("Item not found: " + path);
        }
        
        if (Files.isDirectory(itemPath)) {
            deleteDirectoryRecursively(itemPath);
        } else {
            Files.delete(itemPath);
        }
    }

    // 파일 업로드 (멀티 파일 지원)
    public void uploadFiles(MultipartFile[] files, String path) throws IOException {
        Path targetDir = getLocalPath(path);
        
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                continue;
            }
            
            Path filePath = targetDir.resolve(fileName);
            
            // 같은 이름의 파일이 있으면 번호 추가
            int counter = 1;
            while (Files.exists(filePath)) {
                String baseName = fileName;
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    baseName = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }
                filePath = targetDir.resolve(baseName + "_" + counter + extension);
                counter++;
            }
            
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // 파일 다운로드
    public void downloadFile(String path, boolean preview, HttpServletResponse response) throws IOException {
        Path filePath = getLocalPath(path);
        
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String fileName = filePath.getFileName().toString();
        
        if (preview) {
            // 미리보기 모드
            String ext = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                ext = fileName.substring(dotIndex + 1).toLowerCase();
            }
            
            String contentType = getContentType(ext);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        } else {
            // 다운로드 모드
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
            response.setContentType("application/octet-stream");
        }
        
        response.setContentLengthLong(Files.size(filePath));
        
        try (InputStream is = Files.newInputStream(filePath);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }

    private String getContentType(String ext) {
        switch (ext.toLowerCase()) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            case "html": return "text/html";
            case "css": return "text/css";
            case "js": return "application/javascript";
            default: return "application/octet-stream";
        }
    }

    // 파일 정보 생성
    private Map<String, Object> createFileInfo(Path path, String parentPath) throws IOException {
        Map<String, Object> info = new HashMap<>();
        String name = path.getFileName().toString();
        boolean isDir = Files.isDirectory(path);
        
        String relativePath = getRelativePath(path);
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        
        info.put("id", relativePath);
        info.put("name", name);
        info.put("type", isDir ? "folder" : "file");
        info.put("size", isDir ? 0 : Files.size(path));
        
        ZonedDateTime modified = ZonedDateTime.ofInstant(
                Files.getLastModifiedTime(path).toInstant(),
                ZoneId.systemDefault()
        );
        info.put("date", modified.toInstant().toEpochMilli());
        
        if (!isDir) {
            String ext = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                ext = name.substring(dotIndex + 1).toLowerCase();
            }
            info.put("ext", ext);
        }
        
        return info;
    }

    // 상대 경로 가져오기
    private String getRelativePath(Path path) {
        Path root = Paths.get(rootPath);
        try {
            String relative = root.relativize(path).toString().replace("\\", "/");
            return relative.isEmpty() ? "/" : relative;
        } catch (Exception e) {
            return "/";
        }
    }

    // 로컬 경로 변환
    private Path getLocalPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return Paths.get(rootPath);
        }
        
        String normalizedPath = path.replaceAll("^/+", "");
        normalizedPath = normalizedPath.replace("/", File.separator);
        
        Path fullPath = Paths.get(rootPath, normalizedPath).normalize();
        Path rootPathObj = Paths.get(rootPath).normalize();
        
        if (!fullPath.startsWith(rootPathObj)) {
            throw new SecurityException("Access denied: " + path);
        }
        
        return fullPath;
    }

    // 멀티 파일 다운로드 (ZIP)
    public void downloadFilesAsZip(List<String> paths, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"files_" + System.currentTimeMillis() + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (String path : paths) {
                Path filePath = getLocalPath(path);
                if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                    continue;
                }
                
                String entryName = filePath.getFileName().toString();
                zos.putNextEntry(new ZipEntry(entryName));
                
                try (InputStream is = Files.newInputStream(filePath)) {
                    is.transferTo(zos);
                }
                
                zos.closeEntry();
            }
        }
    }

    // 폴더 재귀적 삭제
    private void deleteDirectoryRecursively(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    deleteDirectoryRecursively(path);
                } else {
                    Files.delete(path);
                }
            }
        }
        Files.delete(dir);
    }

    // 디스크 사용량 정보 조회
    public Map<String, Object> getDiskUsage() {
        Map<String, Object> usage = new HashMap<>();
        File root = new File(rootPath);
        
        if (root.exists()) {
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            usage.put("total", totalSpace);
            usage.put("used", usedSpace);
            usage.put("free", freeSpace);
            
            // 사용률 계산 (소수점 1자리까지)
            double percent = (double) usedSpace / totalSpace * 100;
            usage.put("percent", Math.round(percent * 10) / 10.0);
        } else {
            usage.put("total", 0L);
            usage.put("used", 0L);
            usage.put("free", 0L);
            usage.put("percent", 0.0);
        }
        
        return usage;
    }
}
