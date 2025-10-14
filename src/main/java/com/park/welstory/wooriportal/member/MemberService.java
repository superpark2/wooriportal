package com.park.welstory.wooriportal.member;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final ModelMapper modelMapper = new ModelMapper();
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void addMember(MemberDTO memberDTO) {

        memberDTO.setMemberPassword(passwordEncoder.encode(memberDTO.getMemberPassword()));
        memberDTO.setMemberRole("wait");

        if (memberDTO.getMemberPicture() != null) {

            String basePath = new File("file/profileimg").getAbsolutePath();
            File dir = new File(basePath);
            if (!dir.exists()) dir.mkdirs();

            String originalName = memberDTO.getMemberPicture().getOriginalFilename();
            String extension = "";

            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex != -1 && dotIndex < originalName.length() - 1) {
                extension = originalName.substring(dotIndex);
            }

            String fileName = memberDTO.getMemberId() + extension;
            File saveFile = new File(dir, fileName);

            try {
                memberDTO.getMemberPicture().transferTo(saveFile);
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 실패", e);
            }

            memberDTO.setMemberPictureMeta("/file/profileimg/" + fileName);
        }

        memberRepository.save(modelMapper.map(memberDTO, MemberEntity.class));
    }

    public MemberDTO getMember (Long memberNum) {
        memberRepository.findByMemberNum(memberNum);
        return modelMapper.map(memberRepository.findByMemberNum(memberNum), MemberDTO.class);
    }
}
