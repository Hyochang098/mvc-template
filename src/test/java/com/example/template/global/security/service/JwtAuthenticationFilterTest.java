package com.example.template.global.security.service;

import com.example.template.domain.user.entity.User;
import com.example.template.domain.user.repository.UserRepository;
import com.example.template.global.common.entity.Role;
import com.example.template.global.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;
  @Mock
  private UserRepository userRepository;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    ReflectionTestUtils.setField(filter, "checkUserStateWithDb", true);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("DB 검증 활성화 시 사용자 없으면 인증 컨텍스트를 비운다")
  void doFilter_skipsAuthentication_whenUserNotFound() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    given(jwtTokenProvider.validateToken("token")).willReturn(true);
    given(jwtTokenProvider.getUserIdFromToken("token")).willReturn(99L);
    given(jwtTokenProvider.getEmailFromToken("token")).willReturn("missing@test.com");
    given(jwtTokenProvider.getRoleFromToken("token")).willReturn("GENERAL");
    given(userRepository.findById(99L)).willReturn(Optional.empty());

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("DB 검증 활성화 시 DB 역할을 우선 적용한다")
  void doFilter_prefersDatabaseRole_whenUserExists() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    User user = User.builder()
        .userId(1L)
        .email("user@test.com")
        .password("pw")
        .name("사용자")
        .role(Role.ADMIN)
        .build();

    given(jwtTokenProvider.validateToken("token")).willReturn(true);
    given(jwtTokenProvider.getUserIdFromToken("token")).willReturn(1L);
    given(jwtTokenProvider.getEmailFromToken("token")).willReturn("user@test.com");
    given(jwtTokenProvider.getRoleFromToken("token")).willReturn("GENERAL");
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    filter.doFilterInternal(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);
    assertThat(authentication.getAuthorities())
        .extracting(auth -> auth.getAuthority())
        .containsExactly("ROLE_ADMIN");
  }
}

