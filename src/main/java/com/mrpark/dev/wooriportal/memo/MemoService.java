package com.mrpark.dev.wooriportal.memo;

import com.mrpark.dev.wooriportal.member.MemberDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Log4j2
public class MemoService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final MemoRepository memoRepository;

    public List<MemoDTO> list(String group, Pageable pageable, Long memberNum) {
        // 버그수정: null 초기화 제거 → NPE 위험 없음, switch default로 예외 처리
        Page<MemoEntity> memoList = switch (group) {
            case "sales", "management", "facility" ->
                    memoRepository.findByDivisionGroup(group, pageable);
            case "personal" ->
                    memoRepository.findByDivisionGroupAndMember_MemberNum(group, memberNum, pageable);
            default -> throw new IllegalArgumentException("알 수 없는 그룹: " + group);
        };

        return memoList.getContent().stream()
                .map(memo -> {
                    MemoDTO dto = new MemoDTO();
                    dto.setMemoNum(memo.getMemoNum());
                    dto.setMemoContent(memo.getMemoContent());
                    dto.setDivisionGroup(memo.getDivisionGroup());
                    dto.setCreatedAt(memo.getCreatedAt());
                    dto.setMember(modelMapper.map(memo.getMember(), MemberDTO.class));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void addMemo(MemoDTO memoDTO) {
        memoRepository.save(modelMapper.map(memoDTO, MemoEntity.class));
    }

    @Transactional
    public void deleteMemo(Long memoNum) {
        MemoEntity memo = memoRepository.findById(memoNum)
                .orElseThrow(() -> new EntityNotFoundException("메모가 존재하지 않습니다."));
        memoRepository.delete(memo);
    }
}
