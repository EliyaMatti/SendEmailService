package com.mailSender.common.security;

import java.util.UUID;

public record AuthPrincipal(UUID userId, UUID organizationId, String email, String role) {}
