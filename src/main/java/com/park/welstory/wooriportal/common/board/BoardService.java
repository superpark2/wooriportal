package com.park.welstory.wooriportal.common.board;

import com.park.welstory.wooriportal.common.file.FileEntity;
import com.park.welstory.wooriportal.common.file.FileRepository;
import com.park.welstory.wooriportal.common.file.FileService;
import com.park.welstory.wooriportal.common.image.ImageService;
import com.park.welstory.wooriportal.facility.FacilityEntity;
import com.park.welstory.wooriportal.facility.FacilityRepository;
import com.park.welstory.wooriportal.management.ManagementEntity;
import com.park.welstory.wooriportal.management.ManagementRepository;
import com.park.welstory.wooriportal.member.MemberRepository;
import com.park.welstory.wooriportal.personal.PersonalEntity;
import com.park.welstory.wooriportal.personal.PersonalRepository;
import com.park.welstory.wooriportal.sales.SalesEntity;
import com.park.welstory.wooriportal.sales.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BoardService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final MemberRepository memberRepository;
    private final SalesRepository salesRepository;
    private final ManagementRepository managementRepository;
    private final FacilityRepository facilityRepository;
    private final PersonalRepository personalRepository;
    private final ImageService imageService;
    private final FileService fileService;
    private final FileRepository fileRepository;

    public Page<BoardDTO> list(String group, String category, Long memberNum, Pageable pageable) {
        Page<BoardDTO> dto = new PageImpl<>(new ArrayList<>());
        switch (group) {
            case "sales" -> {
                Page<SalesEntity> salesEntity = salesRepository.findByCategory(category, pageable);
                dto = salesEntity.map(entityTemp ->
                        modelMapper.map(entityTemp, BoardDTO.class));
            }
            case "management" -> {
                Page<ManagementEntity> managementEntity = managementRepository.findByCategory(category, pageable);
                dto = managementEntity.map(entityTemp -> modelMapper.map(entityTemp, BoardDTO.class));
            }
            case "facility" -> {
                Page<FacilityEntity> facilityEntity = facilityRepository.findByCategory(category, pageable);
                dto = facilityEntity.map(entityTemp -> modelMapper.map(entityTemp, BoardDTO.class));
            }
            case "personal" -> {
                Page<PersonalEntity> personalEntity = personalRepository.findByCategoryAndMember_MemberNum(category, memberNum, pageable);
                dto = personalEntity.map(entityTemp -> modelMapper.map(entityTemp, BoardDTO.class));
            }
            case "common" -> {
                List<BoardDTO> allList = new ArrayList<>();
                for (SalesEntity salesEntityTemp : salesRepository.findByCategory(category, pageable)) {
                    allList.add(modelMapper.map(salesEntityTemp, BoardDTO.class));
                }
                for (ManagementEntity managementEntityTemp : managementRepository.findByCategory(category, pageable)) {
                    allList.add(modelMapper.map(managementEntityTemp, BoardDTO.class));
                }
                for (FacilityEntity facilityEntityTemp : facilityRepository.findByCategory(category, pageable)) {
                    allList.add(modelMapper.map(facilityEntityTemp, BoardDTO.class));
                }
            }
            default -> throw new IllegalArgumentException("그룹이 잘못됨 : " + group);
        }
        return dto;
    }


    @Transactional
    public void addBoard(BoardDTO boardDTO) {
        String writeDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long num;
        String group = boardDTO.getGroup();
        String category = boardDTO.getCategory();
        List<MultipartFile> files = boardDTO.getFiles();

        switch (group) {
            case "sales" -> {
                SalesEntity sales = (boardDTO.getBoardNum() == null) ? new SalesEntity() : salesRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));

                sales.setBoardTitle(boardDTO.getBoardTitle());
                sales.setBoardContent(boardDTO.getBoardContent());
                sales.setNotice(boardDTO.isNotice());
                sales.setCategory(category);
                sales.setMember(memberRepository.findByMemberNum(boardDTO.getMember().getMemberNum()));
                salesRepository.save(sales);
                num = sales.getBoardNum();
                sales.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                salesRepository.save(sales);
            }

            case "management" -> {
                ManagementEntity management = (boardDTO.getBoardNum() == null) ? new ManagementEntity() : managementRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));

                management.setBoardTitle(boardDTO.getBoardTitle());
                management.setBoardContent(boardDTO.getBoardContent());
                management.setNotice(boardDTO.isNotice());
                management.setCategory(category);
                management.setMember(memberRepository.findByMemberNum(boardDTO.getMember().getMemberNum()));
                managementRepository.save(management);
                num = management.getBoardNum();
                management.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                managementRepository.save(management);
            }

            case "facility" -> {
                FacilityEntity facility = (boardDTO.getBoardNum() == null) ? new FacilityEntity() : facilityRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));

                facility.setBoardTitle(boardDTO.getBoardTitle());
                facility.setBoardContent(boardDTO.getBoardContent());
                facility.setNotice(boardDTO.isNotice());
                facility.setCategory(category);
                facility.setMember(memberRepository.findByMemberNum(boardDTO.getMember().getMemberNum()));
                facilityRepository.save(facility);
                num = facility.getBoardNum();
                facility.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                facilityRepository.save(facility);
            }

            case "personal" -> {
                PersonalEntity personal = (boardDTO.getBoardNum() == null) ? new PersonalEntity() : personalRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));

                personal.setBoardTitle(boardDTO.getBoardTitle());
                personal.setBoardContent(boardDTO.getBoardContent());
                personal.setNotice(boardDTO.isNotice());
                personal.setCategory(category);
                personal.setMember(memberRepository.findByMemberNum(boardDTO.getMember().getMemberNum()));
                personalRepository.save(personal);
                num = personal.getBoardNum();
                personal.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                personalRepository.save(personal);
            }

            default -> throw new RuntimeException("알 수 없는 그룹: " + group);
        }
        if (files != null && !files.isEmpty()) {
            fileService.addFile(group, category, num, files);
        }
    }

    public BoardDTO detailBoard (String group, String category, Long num) {
        BoardEntity board = switch (group) {
            case "sales" -> salesRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            case "management" -> managementRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            case "facility" -> facilityRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            default -> throw new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다.");
        };
        BoardDTO dto = modelMapper.map(board, BoardDTO.class);
        List<FileEntity> files = (fileRepository.findByBoardNumAndDivisionGroup(num, group));
        dto.setFilesMeta(files);
        return dto;
    }


    public void deleteBoard (String group, Long num) {
        String content = "";
        switch (group) {
            case "sales" -> {content = salesRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                salesRepository.deleteById(num);}
            case "management" -> {content = managementRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                managementRepository.deleteById(num);}
            case "facility" -> {content = facilityRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                facilityRepository.deleteById(num);}
        }
        imageService.deleteImage(content);
        fileService.deleteFile(group, num);
    }


}
