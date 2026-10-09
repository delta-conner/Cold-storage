package com.coldstorage.oms.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private List<Long> roomIds;
}
