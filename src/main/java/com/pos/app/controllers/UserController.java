package com.pos.app.controllers;

import com.pos.app.dto.request.ChangePasswordRequest;
import com.pos.app.dto.request.UserRequest;
import com.pos.app.dto.response.UserResponse;
import com.pos.app.services.UserService;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Integer id) {
        try {
            UserResponse userResponse = userService.getUser(id);
            return userResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @ModelAttribute UserRequest request
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changedPassword(
            @ModelAttribute ChangePasswordRequest request
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return ResponseEntity.ok(userService.ChangePassword(request));
    }

}
