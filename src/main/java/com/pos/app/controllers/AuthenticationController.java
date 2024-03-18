package com.pos.app.controllers;

import com.pos.app.dto.request.AuthenticationRequest;
import com.pos.app.dto.request.RegisterRequest;
import com.pos.app.dto.response.AuthenticationResponse;
import com.pos.app.services.AuthenticationService;
import io.minio.errors.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @PostMapping("/register")
  public ResponseEntity<?> register(
          @ModelAttribute RegisterRequest request
          ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    return ResponseEntity.ok(authenticationService.register(request));
  }
  @PostMapping("/authenticate")
  public ResponseEntity<?> authenticate(
      @RequestBody AuthenticationRequest request
  ) {
    return ResponseEntity.ok(authenticationService.authenticate(request));
  }

  @PostMapping("/refresh-token")
  public void refreshToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    authenticationService.refreshToken(request, response);
  }
}
