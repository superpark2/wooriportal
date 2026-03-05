package com.park.welstory.wooriportal.board;

import com.park.welstory.wooriportal.board.category.CommonEntity;
import com.park.welstory.wooriportal.board.category.CommonRepository;
import com.park.welstory.wooriportal.global.file.FileEntity;
import com.park.welstory.wooriportal.global.file.FileRepository;
import com.park.welstory.wooriportal.global.file.FileService;
import com.park.welstory.wooriportal.global.image.ImageService;
import com.park.welstory.wooriportal.board.category.FacilityEntity;
import com.park.welstory.wooriportal.board.category.FacilityRepository;
import com.park.welstory.wooriportal.board.category.ManagementEntity;
import com.park.welstory.wooriportal.board.category.ManagementRepository;
import com.park.welstory.wooriportal.member.MemberRepository;
import com.park.welstory.wooriportal.board.category.PersonalEntity;
import com.park.welstory.wooriportal.board.category.PersonalRepository;
import com.park.welstory.wooriportal.board.category.SalesEntity;
import com.park.welstory.wooriportal.board.category.SalesRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final CommonRepository commonRepository;

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
                Page<CommonEntity> commonEntity = commonRepository.findByCategory(category, pageable);
                dto = commonEntity.map(entityTemp -> modelMapper.map(entityTemp, BoardDTO.class));
            }
            default -> throw new IllegalArgumentException("그룹이 잘못됨 : " + group);
        }
        return dto;
    }


    @Transactional
    public void addBoard(BoardDTO boardDTO) {
        String writeDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long num;
        String group    = boardDTO.getGroup();
        String category = boardDTO.getCategory();

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

            case "common" -> {
                CommonEntity common = (boardDTO.getBoardNum() == null) ? new CommonEntity() : commonRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));

                common.setBoardTitle(boardDTO.getBoardTitle());
                common.setBoardContent(boardDTO.getBoardContent());
                common.setNotice(boardDTO.isNotice());
                common.setCategory(category);
                common.setMember(memberRepository.findByMemberNum(boardDTO.getMember().getMemberNum()));
                commonRepository.save(common);
                num = common.getBoardNum();
                common.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                commonRepository.save(common);
            }

            default -> throw new RuntimeException("알 수 없는 그룹: " + group);
        }

        // 파일 처리
        fileService.addFile(group, category, num, boardDTO.getNewFiles(), boardDTO.getExistingFilePaths());
    }

    public BoardDTO detailBoard(String group, String category, Long num) {
        BoardEntity board = switch (group) {
            case "sales"      -> salesRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            case "management" -> managementRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            case "facility"   -> facilityRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            case "personal"   -> personalRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            case "common"     -> commonRepository.findById(num)
                    .orElseThrow(() -> new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다."));
            default -> throw new RuntimeException(group + " " + category + " 게시글을 찾지 못했습니다.");
        };
        BoardDTO dto = modelMapper.map(board, BoardDTO.class);
        List<FileEntity> files = fileRepository.findByBoardNumAndDivisionGroup(num, group);
        dto.setFilesMeta(files);
        return dto;
    }


    public void deleteBoard(String group, Long num) {
        String content = "";
        switch (group) {
            case "sales"      -> { content = salesRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                salesRepository.deleteById(num); }
            case "management" -> { content = managementRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                managementRepository.deleteById(num); }
            case "facility"   -> { content = facilityRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                facilityRepository.deleteById(num); }
            case "personal"   -> { content = personalRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                personalRepository.deleteById(num); }
            case "common"     -> { content = commonRepository.findById(num).orElseThrow(() -> new RuntimeException("지울 글이 없음.")).getBoardContent();
                commonRepository.deleteById(num); }
        }
        imageService.deleteImage(content);
        fileService.deleteFile(group, num);
    }
}
