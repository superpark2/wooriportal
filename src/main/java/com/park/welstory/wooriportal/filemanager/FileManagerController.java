package com.park.welstory.wooriportal.filemanager;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin(
        origins = "http://localhost:8080",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
                RequestMethod.PUT, RequestMethod.OPTIONS}
)
@Controller
@RequiredArgsConstructor
@RequestMapping("/filestorage")
public class FileManagerController {

    private final FileManagerService fileManagerService;

    @GetMapping("/list")
    public String showFileManagerPage() {

        return "common/filemanager/list";
    }

    @GetMapping
    public String showFileManagerPageView() {
        return "common/filemanager/view";
    }

    // 파일/폴더 목록 조회
    @GetMapping("/api/files")
    @ResponseBody
    public List<Map<String, Object>> getFiles(@RequestParam(required = false) String path,
                                               @RequestParam(required = false) String password) throws IOException {
        return fileManagerService.getFiles(path, password);
    }

    // 폴더 트리 조회 (jsTree 형식)
    @GetMapping("/api/files/tree")
    @ResponseBody
    public List<Map<String, Object>> getTree(@RequestParam(required = false) String path,
                                            @RequestParam(required = false) String password) throws IOException {
        return fileManagerService.getTree(path, password);
    }

    // 비밀번호 검증
    @PostMapping("/api/lock/verify")
    @ResponseBody
    public ResponseEntity<?> verifyPassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        boolean success = fileManagerService.verifyPassword(password);
        return ResponseEntity.ok().body(Map.of("success", success));
    }

    // 파일/폴더 생성
    @PostMapping("/api/files")
    @ResponseBody
    public ResponseEntity<?> createItem(@RequestBody Map<String, String> request) throws IOException {
        String path = request.get("path");
        String name = request.get("name");
        String type = request.get("type"); // "file" or "folder"
        
        if ("folder".equals(type)) {
            fileManagerService.createFolder(path, name);
        } else {
            fileManagerService.createFile(path, name);
        }
        
        return ResponseEntity.ok().build();
    }

    // 파일/폴더 수정 (이름 변경, 이동)
    @PutMapping("/api/files")
    @ResponseBody
    public ResponseEntity<?> updateItem(@RequestBody Map<String, Object> request) throws IOException {
        String action = (String) request.get("action");
        
        if ("rename".equals(action)) {
            String path = (String) request.get("path");
            String newName = (String) request.get("newName");
            fileManagerService.renameItem(path, newName);
        } else if ("move".equals(action)) {
            String source = (String) request.get("source");
            String target = (String) request.get("target");
            fileManagerService.moveItem(source, target);
        }
        
        return ResponseEntity.ok().build();
    }

    // 파일/폴더 삭제
    @DeleteMapping("/api/files")
    @ResponseBody
    public ResponseEntity<?> deleteItem(@RequestParam String path) throws IOException {
        fileManagerService.deleteItem(path);
        return ResponseEntity.ok().build();
    }

    // 파일 업로드 (멀티 파일 지원)
    @PostMapping("/api/files/upload")
    @ResponseBody
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile[] files,
                                       @RequestParam(required = false) String path) throws IOException {
        fileManagerService.uploadFiles(files, path);
        return ResponseEntity.ok().build();
    }

    // 파일 다운로드
    @GetMapping("/api/files/download")
    public void downloadFile(@RequestParam String path, 
                            @RequestParam(required = false) Boolean preview,
                            HttpServletResponse response) throws IOException {
        fileManagerService.downloadFile(path, preview != null && preview, response);
    }

    // 멀티 파일 다운로드 (ZIP)
    @PostMapping("/api/files/download-multiple")
    public void downloadMultipleFiles(@RequestBody Map<String, Object> request,
                                     HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) request.get("paths");
        fileManagerService.downloadFilesAsZip(paths, response);
    }

    // 디스크 사용량 조회
    @GetMapping("/api/disk/usage")
    @ResponseBody
    public Map<String, Object> getDiskUsage() {
        return fileManagerService.getDiskUsage();
    }
}
