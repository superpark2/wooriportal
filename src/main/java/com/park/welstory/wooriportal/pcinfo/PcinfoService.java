package com.park.welstory.wooriportal.pcinfo;

import com.park.welstory.wooriportal.location.LocationEntity;
import com.park.welstory.wooriportal.log.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RequiredArgsConstructor
@Service
@Log4j2
public class PcInfoService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final PcInfoRepository pcInfoRepository;
    private final LogService logService;

    public List<PcInfoDTO> getList(Long location) {
        List<PcInfoEntity> entity = pcInfoRepository.findByLocation_LocationNum(location);
        entity.sort(Comparator
                .comparing((PcInfoEntity e) -> e.getPcInfoSeatNum().matches("\\d+"))
                .thenComparing(e -> {
                    String s = e.getPcInfoSeatNum();
                    return s.matches("\\d+") ? String.format("%08d", Integer.parseInt(s)) : s;
                })
        );
        List<PcInfoDTO> list = new ArrayList<>();
        for (PcInfoEntity entityTemp : entity) {
            list.add(modelMapper.map(entityTemp, PcInfoDTO.class));
        }
        return list;
    }

    @Transactional
    public PcInfoEntity pcInfoAdd(PcInfoDTO pcInfoDTO, String imageDelete) {
        MultipartFile newImage    = pcInfoDTO.getPcInfoImage();
        String existingMeta       = null;
        Long targetLocationNum    = pcInfoDTO.getLocationNum();
        boolean isNew             = (pcInfoDTO.getPcInfoNum() == null);

        // ── 원본 스냅샷 ─────────────────────────────────────────────
        // JPA 영속성 컨텍스트가 save() 후 동일 ID 엔티티를 덮어쓰기 때문에
        // findById 직후 String 값으로 즉시 복사해 두어야 비교가 정확하다.
        String snapCpu      = null;
        String snapRam      = null;
        String snapStorage  = null;
        String snapVga      = null;
        String snapMonitor  = null;
        String snapIp       = null;
        Long   snapLocNum   = null;

        if (!isNew) {
            Optional<PcInfoEntity> existingOpt = pcInfoRepository.findById(pcInfoDTO.getPcInfoNum());
            if (existingOpt.isPresent()) {
                PcInfoEntity orig = existingOpt.get();
                existingMeta  = orig.getPcInfoImageMeta();
                if (targetLocationNum == null && orig.getLocation() != null) {
                    targetLocationNum = orig.getLocation().getLocationNum();
                }
                // 스냅샷 — save 이전에 반드시 여기서 캡처
                snapCpu     = orig.getPcInfoCpu();
                snapRam     = orig.getPcInfoRam();
                snapStorage = orig.getPcInfoStorage();
                snapVga     = orig.getPcInfoVga();
                snapMonitor = orig.getPcInfoMonitor();
                snapIp      = orig.getPcInfoIp();
                snapLocNum  = orig.getLocation() != null ? orig.getLocation().getLocationNum() : null;
            }
        }

        // 이미지 처리
        if (newImage != null && !newImage.isEmpty()) {
            if (existingMeta != null) deleteLocalFileByMeta(existingMeta);
            pcInfoDTO.setPcInfoImageMeta(savePcImageToLocal(newImage, targetLocationNum));
        } else {
            if ("true".equalsIgnoreCase(imageDelete) && existingMeta != null) {
                deleteLocalFileByMeta(existingMeta);
                pcInfoDTO.setPcInfoImageMeta(null);
            } else if (existingMeta != null) {
                pcInfoDTO.setPcInfoImageMeta(existingMeta);
            }
        }

        PcInfoEntity entity = modelMapper.map(pcInfoDTO, PcInfoEntity.class);
        if (pcInfoDTO.getLocationNum() != null) {
            LocationEntity locRef = new LocationEntity();
            locRef.setLocationNum(pcInfoDTO.getLocationNum());
            entity.setLocation(locRef);
        }

        PcInfoEntity savedEntity = pcInfoRepository.save(entity);
        // save 이후 영속성 컨텍스트가 orig 를 덮어쓰므로 스냅샷 값으로만 비교

        // ── 로그 기록 ────────────────────────────────────────────────
        if (isNew) {
            logService.savePcLog(savedEntity.getPcInfoNum(), "PC 신규 등록");
        } else {
            // 위치 변경
            Long newLocNum = savedEntity.getLocation() != null ? savedEntity.getLocation().getLocationNum() : null;
            if (!Objects.equals(snapLocNum, newLocNum)) {
                logService.savePcLog(savedEntity.getPcInfoNum(), "PC 위치 변경");
            }

            // 사양 변경
            StringBuilder specLog = new StringBuilder();
            appendIfChanged(specLog, "CPU",      snapCpu,     savedEntity.getPcInfoCpu());
            appendIfChanged(specLog, "RAM",      snapRam,     savedEntity.getPcInfoRam());
            appendIfChanged(specLog, "저장장치", snapStorage, savedEntity.getPcInfoStorage());
            appendIfChanged(specLog, "VGA",      snapVga,     savedEntity.getPcInfoVga());
            appendIfChanged(specLog, "모니터",   snapMonitor, savedEntity.getPcInfoMonitor());
            appendIfChanged(specLog, "IP",       snapIp,      savedEntity.getPcInfoIp());

            if (specLog.length() > 0) {
                logService.savePcLog(savedEntity.getPcInfoNum(), "사양 변경 — " + specLog.toString().trim());
            }
        }
        // ────────────────────────────────────────────────────────────

        return savedEntity;
    }

    private void appendIfChanged(StringBuilder sb, String fieldName, String oldVal, String newVal) {
        String o = oldVal == null ? "" : oldVal.trim();
        String n = newVal == null ? "" : newVal.trim();
        if (!o.equals(n)) {
            sb.append(fieldName).append(": ").append(o).append(" → ").append(n).append("  ");
        }
    }

    @Transactional
    public void pcInfoDelete(Long pcInfoNum) {
        pcInfoRepository.findById(pcInfoNum).ifPresent(entity -> {
            logService.savePcLog(pcInfoNum, "PC 삭제");
            if (entity.getPcInfoImageMeta() != null) {
                deleteLocalFileByMeta(entity.getPcInfoImageMeta());
            }
            pcInfoRepository.deleteById(pcInfoNum);
        });
    }

    public PcInfoDTO getById(Long pcInfoNum) {
        PcInfoEntity entity = pcInfoRepository.findById(pcInfoNum).orElseThrow();
        PcInfoDTO dto = modelMapper.map(entity, PcInfoDTO.class);
        if (entity.getLocation() != null) {
            dto.setBuildingNum(entity.getLocation().getLocationParent() != null
                    ? entity.getLocation().getLocationParent().getLocationNum() : null);
            dto.setBuildingName(entity.getLocation().getLocationParent() != null
                    ? entity.getLocation().getLocationParent().getLocationName() : null);
        }
        return dto;
    }

    public void pcInfoUpdate(PcInfoDTO pcInfoDTO, String imageDelete) {
        pcInfoAdd(pcInfoDTO, imageDelete);
    }

    private String savePcImageToLocal(MultipartFile image, Long locationNum) {
        String uuidPrefix   = UUID.randomUUID().toString().substring(0, 8);
        String originalName = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        String fileName     = uuidPrefix + "_" + originalName;
        String dirPath      = "file/pcinfo/" + (locationNum == null ? "unknown" : locationNum);
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, fileName);
        try (InputStream in = image.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패: " + originalName, e);
        }
        return "/file/pcinfo/" + (locationNum == null ? "unknown" : locationNum) + "/" + fileName;
    }

    private void deleteLocalFileByMeta(String metaPath) {
        File f = new File("." + metaPath);
        if (f.exists()) f.delete();
    }
}