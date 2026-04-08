package com.team2.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * JWT 의 {@code role} 클레임을 Spring Security 의 {@code ROLE_*} GrantedAuthority 로 변환.
 *
 * backend-auth 는 JWT payload 에 {@code "role": "ADMIN"} 같은 단일 문자열 클레임을 발급한다.
 * 역할 값은 대소문자 구분 없이 uppercase 로 정규화 후 ROLE_ 접두사를 붙인다.
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object roleClaim = jwt.getClaim("role");
        if (roleClaim == null) {
            return Collections.emptyList();
        }
        String role = roleClaim.toString().toUpperCase();
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
