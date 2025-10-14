package com.park.welstory.wooriportal.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
public class LocationService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final LocationRepository locationRepository;


    public List<LocationDTO> getListLocation(String type, Long buildingNum){

        log.info("섹스" + type);
        log.info("섹스" + buildingNum);

        List<LocationEntity> entityList;
        if (buildingNum == null) {
            entityList = locationRepository.findByLocationType(type);
        } else {entityList = locationRepository.findByLocationTypeAndLocationParent_LocationNum(type, buildingNum);} 

        List<LocationDTO> dtoList = new ArrayList<>();

        for(LocationEntity entity : entityList){
            LocationDTO dto = modelMapper.map(entity, LocationDTO.class);
            dtoList.add(dto);
        }
        log.info("{}시팔년", dtoList);
        return dtoList;
    }

    public void locationAdd(LocationDTO dto, String imageDelete){
        MultipartFile newImage = dto.getLocationImage();
        String existingMeta = null;

        if (dto.getLocationNum() != null) {
            Optional<LocationEntity> opt = locationRepository.findById(dto.getLocationNum());
            if (opt.isPresent()) {
                existingMeta = opt.get().getLocationImageMeta();
            }
        }

        if (newImage != null && !newImage.isEmpty()) {
            if (existingMeta != null) deleteLocalFileByMeta(existingMeta);
            String saved = saveLocationImageToLocal(newImage);
            dto.setLocationImageMeta(saved);
        } else {
            if ("true".equalsIgnoreCase(imageDelete) && existingMeta != null) {
                deleteLocalFileByMeta(existingMeta);
                dto.setLocationImageMeta(null);
            } else if (existingMeta != null) {
                dto.setLocationImageMeta(existingMeta);
            }
        }

        locationRepository.save(modelMapper.map(dto, LocationEntity.class));
    }

    public void locationDelete(LocationDTO dto){
        if (dto.getLocationNum() == null) return;
        locationRepository.findById(dto.getLocationNum()).ifPresent(e -> {
            if (e.getLocationImageMeta() != null) deleteLocalFileByMeta(e.getLocationImageMeta());
            locationRepository.delete(e);
        });
    }

    private String saveLocationImageToLocal(MultipartFile image) {
        String uuidPrefix = UUID.randomUUID().toString().substring(0, 8);
        String originalName = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        String fileName = uuidPrefix + "_" + originalName;

        String dirPath = "file/location";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);
        try (InputStream in = image.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패: " + originalName, e);
        }
        return "/file/location/" + fileName;
    }

    private void deleteLocalFileByMeta(String metaPath) {
        File f = new File("." + metaPath);
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
