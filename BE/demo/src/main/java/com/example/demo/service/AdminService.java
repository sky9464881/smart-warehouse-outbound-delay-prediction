package com.example.demo.service;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.FactorySummaryResponse;
import com.example.demo.dto.UserEntity;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.WarehouseMapper;
import com.example.demo.security.UserRole;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final WarehouseMapper warehouseMapper;

    public List<AdminUserResponse> listUsers() {
        List<UserEntity> users = userMapper.listUsers();
        List<AdminUserResponse> responses = new ArrayList<>(users.size());

        for (UserEntity user : users) {
            AdminUserResponse response = new AdminUserResponse();
            response.setId(user.getId());
            response.setEmail(user.getEmail());
            response.setDisplayName(user.getDisplayName());
            response.setRole(user.getRole());

            if (UserRole.FACTORY_ADMIN.name().equals(user.getRole())) {
                response.setFactoryIds(userMapper.listUserFactoryIds(user.getId()));
            } else {
                response.setFactoryIds(List.of());
            }

            responses.add(response);
        }

        return responses;
    }

    public AdminUserResponse updateUserFactories(Long userId, List<String> factoryIds) {
        UserEntity user = userMapper.findUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found.");
        }

        if (UserRole.GLOBAL_ADMIN.name().equals(user.getRole())) {
            throw new BadRequestException("Cannot change factory access for a global admin user.");
        }

        List<String> normalizedFactoryIds = normalizeFactoryIds(factoryIds);
        validateFactoriesExist(normalizedFactoryIds);

        userMapper.deleteUserFactoryAccess(userId);
        if (!normalizedFactoryIds.isEmpty()) {
            userMapper.insertUserFactoryAccessBulk(userId, normalizedFactoryIds);
        }

        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setRole(user.getRole());
        response.setFactoryIds(normalizedFactoryIds);
        return response;
    }

    private List<String> normalizeFactoryIds(List<String> factoryIds) {
        if (factoryIds == null) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String entry : factoryIds) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                unique.add(trimmed);
            }
        }
        return new ArrayList<>(unique);
    }

    private void validateFactoriesExist(List<String> factoryIds) {
        if (factoryIds.isEmpty()) {
            return;
        }

        List<FactorySummaryResponse> factories = warehouseMapper.getFactoriesByIds(factoryIds);
        Set<String> existing = new LinkedHashSet<>();
        for (FactorySummaryResponse factory : factories) {
            existing.add(factory.getLayoutId());
        }

        List<String> missing = factoryIds.stream()
                .filter(id -> !existing.contains(id))
                .toList();

        if (!missing.isEmpty()) {
            throw new BadRequestException("Unknown factory IDs: " + String.join(", ", missing));
        }
    }
}

