package com.example.template.domain.user.service;

import com.example.template.domain.user.dto.UserResponseDto;
import com.example.template.domain.user.entity.User;
import com.example.template.domain.user.repository.UserRepository;
import com.example.template.domain.user.service.impl.UserServiceImpl;
import com.example.template.global.common.entity.Role;
import com.example.template.global.common.exception.ApiException;
import com.example.template.global.common.exception.ErrorMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("findMe - 인증된 유저ID면 DTO로 변환해 반환한다")
    void findMe_returnsDto_whenUserExists() {
        // given: 조회 가능한 사용자
        User user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .password("encoded")
                .name("사용자")
                .role(Role.GENERAL)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        UserResponseDto result = userService.findMe(1L);

        // then: 필드 매핑 확인
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.name()).isEqualTo("사용자");
        assertThat(result.role()).isEqualTo(Role.GENERAL);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("findMe - 유저가 없으면 ApiException(NOT_FOUND) 발생")
    void findMe_throwsNotFound_whenUserMissing() {
        // given: 조회 결과 없음
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findMe(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(ErrorMessage.USER_NOT_FOUND);
    }
}

