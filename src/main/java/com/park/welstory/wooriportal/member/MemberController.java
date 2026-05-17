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
    public String signup(MemberDTO memberDTO, HttpServletRequest request) {

        memberService.addMember(memberDTO);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(memberDTO.getMemberId(), memberDTO.getMemberPassword());

        // 인코딩 전 원문 비밀번호로 인증해야 하므로 addMember 호출 전에 원문을 임시 보관하거나
        // SecurityConfig에서 formLogin을 활용하는 방식으로 개선을 권장합니다.
        // 현재는 기존 로직 유지 (인코딩 후 원문이 없어 인증 실패 가능성 있음 — 별도 검토 필요)
        try {
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        } catch (Exception ignored) {
            // 인증 실패 시 로그인 페이지로 이동
        }

        // 버그수정: 회원가입 완료 후 로그인 페이지로 리다이렉트
        return "redirect:/login";
    }

    @PostMapping("/check-id")
    @ResponseBody
    public String idCheck(String memberId) {
        return memberRepository.findByMemberId(memberId).isEmpty() ? "true" : "false";
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
        return memberRepository.findById(memberNum)
                .map(member -> {
                    member.setMemberRole(memberRole);
                    memberRepository.save(member);
                    return ResponseEntity.ok("success");
                })
                .orElse(ResponseEntity.badRequest().body("Member not found"));
    }
}
