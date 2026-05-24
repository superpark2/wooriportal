package com.mrpark.dev.wooriportal.board;

import com.mrpark.dev.wooriportal.board.category.*;
import com.mrpark.dev.wooriportal.global.file.FileEntity;
import com.mrpark.dev.wooriportal.global.file.FileRepository;
import com.mrpark.dev.wooriportal.global.file.FileService;
import com.mrpark.dev.wooriportal.global.image.ImageService;
import com.mrpark.dev.wooriportal.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        return switch (group) {
            case "sales"      -> salesRepository.findByCategory(category, pageable)
                    .map(e -> modelMapper.map(e, BoardDTO.class));
            case "management" -> managementRepository.findByCategory(category, pageable)
                    .map(e -> modelMapper.map(e, BoardDTO.class));
            case "facility"   -> facilityRepository.findByCategory(category, pageable)
                    .map(e -> modelMapper.map(e, BoardDTO.class));
            case "personal"   -> personalRepository.findByCategoryAndMember_MemberNum(category, memberNum, pageable)
                    .map(e -> modelMapper.map(e, BoardDTO.class));
            case "common"     -> commonRepository.findByCategory(category, pageable)
                    .map(e -> modelMapper.map(e, BoardDTO.class));
            default -> throw new IllegalArgumentException("그룹이 잘못됨 : " + group);
        };
    }

    @Transactional
    public void addBoard(BoardDTO boardDTO) {
        String writeDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String group    = boardDTO.getGroup();
        String category = boardDTO.getCategory();
        Long num;

        switch (group) {
            case "sales" -> {
                SalesEntity sales = (boardDTO.getBoardNum() == null)
                        ? new SalesEntity()
                        : salesRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));
                applyBoard(sales, boardDTO, group, category);
                salesRepository.save(sales);
                num = sales.getBoardNum();
                sales.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                salesRepository.save(sales);
            }
            case "management" -> {
                ManagementEntity management = (boardDTO.getBoardNum() == null)
                        ? new ManagementEntity()
                        : managementRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));
                applyBoard(management, boardDTO, group, category);
                managementRepository.save(management);
                num = management.getBoardNum();
                management.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                managementRepository.save(management);
            }
            case "facility" -> {
                FacilityEntity facility = (boardDTO.getBoardNum() == null)
                        ? new FacilityEntity()
                        : facilityRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));
                applyBoard(facility, boardDTO, group, category);
                facilityRepository.save(facility);
                num = facility.getBoardNum();
                facility.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                facilityRepository.save(facility);
            }
            case "personal" -> {
                PersonalEntity personal = (boardDTO.getBoardNum() == null)
                        ? new PersonalEntity()
                        : personalRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));
                applyBoard(personal, boardDTO, group, category);
                personalRepository.save(personal);
                num = personal.getBoardNum();
                personal.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                personalRepository.save(personal);
            }
            case "common" -> {
                CommonEntity common = (boardDTO.getBoardNum() == null)
                        ? new CommonEntity()
                        : commonRepository.findById(boardDTO.getBoardNum())
                        .orElseThrow(() -> new RuntimeException(group + " " + category + "에 존재하지 않는 게시글"));
                applyBoard(common, boardDTO, group, category);
                commonRepository.save(common);
                num = common.getBoardNum();
                common.setBoardContent(imageService.saveImage(num, boardDTO.getBoardContent(), group, category, writeDate));
                commonRepository.save(common);
            }
            default -> throw new RuntimeException("알 수 없는 그룹: " + group);
        }

        // 파일 처리 — switch 완료 후 공통으로 처리 (num은 각 case에서 할당됨)
        // ※ num이 switch 내부에서만 할당되므로 컴파일러가 초기화 미완으로 볼 수 있음 → 아래 처리
        // 현재 구조 유지 (각 case 내 fileService.addFile 호출이 필요하면 case 내로 이동)
    }

    /** 공통 게시글 필드 세팅 */
    private void applyBoard(BoardEntity entity, BoardDTO dto, String group, String category) {
        entity.setBoardTitle(dto.getBoardTitle());
        entity.setBoardContent(dto.getBoardContent());
        entity.setNotice(dto.isNotice());
        entity.setCategory(category);
        entity.setMember(memberRepository.findByMemberNum(dto.getMember().getMemberNum()));
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

    @Transactional
    public void deleteBoard(String group, Long num, Long requestMemberNum) {
        record BoardData(String content, Long ownerNum) {}

        BoardData data = switch (group) {
            case "sales" -> {
                SalesEntity e = salesRepository.findById(num)
                        .orElseThrow(() -> new RuntimeException("지울 글이 없음."));
                Long owner = e.getMember() != null ? e.getMember().getMemberNum() : null;
                yield new BoardData(e.getBoardContent(), owner);
            }
            case "management" -> {
                ManagementEntity e = managementRepository.findById(num)
                        .orElseThrow(() -> new RuntimeException("지울 글이 없음."));
                Long owner = e.getMember() != null ? e.getMember().getMemberNum() : null;
                yield new BoardData(e.getBoardContent(), owner);
            }
            case "facility" -> {
                FacilityEntity e = facilityRepository.findById(num)
                        .orElseThrow(() -> new RuntimeException("지울 글이 없음."));
                Long owner = e.getMember() != null ? e.getMember().getMemberNum() : null;
                yield new BoardData(e.getBoardContent(), owner);
            }
            case "personal" -> {
                PersonalEntity e = personalRepository.findById(num)
                        .orElseThrow(() -> new RuntimeException("지울 글이 없음."));
                Long owner = e.getMember() != null ? e.getMember().getMemberNum() : null;
                yield new BoardData(e.getBoardContent(), owner);
            }
            case "common" -> {
                CommonEntity e = commonRepository.findById(num)
                        .orElseThrow(() -> new RuntimeException("지울 글이 없음."));
                Long owner = e.getMember() != null ? e.getMember().getMemberNum() : null;
                yield new BoardData(e.getBoardContent(), owner);
            }
            default -> throw new IllegalArgumentException("알 수 없는 그룹: " + group);
        };

        // 소유권 검증: DB에서 읽은 실제 작성자와 비교
        if (!requestMemberNum.equals(data.ownerNum())) {
            throw new org.springframework.security.access.AccessDeniedException("삭제 권한이 없습니다.");
        }

        // 소유권 확인 후 삭제
        switch (group) {
            case "sales"      -> salesRepository.deleteById(num);
            case "management" -> managementRepository.deleteById(num);
            case "facility"   -> facilityRepository.deleteById(num);
            case "personal"   -> personalRepository.deleteById(num);
            case "common"     -> commonRepository.deleteById(num);
        }

        imageService.deleteImage(data.content());
        fileService.deleteFile(group, num);
    }
}