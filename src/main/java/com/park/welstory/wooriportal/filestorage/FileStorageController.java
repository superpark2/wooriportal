package com.park.welstory.wooriportal.filestorage;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/filestorage")
public class FileStorageController {

    private final LocalFileService localFileService;
    private String adminPassword = "2004";

    @GetMapping("/list")
    public String showFileStoragePage() {
        return "common/filestorage/list";
    }

    @GetMapping(value = "/filelist")
    @ResponseBody
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "/") String path ) throws IOException {
        return localFileService.listFilesForTree(path);
    }

    // 파일 다운로드
    @GetMapping("/download")
    public void download(@RequestParam String path, HttpServletResponse response) throws IOException {
        localFileService.downloadFile(path, response);
    }

    // 파일 업로드
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> upload(@RequestParam("files") MultipartFile[] files, @RequestParam String path) throws IOException {
        localFileService.uploadFiles(files, path);
        return ResponseEntity.ok().build();
    }

    // 파일/폴더 삭제
    @DeleteMapping("/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@RequestBody Map<String, Object> payload) {
        String path = (String) payload.get("path");
        Object dirObj = payload.get("directory");
        boolean directory = false;
        if (dirObj instanceof Boolean) {
            directory = (Boolean) dirObj;
        } else if (dirObj != null) {
            directory = Boolean.parseBoolean(String.valueOf(dirObj));
        }
        String password = (String) payload.get("password");

        if (password == null || !adminPassword.equals(password)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("비밀번호 불일치");
        }

        try {
            localFileService.deleteItem(path, directory);
            return ResponseEntity.ok("삭제 성공");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 실패: " + e.getMessage());
        }
    }

    @PostMapping("/create-folder")
    @ResponseBody
    public ResponseEntity<String> createFolder(@RequestBody Map<String, String> body) {
        String path = body.get("path"); // 현재 경로
        String name = body.get("name"); // 새 폴더 이름

        // jstree에서 #은 루트를 의미
        if (path.equals("#")) {
            path = "";
        }

        Path newFolderPath = Paths.get("C:/Users/Administrator/Desktop/wooriFTP", path, name);

        try {
            if (!Files.exists(newFolderPath)) {
                Files.createDirectories(newFolderPath);
                return ResponseEntity.ok("폴더 생성 완료");
            } else {
                return ResponseEntity.status(400).body("폴더가 이미 존재합니다");
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("폴더 생성 실패: " + e.getMessage());
        }
    }

    // 파일/폴더 이동
    @PostMapping("/move")
    @ResponseBody
    public ResponseEntity<?> move(@RequestBody Map<String, String> req) throws IOException {
        String from = req.get("from");
        String to = req.get("to");
        localFileService.moveItem(from, to);
        return ResponseEntity.ok().build();
    }

    // 이름변경
    @PostMapping("/rename")
    @ResponseBody
    public ResponseEntity<?> rename(@RequestBody Map<String, Object> req) throws IOException {
        String path = (String) req.get("path");
        String newName = (String) req.get("newName");
        boolean directory = (boolean) req.get("directory");
        localFileService.renameItem(path, newName, directory);
        return ResponseEntity.ok().build();
    }

    // 여러 파일 ZIP 다운로드
    @PostMapping("/bulk-download")
    public void bulkDownload(@RequestBody Map<String, Object> req, HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> filePaths = (List<String>) req.get("filePaths");
        localFileService.downloadFilesAsZip(filePaths, response);
    }


    @PostMapping("/secret-access")
    @ResponseBody
    public ResponseEntity<String> secretAccess(@RequestBody Map<String,String> body, HttpSession session) {

        String screatpassword = "2004";

        String password = body.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호 필요");
        }

        if (screatpassword.equals(password)) {
            session.setAttribute("showHidden", Boolean.TRUE);
            return ResponseEntity.ok("unlocked");
        } else {
            return ResponseEntity.status(403).body("비밀번호 불일치");
        }
    }

    @GetMapping("")
    public String downloadsView() {

        return "common/filestorage/view";
    }
}
