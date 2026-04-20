package com.lenyan.lendada.model.dto.user;

import lombok.Data;

@Data
public class UserResetPasswordRequest {

    private String userAccount;

    private String oldPassword;

    private String newPassword;
}