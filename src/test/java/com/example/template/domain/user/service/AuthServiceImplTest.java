package com.example.template.domain.user.service;

import com.example.template.domain.refreshtoken.entity.RefreshToken;
import com.example.template.domain.refreshtoken.repository.RefreshTokenRepository;
import com.example.template.domain.refreshtoken.service.RefreshTokenCacheService;
import com.example.template.domain.user.dto.LoginRequestDto;
import com.example.template.domain.user.dto.SignUpRequestDto;
import com.example.template.domain.user.entity.User;
import com.example.template.domain.user.repository.UserRepository;
import com.example.template.domain.user.service.impl.AuthServiceImpl;
import com.example.template.global.common.entity.Role;
import com.example.template.global.common.exception.ApiException;
import com.example.template.global.common.exception.ErrorMessage;
import com.example.template.global.security.service.AccessTokenBlacklistService;
import com.example.template.global.security.service.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenCacheService refreshTokenCacheService;
    @Mock private AccessTokenBlacklistService accessTokenBlacklistService;
    @Mock private PasswordEncoder passwordEncoder;

    @BeforeEach
    void init() {
        lenient().when(refreshTokenCacheService.get(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("signUp - 새로운 이메일이면 암호화 후 GENERAL 권한으로 저장한다")
    void signUp_savesWithEncodedPasswordAndGeneralRole_whenEmailIsNew() {
        // given: 중복 이메일 없음, 비밀번호 암호화 준비
        SignUpRequestDto request = new SignUpRequestDto(" New@Test.com ", "Password123!", "홍길동");
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("Password123!")).willReturn("encodedPw");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User arg = invocation.getArgument(0);
            return User.builder()
                    .userId(1L)
                    .email(arg.getEmail())
                    .password(arg.getPassword())
                    .name(arg.getName())
                    .role(arg.getRole())
                    .build();
        });

        // when: 회원가입 수행
        authService.signUp(request);

        // then: 저장 시 암호화된 비밀번호와 GENERAL 권한이 설정되어야 함
        verify(userRepository).save(argThat(saved ->
                saved.getEmail().equals("new@test.com")
                        && saved.getPassword().equals("encodedPw")
                        && saved.getRole() == Role.GENERAL
        ));
    }

    @Test
    @DisplayName("signUp - 중복 이메일이면 ApiException(CONFLICT) 발생하고 저장하지 않는다")
    void signUp_throwsConflict_whenEmailExists() {
        // given: 중복 이메일
        SignUpRequestDto request = new SignUpRequestDto("dup@test.com", "Password123!", "홍길동");
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        // when & then: 예외 확인 및 save 미호출 검증
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessage.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - 인증 성공 시 토큰을 생성하고 기존 리프레시 토큰을 갱신한다")
    void login_updatesExistingRefreshToken_whenAuthenticationSucceeds() {
        // given: 인증 성공, 사용자 조회 가능, 기존 리프레시 토큰 존재
        LoginRequestDto request = new LoginRequestDto("user@test.com", "Password123!");
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .password("encoded")
                .name("사용자")
                .role(Role.GENERAL)
                .build();
        RefreshToken stored = RefreshToken.builder()
                .refreshTokenId(10L)
                .userId(1L)
                .tokenHash("oldHash")
                .expiresAt(LocalDateTime.now())
                .build();

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(stored));
        given(jwtTokenProvider.createAccessToken(1L, "user@test.com", "GENERAL")).willReturn("newAccess");
        given(jwtTokenProvider.createRefreshToken(1L, "user@test.com", "GENERAL")).willReturn("newRefresh");
        given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(604800L);
        given(passwordEncoder.encode("newRefresh")).willReturn("hashedNewRefresh");

        // when: 로그인 수행
        authService.login(request);

        // then: 저장된 토큰이 갱신되고 새 토큰 저장 호출이 없는지 확인
        assertThat(stored.getTokenHash()).isEqualTo("hashedNewRefresh");
        assertThat(stored.getExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(1));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(refreshTokenCacheService).store(eq(1L), eq("hashedNewRefresh"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("login - 이메일을 정규화(trim+lowercase)한 값으로 인증과 조회를 진행한다")
    void login_normalizesEmailBeforeAuthentication() {
        // given
        LoginRequestDto request = new LoginRequestDto("  USER@Test.com  ", "Password123!");
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .password("encoded")
                .name("사용자")
                .role(Role.GENERAL)
                .build();

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(1L, "user@test.com", "GENERAL")).willReturn("access");
        given(jwtTokenProvider.createRefreshToken(1L, "user@test.com", "GENERAL")).willReturn("refresh");
        given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(604800L);
        given(passwordEncoder.encode("refresh")).willReturn("hashedRefresh");
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        // when
        authService.login(request);

        // then
        verify(authenticationManager).authenticate(
                argThat(token -> token.getPrincipal().equals("user@test.com"))
        );
        verify(userRepository).findByEmail("user@test.com");
        verify(refreshTokenRepository).save(argThat(saved -> saved.getTokenHash().equals("hashedRefresh")));
        verify(refreshTokenCacheService).store(eq(1L), eq("hashedRefresh"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("login - 인증 실패 시 ApiException(BAD_REQUEST) 발생")
    void login_throwsBadRequest_whenAuthenticationFails() {
        // given: 인증 매니저가 실패
        LoginRequestDto request = new LoginRequestDto("user@test.com", "wrong");
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("bad"));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.")
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @DisplayName("refreshToken - 토큰 검증 실패 시 ApiException(UNAUTHORIZED) 발생")
    void refreshToken_throwsUnauthorized_whenTokenInvalid() {
        // given: 토큰 검증 실패
        given(jwtTokenProvider.validateToken("badToken")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken("badToken"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessage.INVALID_REFRESH_TOKEN)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    @DisplayName("refreshToken - 저장된 토큰 없으면 ApiException(NOT_FOUND) 발생")
    void refreshToken_throwsNotFound_whenStoredTokenMissing() {
        // given: 토큰 검증 성공하지만 저장된 토큰 없음
        given(jwtTokenProvider.validateToken("refresh")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("refresh")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken("refresh"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessage.REFRESH_TOKEN_NOT_FOUND)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    @DisplayName("refreshToken - 저장된 토큰이 만료되면 ApiException(UNAUTHORIZED) 발생")
    void refreshToken_throwsUnauthorized_whenStoredTokenExpired() {
        // given
        RefreshToken stored = RefreshToken.builder()
                .refreshTokenId(10L)
                .userId(1L)
                .tokenHash("storedHash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        given(jwtTokenProvider.validateToken("refresh")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("refresh")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(stored));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken("refresh"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessage.INVALID_REFRESH_TOKEN)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    @DisplayName("refreshToken - 저장 토큰 불일치 시 ApiException(UNAUTHORIZED) 발생")
    void refreshToken_throwsUnauthorized_whenTokenMismatch() {
        // given: 저장 토큰과 입력 토큰 다름
        RefreshToken stored = RefreshToken.builder()
                .refreshTokenId(10L)
                .userId(1L)
                .tokenHash("otherHash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        given(jwtTokenProvider.validateToken("input")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("input")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(stored));
        given(passwordEncoder.matches("input", "otherHash")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken("input"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessage.INVALID_REFRESH_TOKEN)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    @DisplayName("refreshToken - 정상 흐름이면 새 토큰을 생성하고 저장 토큰을 갱신한다")
    void refreshToken_updatesStoredToken_whenValid() {
        // given: 토큰 검증 및 저장 토큰 일치
        RefreshToken stored = RefreshToken.builder()
                .refreshTokenId(10L)
                .userId(1L)
                .tokenHash("storedHash")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        given(jwtTokenProvider.validateToken("refresh")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("refresh")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(stored));
        given(jwtTokenProvider.getEmailFromToken("refresh")).willReturn("user@test.com");
        given(jwtTokenProvider.getRoleFromToken("refresh")).willReturn("GENERAL");
        given(jwtTokenProvider.createAccessToken(1L, "user@test.com", "GENERAL")).willReturn("newAccess");
        given(jwtTokenProvider.createRefreshToken(1L, "user@test.com", "GENERAL")).willReturn("newRefresh");
        given(jwtTokenProvider.getRefreshTokenValidityInSeconds()).willReturn(604800L);
        given(passwordEncoder.matches("refresh", "storedHash")).willReturn(true);
        given(passwordEncoder.encode("newRefresh")).willReturn("newHashedRefresh");

        // when
        authService.refreshToken("refresh");

        // then
        assertThat(stored.getTokenHash()).isEqualTo("newHashedRefresh");
        assertThat(stored.getExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(1));
        verify(refreshTokenRepository).save(stored);
        verify(refreshTokenCacheService).store(eq(1L), eq("newHashedRefresh"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("logout - userId로 리프레시 토큰을 삭제한다")
    void logout_deletesRefreshTokenByUserId() {
        // given: 삭제 대상 userId
        Long userId = 1L;
        given(jwtTokenProvider.getRemainingValidityInSeconds("access")).willReturn(120L);

        // when
        authService.logout(userId, "access");

        // then
        verify(refreshTokenRepository).deleteByUserId(userId);
        verify(refreshTokenCacheService).evict(userId);
        verify(accessTokenBlacklistService).blacklist("access", 120L);
    }

    @Test
    @DisplayName("isEmailAvailable - 존재 여부의 반전을 반환한다")
    void isEmailAvailable_returnsFalseWhenExistsAndTrueWhenNotExists() {
        // given: 이메일 존재 여부 목킹 (소문자/trim 변환 포함)
        given(userRepository.existsByEmail("used@test.com")).willReturn(true);
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);

        // when & then
        assertThat(authService.isEmailAvailable("  used@test.com ")).isFalse();
        assertThat(authService.isEmailAvailable("NEW@test.com")).isTrue(); // toLowerCase 적용 기대
    }

    @Test
    @DisplayName("isEmailAvailable - 이메일이 비어있으면 ApiException(BAD_REQUEST) 발생")
    void isEmailAvailable_throwsBadRequestWhenEmailBlank() {
        // when & then
        assertThatThrownBy(() -> authService.isEmailAvailable(" "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("이메일은 필수 입력 값입니다.")
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }
}
