package com.park.welstory.wooriportal.config.Security;

import com.park.welstory.wooriportal.member.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final MemberEntity member;

    public CustomUserDetails(MemberEntity member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(member.getMemberRole()));
    }

    @Override
    public String getPassword() {
        return member.getMemberPassword();
    }

    @Override
    public String getUsername() {
        return member.getMemberId();
    }

    public String getMemberId() {
        return member.getMemberId();
    }

    public String getMemberProfile() {
        return member.getMemberPictureMeta();
    }

    public String getMemberComment() {
        return member.getMemberComment();
    }

    public String getMemberName() {
        return member.getMemberName();
    }

    public Long getMemberNum() {return member.getMemberNum();}

    public String getMemberRole() {
        String role = member.getMemberRole();

        return switch (role) {
            case "admin" -> "관리자";
            case "user" -> "사용자";
            default -> "알 수 없음";
        };
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
