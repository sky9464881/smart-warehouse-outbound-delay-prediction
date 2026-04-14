package com.example.demo.controller;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.UpdateUserFactoriesRequest;
import com.example.demo.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @PutMapping("/users/{userId}/factories")
    public AdminUserResponse updateUserFactories(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserFactoriesRequest request
    ) {
        return adminService.updateUserFactories(userId, request.getFactoryIds());
    }
}

