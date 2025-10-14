package com.park.welstory.wooriportal.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/signup")
    public String signup(MemberDTO membbrDTO, HttpServletRequest request) {


        memberService.addMember(membbrDTO);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(membbrDTO.getMemberId(), membbrDTO.getMemberPassword());
        Authentication auth = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 세션에 인증정보 저장 (로그인 유지)
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        return "login";
    }

    @PostMapping("/check-id")
    @ResponseBody
    public String idCheck(String memberId) {

        if (memberRepository.findByMemberId(memberId).isEmpty()) {
            return "true";
        }else{
            return "false";
        }
    }

    @GetMapping("/member/mgmt")
    public String memberMgmt(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<MemberEntity> memberPage = memberRepository.findAll(pageable);
        
        model.addAttribute("members", memberPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", memberPage.getTotalPages());
        model.addAttribute("totalElements", memberPage.getTotalElements());
        
        return "common/mgmt";
    }

    @PostMapping("/member/update-role")
    @ResponseBody
    public ResponseEntity<String> updateMemberRole(@RequestParam Long memberNum, @RequestParam String memberRole) {
        try {
            MemberEntity member = memberRepository.findById(memberNum).orElse(null);
            if (member != null) {
                member.setMemberRole(memberRole);
                memberRepository.save(member);
                return ResponseEntity.ok("success");
            }
            return ResponseEntity.badRequest().body("Member not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating role");
        }
    }

}
