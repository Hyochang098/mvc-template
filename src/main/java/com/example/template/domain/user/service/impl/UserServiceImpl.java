package com.example.template.domain.user.service.impl;

import com.example.template.domain.user.dto.UserResponseDto;
import com.example.template.domain.user.entity.User;
import com.example.template.domain.user.repository.UserRepository;
import com.example.template.domain.user.service.UserService;
import com.example.template.global.common.exception.ApiException;
import com.example.template.global.common.exception.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDto findMe(Long userId) {
        log.info("[UserService] 본인 정보 조회");
        User user= userRepository.findById(userId).orElseThrow(()->{
            log.warn("[UserService] 본인 정보 조회 실패 - 유저 정보 없음");
            return ApiException.of(HttpStatus.NOT_FOUND, ErrorMessage.USER_NOT_FOUND);
        });
        log.info("[UserService] 본인 정보 조회 완료");
        return UserResponseDto.from(user);
    }


}
