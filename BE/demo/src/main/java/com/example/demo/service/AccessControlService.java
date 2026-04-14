package com.example.demo.service;

import com.example.demo.mapper.UserMapper;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.security.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserMapper userMapper;

    public AppUserPrincipal currentUser() {
        return SecurityUtils.requirePrincipal();
    }

    public Long currentUserId() {
        return currentUser().getId();
    }

    public boolean isGlobalAdmin() {
        return SecurityUtils.isGlobalAdmin(currentUser());
    }

    public void assertGlobalAdmin() {
        if (!isGlobalAdmin()) {
            throw new AccessDeniedException("Global admin role required.");
        }
    }

    public void assertFactoryAccess(String layoutId) {
        if (isGlobalAdmin()) {
            return;
        }

        Long userId = currentUserId();
        if (!userMapper.hasFactoryAccess(userId, layoutId)) {
            throw new AccessDeniedException("Factory access denied.");
        }
    }

    public List<String> listAllowedFactoryIds() {
        return userMapper.listUserFactoryIds(currentUserId());
    }
}

