package com.example.template.domain.user.service;

import com.example.template.domain.user.dto.UserResponseDto;

public interface UserService {

    /**
     *  본인 정보 조회
     */
    UserResponseDto findMe(Long userId);
}
