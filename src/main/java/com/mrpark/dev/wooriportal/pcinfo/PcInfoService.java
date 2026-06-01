package com.mrpark.dev.wooriportal.pcinfo;

import com.mrpark.dev.wooriportal.location.LocationEntity;
import com.mrpark.dev.wooriportal.log.LogRepository;
import com.mrpark.dev.wooriportal.log.LogService;
import com.mrpark.dev.wooriportal.pcinfo.require.PcInfoRequireRepository;
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
    private final LogRepository logRepository;
    private final PcInfoRequireRepository requireRepository;

    public List<PcInfoDTO> getList(Long location) {
        List<PcInfoEntity> entity = pcInfoRepository.findByLocation_LocationNum(location);
        entity.sort(Comparator
                .comparing((PcInfoEntity e) -> e.getPcInfoSeatNum().matches("\\d+"))
                .thenComparing(e -> {
                    String s = e.getPcInfoSeatNum();
                    return s.matches("\\d+") ? String.format("%08d", Integer.parseInt(s)) : s;
                })
        );
        return entity.stream()
                .map(e -> modelMapper.map(e, PcInfoDTO.class))
                .toList();
    }

    @Transactional
    public PcInfoEntity pcInfoAdd(PcInfoDTO pcInfoDTO, String imageDelete) {
        MultipartFile newImage = pcInfoDTO.getPcInfoImage();
        String existingMeta   = null;
        Long targetLocationNum = pcInfoDTO.getLocationNum();
        boolean isNew         = (pcInfoDTO.getPcInfoNum() == null);

        // 기존 엔티티 스냅샷 (save() 이후 영속성 컨텍스트 덮어쓰기 전에 캡처)
        String snapCpu = null, snapRam = null, snapStorage = null;
        String snapVga = null, snapMonitor = null, snapIp = null;
        Long snapLocNum = null;

        if (!isNew) {
            Optional<PcInfoEntity> existingOpt = pcInfoRepository.findById(pcInfoDTO.getPcInfoNum());
            if (existingOpt.isPresent()) {
                PcInfoEntity orig = existingOpt.get();
                existingMeta = orig.getPcInfoImageMeta();
                if (targetLocationNum == null && orig.getLocation() != null) {
                    targetLocationNum = orig.getLocation().getLocationNum();
                }
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
        } else if ("true".equalsIgnoreCase(imageDelete) && existingMeta != null) {
            deleteLocalFileByMeta(existingMeta);
            pcInfoDTO.setPcInfoImageMeta(null);
        } else if (existingMeta != null) {
            pcInfoDTO.setPcInfoImageMeta(existingMeta);
        }

        PcInfoEntity entity = modelMapper.map(pcInfoDTO, PcInfoEntity.class);
        if (pcInfoDTO.getLocationNum() != null) {
            LocationEntity locRef = new LocationEntity();
            locRef.setLocationNum(pcInfoDTO.getLocationNum());
            entity.setLocation(locRef);
        }

        PcInfoEntity saved = pcInfoRepository.save(entity);

        // 로그 기록
        if (isNew) {
            logService.savePcLog(saved.getPcInfoNum(), "PC 신규 등록");
        } else {
            Long newLocNum = saved.getLocation() != null ? saved.getLocation().getLocationNum() : null;
            if (!Objects.equals(snapLocNum, newLocNum)) {
                logService.savePcLog(saved.getPcInfoNum(), "PC 위치 변경");
            }

            StringBuilder specLog = new StringBuilder();
            appendIfChanged(specLog, "CPU",      snapCpu,     saved.getPcInfoCpu());
            appendIfChanged(specLog, "RAM",      snapRam,     saved.getPcInfoRam());
            appendIfChanged(specLog, "저장장치", snapStorage, saved.getPcInfoStorage());
            appendIfChanged(specLog, "VGA",      snapVga,     saved.getPcInfoVga());
            appendIfChanged(specLog, "모니터",   snapMonitor, saved.getPcInfoMonitor());
            appendIfChanged(specLog, "IP",       snapIp,      saved.getPcInfoIp());

            if (!specLog.isEmpty()) {
                logService.savePcLog(saved.getPcInfoNum(), "사양 변경 — " + specLog.toString().trim());
            }
        }

        return saved;
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
            // 삭제 마커 로그 내용(태그 포함)을 삭제 전에 미리 구성
            String deleteLog = buildDeleteLogContent(entity);

            // 1) 기존 로그/요청의 FK 해제 (log, require 의 pcinfo_num → null)
            //    삭제 전 별도 트랜잭션(REQUIRES_NEW) 로그 쓰기를 두면 같은 행에 대한
            //    벌크 UPDATE와 경합해 MariaDB 1020(record has changed)이 발생하므로,
            //    로그 쓰기는 삭제 이후 같은 트랜잭션에서 pc 참조 없이 수행한다.
            logRepository.detachPcInfo(pcInfoNum);
            requireRepository.detachPcInfo(pcInfoNum);

            // 2) PC 삭제
            if (entity.getPcInfoImageMeta() != null) deleteLocalFileByMeta(entity.getPcInfoImageMeta());
            pcInfoRepository.deleteById(pcInfoNum);

            // 3) 삭제 마커 로그 저장 (pcInfo=null, 텍스트로만 잔존)
            logService.saveDetachedLog(deleteLog);
        });
    }

    /** "[건물] [실] [좌석] PC 삭제" 형태의 로그 내용 구성 */
    private String buildDeleteLogContent(PcInfoEntity pc) {
        String building = "미지정";
        String room     = "미지정";
        String seat     = pc.getPcInfoSeatNum() != null ? pc.getPcInfoSeatNum() : "미지정";
        if (pc.getLocation() != null) {
            room = pc.getLocation().getLocationName();
            if (pc.getLocation().getLocationParent() != null) {
                building = pc.getLocation().getLocationParent().getLocationName();
            }
        }
        return "[" + building + "] [" + room + "] [" + seat + "] PC 삭제";
    }

    public PcInfoDTO getById(Long pcInfoNum) {
        PcInfoEntity entity = pcInfoRepository.findById(pcInfoNum).orElseThrow();
        PcInfoDTO dto = modelMapper.map(entity, PcInfoDTO.class);
        if (entity.getLocation() != null) {
            LocationEntity parent = entity.getLocation().getLocationParent();
            dto.setBuildingNum(parent != null ? parent.getLocationNum() : null);
            dto.setBuildingName(parent != null ? parent.getLocationName() : null);
        }
        return dto;
    }

    // 버그수정: pcInfoUpdate 중복 메서드 제거 — 호출부에서 pcInfoAdd를 직접 사용
    // (기존에 pcInfoUpdate가 pcInfoAdd를 그대로 위임만 했음)

    private String savePcImageToLocal(MultipartFile image, Long locationNum) {
        String uuidPrefix   = UUID.randomUUID().toString().substring(0, 8);
        String originalName = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        String fileName     = uuidPrefix + "_" + originalName;
        String locationDir  = locationNum == null ? "unknown" : String.valueOf(locationNum);
        File dir = new File("file/pcinfo/" + locationDir);
        if (!dir.exists()) dir.mkdirs();
        try (InputStream in = image.getInputStream()) {
            Files.copy(in, new File(dir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패: " + originalName, e);
        }
        return "/file/pcinfo/" + locationDir + "/" + fileName;
    }

    private void deleteLocalFileByMeta(String metaPath) {
        File f = new File("." + metaPath);
        if (f.exists()) f.delete();
    }
}
