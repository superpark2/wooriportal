package com.park.welstory.wooriportal.pcinfo.pcinfoRequire;

import com.park.welstory.wooriportal.pcinfo.PcInfoEntity;
import com.park.welstory.wooriportal.pcinfo.PcInfoRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PcInfoRequireService {

    private final PcInfoRequireRepository requireRepository;
    private final PcInfoRepository pcInfoRepository;
    private final ModelMapper modelMapper = new ModelMapper();

    public Page<PcInfoRequireDTO> list(Long pcInfoNum, int page, int size) {
        Page<PcInfoRequireEntity> entities = requireRepository.findByPcInfo_PcInfoNumOrderByReNumDesc(pcInfoNum, PageRequest.of(page, size));
        return entities.map(e -> modelMapper.map(e, PcInfoRequireDTO.class));
    }

    public Page<PcInfoRequireDTO> listAll(int page, int size) {
        Page<PcInfoRequireEntity> entities = requireRepository.findByReTypeOrderByReNumDesc("Q", PageRequest.of(page, size));
        Page<PcInfoRequireDTO> dtoPage = entities.map(e -> {
            PcInfoRequireDTO dto = modelMapper.map(e, PcInfoRequireDTO.class);
            // 건물 정보 설정
            if (e.getPcInfo() != null && e.getPcInfo().getLocation() != null) {
                if (e.getPcInfo().getLocation().getLocationParent() != null) {
                    dto.getPcInfo().setBuildingName(e.getPcInfo().getLocation().getLocationParent().getLocationName());
                }
                dto.getPcInfo().setLocationName(e.getPcInfo().getLocation().getLocationName());
            }
            return dto;
        });
        
        // 답변들을 각 질문에 연결
        dtoPage.getContent().forEach(dto -> {
            var answers = requireRepository.findByReParent_ReNumOrderByReNumDesc(dto.getReNum());
            dto.setAnswers(answers.stream().map(e -> modelMapper.map(e, PcInfoRequireDTO.class)).toList());
        });
        
        return dtoPage;
    }

    public Page<PcInfoRequireDTO> listByStatus(String status, int page, int size) {
        Page<PcInfoRequireEntity> entities = requireRepository.findByReStatusOrderByReNumDesc(status, PageRequest.of(page, size));
        Page<PcInfoRequireDTO> dtoPage = entities.map(e -> {
            PcInfoRequireDTO dto = modelMapper.map(e, PcInfoRequireDTO.class);
            // 건물 정보 설정
            if (e.getPcInfo() != null && e.getPcInfo().getLocation() != null) {
                if (e.getPcInfo().getLocation().getLocationParent() != null) {
                    dto.getPcInfo().setBuildingName(e.getPcInfo().getLocation().getLocationParent().getLocationName());
                }
                dto.getPcInfo().setLocationName(e.getPcInfo().getLocation().getLocationName());
            }
            return dto;
        });
        
        // 답변들을 각 질문에 연결
        dtoPage.getContent().forEach(dto -> {
            var answers = requireRepository.findByReParent_ReNumOrderByReNumDesc(dto.getReNum());
            dto.setAnswers(answers.stream().map(e -> modelMapper.map(e, PcInfoRequireDTO.class)).toList());
        });
        
        return dtoPage;
    }

    public PcInfoRequireDTO add(Long pcInfoNum, String author, String seat, String content) {
        PcInfoRequireEntity entity = new PcInfoRequireEntity();
        entity.setReWriter(author);
        entity.setReContent(content);
        entity.setReSeat(seat);
        // 질문자 등록은 고정값으로 처리
        entity.setReType("Q");
        entity.setReStatus("미완료");
        
        PcInfoEntity pc = pcInfoRepository.findById(pcInfoNum)
            .orElseThrow(() -> new IllegalArgumentException("PC 정보를 찾을 수 없습니다. (ID: " + pcInfoNum + ")"));
        entity.setPcInfo(pc);
        
        PcInfoRequireEntity saved = requireRepository.save(entity);
        return modelMapper.map(saved, PcInfoRequireDTO.class);
    }

    public void delete(Long reNum) {
        requireRepository.deleteById(reNum);
    }

    public PcInfoRequireDTO reply(Long parentReNum, String reWriter, String reContent) {
        PcInfoRequireEntity parent = requireRepository.findById(parentReNum).orElseThrow();
        PcInfoRequireEntity entity = new PcInfoRequireEntity();
        entity.setReWriter(reWriter);
        entity.setReContent(reContent);
        entity.setReSeat(null);
        entity.setReType("A");
        entity.setReStatus(null); // 답변은 상태 없음
        entity.setPcInfo(parent.getPcInfo());
        entity.setReParent(parent);
        PcInfoRequireEntity saved = requireRepository.save(entity);
        return modelMapper.map(saved, PcInfoRequireDTO.class);
    }

    public void updateStatus(Long reNum, String status) {
        requireRepository.findById(reNum).ifPresent(e -> { e.setReStatus(status); requireRepository.save(e); });
    }



    public Page<PcInfoRequireDTO> listByPcInfo(Long pcInfoNum, int page, int size) {
        Page<PcInfoRequireEntity> entities = requireRepository.findByPcInfo_PcInfoNumOrderByReNumDesc(pcInfoNum, PageRequest.of(page, size));
        Page<PcInfoRequireDTO> dtoPage = entities.map(e -> {
            PcInfoRequireDTO dto = modelMapper.map(e, PcInfoRequireDTO.class);
            // 건물 정보 설정
            if (e.getPcInfo() != null && e.getPcInfo().getLocation() != null) {
                if (e.getPcInfo().getLocation().getLocationParent() != null) {
                    dto.getPcInfo().setBuildingName(e.getPcInfo().getLocation().getLocationParent().getLocationName());
                }
                dto.getPcInfo().setLocationName(e.getPcInfo().getLocation().getLocationName());
            }
            return dto;
        });
        
        // 답변들을 각 질문에 연결
        dtoPage.getContent().forEach(dto -> {
            if ("Q".equals(dto.getReType())) {
                // 해당 질문의 답변들 찾기
                var answers = requireRepository.findByReParent_ReNumOrderByReNumDesc(dto.getReNum());
                dto.setAnswers(answers.stream().map(e -> modelMapper.map(e, PcInfoRequireDTO.class)).toList());
            }
        });
        
        return dtoPage;
    }
}


