package com.park.welstory.wooriportal.pcinfo;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.park.welstory.wooriportal.location.LocationEntity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PcinfoService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final PcInfoRepository pcInfoRepository;

    public List<PcInfoDTO> getList(Long location) {
        List<PcInfoEntity> entity = pcInfoRepository.findByLocation_LocationNum(location);
        List<PcInfoDTO> list = new ArrayList<>();
        for (PcInfoEntity entityTemp : entity) {
            PcInfoDTO pcInfoDTO = modelMapper.map(entityTemp, PcInfoDTO.class);
            list.add(pcInfoDTO);
        }
        return list;
    }

    public void pcInfoAdd (PcInfoDTO pcInfoDTO, String imageDelete) {
        // 공용 로직: 생성/수정 모두 처리
        MultipartFile newImage = pcInfoDTO.getPcInfoImage();
        String existingMeta = null;
        Long targetLocationNum = pcInfoDTO.getLocationNum();

        if (pcInfoDTO.getPcInfoNum() != null) {
            Optional<PcInfoEntity> existingOpt = pcInfoRepository.findById(pcInfoDTO.getPcInfoNum());
            if (existingOpt.isPresent()) {
                PcInfoEntity existing = existingOpt.get();
                existingMeta = existing.getPcInfoImageMeta();
                if (targetLocationNum == null && existing.getLocation() != null) {
                    targetLocationNum = existing.getLocation().getLocationNum();
                }
            }
        }

        // 새 이미지 업로드가 있는 경우 → 기존 이미지 삭제 후 저장
        if (newImage != null && !newImage.isEmpty()) {
            if (existingMeta != null) {
                deleteLocalFileByMeta(existingMeta);
            }
            String savedMeta = savePcImageToLocal(newImage, targetLocationNum);
            pcInfoDTO.setPcInfoImageMeta(savedMeta);
        } else {
            // 새 이미지 없고, 삭제 체크한 경우 → 기존 파일 삭제 및 메타 제거
            if ("true".equalsIgnoreCase(imageDelete) && existingMeta != null) {
                deleteLocalFileByMeta(existingMeta);
                pcInfoDTO.setPcInfoImageMeta(null);
            } else {
                // 그대로 유지
                if (existingMeta != null) {
                    pcInfoDTO.setPcInfoImageMeta(existingMeta);
                }
            }
        }

        PcInfoEntity entity = modelMapper.map(pcInfoDTO, PcInfoEntity.class);
        // DTO의 locationNum을 ManyToOne 연관관계에 명시적으로 매핑
        if (pcInfoDTO.getLocationNum() != null) {
            LocationEntity locRef = new LocationEntity();
            locRef.setLocationNum(pcInfoDTO.getLocationNum());
            entity.setLocation(locRef);
        }
        pcInfoRepository.save(entity);
    }

    public void pcInfoDelete (Long pcInfoNum) {
        // 삭제 시 로컬 이미지도 함께 제거
        pcInfoRepository.findById(pcInfoNum).ifPresent(e -> {
            if (e.getPcInfoImageMeta() != null) deleteLocalFileByMeta(e.getPcInfoImageMeta());
            pcInfoRepository.deleteById(pcInfoNum);
        });
    }

    public PcInfoDTO getById(Long pcInfoNum) {
        PcInfoEntity entity = pcInfoRepository.findById(pcInfoNum).orElseThrow();
        PcInfoDTO dto = modelMapper.map(entity, PcInfoDTO.class);
        if (entity.getLocation() != null) {
            dto.setBuildingNum(entity.getLocation().getLocationParent() != null ? entity.getLocation().getLocationParent().getLocationNum() : null);
            dto.setBuildingName(entity.getLocation().getLocationParent() != null ? entity.getLocation().getLocationParent().getLocationName() : null);
        }
        return dto;
    }

    public void pcInfoUpdate(PcInfoDTO pcInfoDTO, String imageDelete) {
        // add 로직이 통합 처리하므로 이 메서드는 add를 위임 호출
        pcInfoAdd(pcInfoDTO, imageDelete);
    }

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
        // metaPath 예: /file/pcinfo/{locationNum}/xxxx_name.ext → 로컬은 ./file/pcinfo/... 로 매핑
        File f = new File("." + metaPath);
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
