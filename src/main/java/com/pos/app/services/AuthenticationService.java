package com.pos.app.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.app.dto.request.AuthenticationRequest;
import com.pos.app.dto.request.RegisterRequest;
import com.pos.app.dto.response.AuthenticationResponse;
import com.pos.app.models.Image;
import com.pos.app.models.Token;
import com.pos.app.models.User;
import com.pos.app.repositories.ImageRepository;
import com.pos.app.repositories.TokenRepository;

import com.pos.app.repositories.UserRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${minio.bucketName}")
    private String bucketName;
    private final MinioClient minioClient;

    public ResponseEntity<?> register(RegisterRequest request) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (userRepository.existsByEmail(request.getEmail())) {
            return new ResponseEntity<>("email is already taken !", HttpStatus.CONFLICT);
        } else {
            String originalFilename = Instant.now().toEpochMilli() + "-" + request.getFile().getOriginalFilename();
            String[] filenameParts = originalFilename.split("\\.");
            String fileType = filenameParts[filenameParts.length - 1];

            InputStream inputStream = request.getFile().getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(originalFilename)
                    .stream(inputStream, inputStream.available(), -1)
                    .build());

            var avatar = Image.builder().type(fileType).fileName(originalFilename).build();
            var user = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword())).images(Collections.singletonList(avatar))
                    .build();

            var savedUser = userRepository.save(user);
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            saveUserToken(savedUser, jwtToken, "token");
            saveUserToken(savedUser, refreshToken, "refreshToken");
            imageRepository.save(avatar);

            return new ResponseEntity<>(AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .build(), HttpStatus.OK);
        }
    }

//    public AuthenticationResponse authenticate(AuthenticationRequest request) {
//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        request.getEmail(),
//                        request.getPassword()
//                )
//        );
//        var user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow();
//        var jwtToken = jwtService.generateToken(user);
//        var refreshToken = jwtService.generateRefreshToken(user);
////    revokeAllUserTokens(user);
//        saveUserToken(user, jwtToken, "token");
//        saveUserToken(user, refreshToken, "refreshToken");
//        return AuthenticationResponse.builder()
//                .accessToken(jwtToken)
//                .refreshToken(refreshToken).userId(String.valueOf(user.getId()))
//                .build();
//    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            var user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow();

            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);

            // Commented out the token revocation for now
            // revokeAllUserTokens(user);

            saveUserToken(user, jwtToken, "token");
            saveUserToken(user, refreshToken, "refreshToken");

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .userId(String.valueOf(user.getId()))
                    .build();
        } catch (AuthenticationException e) {
            return AuthenticationResponse.builder()
                    .error("Wrong email or password")
                    .build();
        }
    }

    private void saveUserToken(User user, String jwtToken, String typeToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .expired(new Date(System.currentTimeMillis() + (Objects.equals(typeToken, "token") ? jwtExpiration : refreshExpiration)))
                .build();
        tokenRepository.save(token);
    }

//  private void revokeAllUserTokens(User user) {
//    var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
//    if (validUserTokens.isEmpty())
//      return;
//    validUserTokens.forEach(token -> {
//      token.setExpired(true);
//    });
//    tokenRepository.saveAll(validUserTokens);
//  }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.userRepository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                var accessRefreshToken = jwtService.generateRefreshToken(user);
                saveUserToken(user, accessToken, "token");
                saveUserToken(user, accessRefreshToken, "refreshToken");
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken).userId(String.valueOf(user.getId()))
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }
}
