package com.tex.cloud_task_manager.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRegisteredMessage(
    UUID messageId,
    String traceId,
    Long userId,
    String name,
    String email,
    String accountType,
    LocalDateTime registeredAt) {}
