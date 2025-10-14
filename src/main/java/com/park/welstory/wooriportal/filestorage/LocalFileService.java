package com.park.welstory.wooriportal.filestorage;

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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class LocalFileService {

    @Value("D:/WebStorage")
    private String rootPath;

    // jsTree 형식으로 파일 목록 반환
    public List<Map<String, Object>> listFilesForTree(String path) throws IOException {
        List<Map<String, Object>> treeData = new ArrayList<>();
        Path dirPath = getLocalPath(path);
        
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return treeData;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path filePath : stream) {
                String name = filePath.getFileName().toString();
                boolean isDir = Files.isDirectory(filePath);
                String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
                long size = isDir ? 0L : Files.size(filePath);
                LocalDateTime modified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(filePath).toInstant(), 
                    ZoneId.systemDefault()
                );

                Map<String, Object> node = new HashMap<>();
                node.put("id", fullPath);
                node.put("text", name);

                String type;

                if (isDir) {
                    type = "folder";
                } else {
                    String ext = "";
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex > 0) {
                        ext = name.substring(dotIndex + 1).toLowerCase();
                    }

                    switch(ext){
                        case "png": case "jpg": case "jpeg": case "gif": type = "image"; break;
                        case "pdf": type = "pdf"; break;
                        case "doc": case "docx": type = "doc"; break;
                        case "xls": case "xlsx": type = "xls"; break;
                        case "mp4": case "avi": type = "video"; break;
                        case "mp3": case "wav": type = "audio"; break;
                        default: type = "file";
                    }
                }

                node.put("type", type);


                node.put("children", isDir); // 폴더인 경우 children 속성 추가
                
                // 추가 데이터
                Map<String, Object> data = new HashMap<>();
                data.put("directory", isDir);
                data.put("size", size);
                data.put("lastModified", modified);
                node.put("data", data);

                treeData.add(node);
            }
        }

        return treeData;
    }

    // 파일 다운로드
    public void downloadFile(String path, HttpServletResponse response) throws IOException {
        Path filePath = getLocalPath(path);
        
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String fileName = filePath.getFileName().toString();
        response.setHeader("Content-Disposition", 
            "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(Files.size(filePath));

        try (InputStream is = Files.newInputStream(filePath);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }

    // 파일 업로드
    public void uploadFiles(MultipartFile[] files, String path) throws IOException {
        Path targetDir = getLocalPath(path);
        
        // 디렉토리가 없으면 생성
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileName == null) continue;
            
            // 상대 경로가 포함된 파일명 처리
            String[] parts = fileName.split("/");
            Path currentPath = targetDir;
            
            // 폴더 구조 생성
            for (int i = 0; i < parts.length - 1; i++) {
                currentPath = currentPath.resolve(parts[i]);
                if (!Files.exists(currentPath)) {
                    Files.createDirectories(currentPath);
                }
            }
            
            // 파일 저장
            Path filePath = currentPath.resolve(parts[parts.length - 1]);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // 파일/폴더 삭제
    public void deleteItem(String path, boolean isDirectory) throws IOException {
        Path itemPath = getLocalPath(path);
        
        if (!Files.exists(itemPath)) {
            throw new IOException("파일 또는 폴더를 찾을 수 없습니다: " + path);
        }

        if (isDirectory) {
            // 폴더 삭제 (재귀적)
            deleteDirectoryRecursively(itemPath);
        } else {
            // 파일 삭제
            Files.delete(itemPath);
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

    // 파일/폴더 이름 변경
    public void renameItem(String path, String newName, boolean isDirectory) throws IOException {
        Path oldPath = getLocalPath(path);
        Path parentDir = oldPath.getParent();
        Path newPath = parentDir.resolve(newName);
        
        if (!Files.exists(oldPath)) {
            throw new IOException("파일 또는 폴더를 찾을 수 없습니다: " + path);
        }
        
        if (Files.exists(newPath)) {
            throw new IOException("같은 이름의 파일 또는 폴더가 이미 존재합니다: " + newName);
        }
        
        Files.move(oldPath, newPath);
    }

    // 파일/폴더 이동
    public void moveItem(String fromPath, String toPath) throws IOException {
        Path sourcePath = getLocalPath(fromPath);
        Path targetPath = getLocalPath(toPath);
        
        if (!Files.exists(sourcePath)) {
            throw new IOException("소스 파일 또는 폴더를 찾을 수 없습니다: " + fromPath);
        }
        
        // 대상 디렉토리가 없으면 생성
        if (Files.isDirectory(targetPath)) {
            targetPath = targetPath.resolve(sourcePath.getFileName());
        } else {
            // 대상의 부모 디렉토리가 없으면 생성
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
        }
        
        Files.move(sourcePath, targetPath);
    }

    // 로컬 경로 변환
    private Path getLocalPath(String path) {
        if (path.equals("/") || path.isEmpty()) {
            return Paths.get(rootPath);
        }
        
        // 경로 정규화 및 보안 검사
        String normalizedPath = path.replaceAll("^/+", ""); // 앞의 슬래시 제거
        // Windows 경로 구분자를 Unix 스타일로 변환
        normalizedPath = normalizedPath.replace("/", File.separator);
        
        Path fullPath = Paths.get(rootPath, normalizedPath).normalize();
        Path rootPathObj = Paths.get(rootPath).normalize();
        
        // 루트 경로를 벗어나지 않는지 확인
        if (!fullPath.startsWith(rootPathObj)) {
            throw new SecurityException("접근이 허용되지 않은 경로입니다: " + path);
        }
        
        return fullPath;
    }

    // 여러 파일을 ZIP으로 다운로드
    public void downloadFilesAsZip(List<String> filePaths, HttpServletResponse response) throws IOException {
        if (filePaths == null || filePaths.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "다운로드할 파일이 없습니다.");
            return;
        }

        // ZIP 파일명 생성 (현재 시간 포함)
        String zipFileName = "files_" + System.currentTimeMillis() + ".zip";
        
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", 
            "attachment; filename=\"" + URLEncoder.encode(zipFileName, "UTF-8") + "\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            Map<String, Integer> entryCounters = new HashMap<>();
            
            for (String filePath : filePaths) {
                Path localPath = getLocalPath(filePath);
                
                if (!Files.exists(localPath)) {
                    continue; // 파일이나 폴더가 없는 경우 건너뛰기
                }

                if (Files.isDirectory(localPath)) {
                    // 폴더인 경우 재귀적으로 압축
                    addDirectoryToZip(zipOut, localPath, "", entryCounters);
                } else {
                    // 파일인 경우 직접 압축
                    addFileToZip(zipOut, localPath, "", entryCounters);
                }
            }
        }
    }

    // 폴더를 ZIP에 재귀적으로 추가
    private void addDirectoryToZip(ZipOutputStream zipOut, Path dirPath, String basePath, Map<String, Integer> entryCounters) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                String entryName = basePath + path.getFileName().toString();
                
                if (Files.isDirectory(path)) {
                    // 폴더인 경우 폴더 엔트리 추가 후 재귀 호출
                    entryName += "/";
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipOut.putNextEntry(zipEntry);
                    zipOut.closeEntry();
                    
                    // 하위 폴더 재귀 처리
                    addDirectoryToZip(zipOut, path, entryName, entryCounters);
                } else {
                    // 파일인 경우 파일 추가
                    addFileToZip(zipOut, path, basePath, entryCounters);
                }
            }
        }
    }

    // 파일을 ZIP에 추가
    private void addFileToZip(ZipOutputStream zipOut, Path filePath, String basePath, Map<String, Integer> entryCounters) throws IOException {
        String entryName = basePath + filePath.getFileName().toString();
        
        // 같은 이름의 파일이 있는 경우 번호 추가
        if (entryCounters.containsKey(entryName)) {
            int counter = entryCounters.get(entryName) + 1;
            entryCounters.put(entryName, counter);
            
            // 확장자 분리
            int dotIndex = entryName.lastIndexOf('.');
            if (dotIndex > 0) {
                String nameWithoutExt = entryName.substring(0, dotIndex);
                String extension = entryName.substring(dotIndex);
                entryName = nameWithoutExt + "_" + counter + extension;
            } else {
                entryName = entryName + "_" + counter;
            }
        } else {
            entryCounters.put(entryName, 1);
        }

        ZipEntry zipEntry = new ZipEntry(entryName);
        zipOut.putNextEntry(zipEntry);

        try (InputStream fileIn = Files.newInputStream(filePath)) {
            fileIn.transferTo(zipOut);
        }
        
        zipOut.closeEntry();
    }
}
