package com.park.welstory.wooriportal.pcinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.park.welstory.wooriportal.location.LocationEntity;
import com.park.welstory.wooriportal.util.LogUtil;
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
@Log4j2
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

    @Transactional
    public void pcInfoAdd (PcInfoDTO pcInfoDTO, String imageDelete) {
        MultipartFile newImage = pcInfoDTO.getPcInfoImage();
        String existingMeta = null;
        Long targetLocationNum = pcInfoDTO.getLocationNum();
        PcInfoEntity originalEntity = null;

        if (pcInfoDTO.getPcInfoNum() != null) {
            Optional<PcInfoEntity> existingOpt = pcInfoRepository.findById(pcInfoDTO.getPcInfoNum());
            if (existingOpt.isPresent()) {
                originalEntity = existingOpt.get();
                existingMeta = originalEntity.getPcInfoImageMeta();
                if (targetLocationNum == null && originalEntity.getLocation() != null) {
                    targetLocationNum = originalEntity.getLocation().getLocationNum();
                }
            }
        }

        if (newImage != null && !newImage.isEmpty()) {
            if (existingMeta != null) {
                deleteLocalFileByMeta(existingMeta);
            }
            String savedMeta = savePcImageToLocal(newImage, targetLocationNum);
            pcInfoDTO.setPcInfoImageMeta(savedMeta);
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
        
        // 수정인 경우 변경사항 로그 기록
        if (originalEntity != null) {
            LogUtil.logPcSpecChange(originalEntity, savedEntity);
        }
    }

    @Transactional
    public void pcInfoDelete (Long pcInfoNum) {
        var pcEntity = pcInfoRepository.findById(pcInfoNum);
        if (pcEntity.isPresent()) {
            var entity = pcEntity.get();
            
            // 이미지 파일이 있으면 로컬에서 삭제
            if (entity.getPcInfoImageMeta() != null) {
                deleteLocalFileByMeta(entity.getPcInfoImageMeta());
            }
            
            // PC 정보 삭제
            pcInfoRepository.deleteById(pcInfoNum);
        }
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
        File f = new File("." + metaPath);
        if (f.exists()) {
            f.delete();
        }
    }
    

}
