package com.tex.cloud_task_manager.User.response_request;

import com.tex.cloud_task_manager.User.UserEntity;

public record UserResponse(
    String name
) {
    public static UserResponse from(UserEntity save) {
        return new UserResponse(
            save.getName()
        );
    }

}
