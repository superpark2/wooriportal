package com.park.welstory.wooriportal.common.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;

@Component
public class TempImageCleanupScheduler {

	private static final Logger log = LoggerFactory.getLogger(TempImageCleanupScheduler.class);

	@Value("${spring.web.resources.static-locations:file:./file/}")
	private String staticLocations;

	@Scheduled(cron = "0 0 0 ? * MON", zone = "Asia/Seoul")
	public void weeklyCleanup() {
		Path fileRoot = resolveLocalFileRoot(staticLocations);
		if (fileRoot == null) {
			log.warn("[Temp 파일] 읽기 실패 : {}", staticLocations);
			return;
		}
		try {
			cleanupOlderThan(fileRoot, Duration.ofHours(1));
		} catch (IOException e) {
			log.error("[Temp 파일] 정리 중 오류", e);
		}
	}

	private Path resolveLocalFileRoot(String locationsProp) {
		if (locationsProp == null) return Paths.get("./file/").toAbsolutePath().normalize();
		String[] parts = locationsProp.split(",");
		for (String p : parts) {
			p = p.trim();
			if (p.startsWith("file:")) {
				String local = p.substring("file:".length());
				return Paths.get(local).toAbsolutePath().normalize();
			}
		}
		return Paths.get("./file/").toAbsolutePath().normalize();
	}

	private void cleanupOlderThan(Path fileRoot, Duration olderThan) throws IOException {
		final Instant threshold = Instant.now().minus(olderThan);

		Files.walkFileTree(fileRoot, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

				if (dir.getNameCount() >= 2) {
					Path last = dir.getFileName();
					Path parent = dir.getParent() != null ? dir.getParent().getFileName() : null;
					if (last != null && parent != null && "temp".equals(last.toString()) && "image".equals(parent.toString())) {

						cleanupTempDir(dir, threshold);

					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void cleanupTempDir(Path tempDir, Instant threshold) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
			for (Path p : stream) {
				try {
					if (Files.isDirectory(p)) continue;
					BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
					Instant created = attrs.creationTime().toInstant();
					Instant modified = attrs.lastModifiedTime().toInstant();
					Instant basis = created != null ? created : modified;
					if (basis.isBefore(threshold)) {
						Files.deleteIfExists(p);
						log.info("[TempImageCleanup] 삭제: {}", p);
					}
				} catch (Exception ex) {
					log.warn("[TempImageCleanup] 파일 삭제 실패: {} - {}", p, ex.getMessage());
				}
			}
		} catch (IOException e) {
			log.warn("[TempImageCleanup] 폴더 접근 실패: {} - {}", tempDir, e.getMessage());
		}
	}
}


