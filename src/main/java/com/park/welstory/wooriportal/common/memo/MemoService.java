package com.park.welstory.wooriportal.common.memo;

import com.park.welstory.wooriportal.member.MemberDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Log4j2
public class MemoService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final MemoRepository memoRepository;

    public List<MemoDTO> list(String group, Pageable pageable, Long memberNum) {

        Page<MemoEntity> memoList = null;

        switch (group) {
            case "sales" -> memoList = memoRepository.findByDivisionGroup(group, pageable);
            case "management" -> memoList = memoRepository.findByDivisionGroup(group, pageable);
            case "facility" -> memoList = memoRepository.findByDivisionGroup(group, pageable);
            case "personal" -> memoList = memoRepository.findByDivisionGroupAndMember_MemberNum(group, memberNum, pageable);
        }
        List<MemoDTO> memoDTOList = new ArrayList<>();
        for (MemoEntity memo : memoList.getContent()) {
            MemoDTO dto = new MemoDTO();
            dto.setMemoNum(memo.getMemoNum());
            dto.setMemoContent(memo.getMemoContent());
            dto.setDivisionGroup(memo.getDivisionGroup());
            dto.setCreatedAt(memo.getCreatedAt());
            dto.setMember(modelMapper.map(memo.getMember(), MemberDTO.class));
            memoDTOList.add(dto);
        }
        return memoDTOList;
    }

    public void addMemo(MemoDTO memoDTO) {
        memoRepository.save(modelMapper.map(memoDTO, MemoEntity.class));
    }

    @Transactional
    public void deleteMemo(Long memoNum){
        MemoEntity memo = memoRepository.findById(memoNum)
                .orElseThrow(() -> new EntityNotFoundException("메모가 존재하지 않습니다."));
        memoRepository.delete(memo);

    }

}
