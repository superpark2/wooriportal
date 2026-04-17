package com.park.welstory.wooriportal.pcinfo;

import com.park.welstory.wooriportal.global.util.LogUtil;
import com.park.welstory.wooriportal.location.LocationEntity;
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
        MultipartFile newImage = pcInfoDTO.getPcInfoImage();
        String existingMeta = null;
        Long targetLocationNum = pcInfoDTO.getLocationNum();
        PcInfoEntity originalEntity = null;
        boolean isNew = (pcInfoDTO.getPcInfoNum() == null);

        if (!isNew) {
            Optional<PcInfoEntity> existingOpt = pcInfoRepository.findById(pcInfoDTO.getPcInfoNum());
            if (existingOpt.isPresent()) {
                originalEntity = existingOpt.get();
                existingMeta = originalEntity.getPcInfoImageMeta();
                if (targetLocationNum == null && originalEntity.getLocation() != null) {
                    targetLocationNum = originalEntity.getLocation().getLocationNum();
                }
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

        // ── 자동 로그 기록 ──────────────────────────────
        if (isNew) {
            // 신규등록
            LogUtil.logPcRegistration(savedEntity);
        } else if (originalEntity != null) {
            // 위치 이동 여부 먼저 체크
            LogUtil.logPcLocationChange(originalEntity, savedEntity);
            // 사양 변경 체크 (CPU, RAM 등)
            LogUtil.logPcSpecChange(originalEntity, savedEntity);
        }
        // ────────────────────────────────────────────────

        return savedEntity;
    }

    @Transactional
    public void pcInfoDelete(Long pcInfoNum) {
        pcInfoRepository.findById(pcInfoNum).ifPresent(entity -> {
            if (entity.getPcInfoImageMeta() != null) {
                deleteLocalFileByMeta(entity.getPcInfoImageMeta());
            }
            // @PreRemove → LogUtil.onPcRemove 가 삭제 로그 자동 기록
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

    // 이미지 저장
    private String savePcImageToLocal(MultipartFile image, Long locationNum) {
        String uuidPrefix = UUID.randomUUID().toString().substring(0, 8);
        String originalName = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        String fileName = uuidPrefix + "_" + originalName;
        String dirPath = "file/pcinfo/" + (locationNum == null ? "unknown" : locationNum);
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