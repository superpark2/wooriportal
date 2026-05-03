package com.park.welstory.wooriportal.config.Security;

import com.park.welstory.wooriportal.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        return new CustomUserDetails(memberRepository.findByMemberId(memberId).orElseThrow(()-> new UsernameNotFoundException("맴버가 없습니다.")));
    }

}
