package com.example.demo.mapper;

import com.example.demo.dto.AuthTokenEntity;
import com.example.demo.dto.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    boolean existsGlobalAdmin();

    UserEntity findUserByEmail(@Param("email") String email);

    UserEntity findUserById(@Param("id") Long id);

    void insertUser(UserEntity user);

    AuthTokenEntity findToken(@Param("token") String token);

    void insertToken(
            @Param("token") String token,
            @Param("userId") Long userId,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    void deleteToken(@Param("token") String token);

    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    List<String> listUserFactoryIds(@Param("userId") Long userId);

    boolean hasFactoryAccess(
            @Param("userId") Long userId,
            @Param("layoutId") String layoutId
    );

    void deleteUserFactoryAccess(@Param("userId") Long userId);

    void insertUserFactoryAccessBulk(
            @Param("userId") Long userId,
            @Param("layoutIds") List<String> layoutIds
    );

    List<UserEntity> listUsers();
}

