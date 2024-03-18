package com.pos.app.services;

import com.pos.app.dto.request.ChangePasswordRequest;
import com.pos.app.dto.request.UserRequest;
import com.pos.app.dto.response.ResponseMessage;
import com.pos.app.dto.response.UserResponse;
import com.pos.app.models.Image;
import com.pos.app.models.User;
import com.pos.app.models.VerificationCode;
import com.pos.app.repositories.ImageRepository;
import com.pos.app.repositories.UserRepository;
import com.pos.app.repositories.VerificationCodeRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;

    ModelMapper modelMapper = new ModelMapper();
    @Value("${minio.bucketName}")
    private String bucketName;
    private final MinioClient minioClient;

    public UserResponse getUser(Integer id) {
        User user = userRepository.findFirstById(id);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        return userResponse;
    }

    private VerificationCode getFirstRecordLastByUser(Integer id) {
        return verificationCodeRepository.findLastFirstByUser(id);
    }

    private boolean isTimeOutCode(Integer id) {
        Instant currentInstant = Instant.now();
        Duration duration = Duration.between(verificationCodeRepository.findLastFirstByUser(id).getCreated_at(), currentInstant);

        if (duration.toMinutes() > 10) {
            return true;
        }
        return false;
    }

    public ResponseEntity<?> updateProfile(UserRequest request) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String emailLogged = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<User> optionalUser = userRepository.findByEmail(emailLogged);
        ResponseMessage responseMessage = new ResponseMessage();

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                responseMessage.setMessages(Collections.singletonList("Email is already in use"));
                responseMessage.setSucceeded(false);
                return new ResponseEntity<>(responseMessage, HttpStatus.CONFLICT);
            } else if (!user.getEmail().equals(request.getEmail()) && request.getCodeVerification() == null) {
                responseMessage.setMessages(Collections.singletonList("Request authentication code"));
                responseMessage.setSucceeded(false);
                return new ResponseEntity<>(responseMessage, HttpStatus.CONFLICT);
            } else if (!Objects.equals(request.getCodeVerification(), getFirstRecordLastByUser(user.getId()).getCode())) {
                responseMessage.setMessages(Collections.singletonList("Incorrect code"));
                responseMessage.setSucceeded(false);
                return new ResponseEntity<>(responseMessage, HttpStatus.NOT_FOUND);
            } else if (isTimeOutCode(user.getId())) {
                responseMessage.setMessages(Collections.singletonList("The verification code time has passed"));
                responseMessage.setSucceeded(false);
                return new ResponseEntity<>(responseMessage, HttpStatus.NOT_FOUND);
            }


            // Sửa thông tin người dùng
            user.setEmail(request.getEmail());
            user.setFirstname(request.getFirstname());
            user.setLastname(request.getLastname());

            // Nếu có ảnh mới được cung cấp, thực hiện việc lưu ảnh mới và cập nhật thông tin ảnh
            if (request.getFile() != null) {
                try {
                    String originalFilename = Instant.now().toEpochMilli() + "-" + request.getFile().getOriginalFilename();
                    String[] filenameParts = originalFilename.split("\\.");
                    String fileType = filenameParts[filenameParts.length - 1];

                    InputStream inputStream = request.getFile().getInputStream();
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(originalFilename)
                            .stream(inputStream, inputStream.available(), -1)
                            .build());

                    Image avatar = Image.builder().type(fileType).fileName(originalFilename).build();
                    user.getImages().clear(); // Xóa hết ảnh cũ (nếu có)
                    user.getImages().add(avatar); // Thêm ảnh mới

                    // Lưu ảnh mới vào imageRepository
                    imageRepository.save(avatar);
                } catch (IOException | InvalidKeyException | ErrorResponseException | InvalidResponseException |
                         NoSuchAlgorithmException | XmlParserException | InternalException e) {
                    e.printStackTrace(); // Xử lý lỗi tùy ý
                    responseMessage.setMessages(Collections.singletonList("Error"));
                    responseMessage.setSucceeded(false);
                    return new ResponseEntity<>(responseMessage, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else if (request.isRemovedImage()) {
                user.getImages().clear(); // Xóa hết ảnh cũ (nếu có)
            }

            // Lưu thông tin người dùng đã cập nhật
            userRepository.save(user);
            responseMessage.setMessages(Collections.singletonList("Success"));
            responseMessage.setSucceeded(true);
            return new ResponseEntity<>(responseMessage, HttpStatus.OK);
        } else {
            responseMessage.setMessages(Collections.singletonList("Error"));
            responseMessage.setSucceeded(false);
            return new ResponseEntity<>(responseMessage, HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<?> ChangePassword(ChangePasswordRequest request) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String emailLogged = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<User> optionalUser = userRepository.findByEmail(emailLogged);
        ResponseMessage responseMessage = new ResponseMessage();

        if (optionalUser.isPresent()){
            User user = optionalUser.get();

            if (!passwordEncoder.matches(request.getPasswordOld(), user.getPassword())) {
                responseMessage.setMessages(Collections.singletonList("The old password is incorrect!"));
                responseMessage.setSucceeded(false);
                return new ResponseEntity<>(responseMessage, HttpStatus.NOT_FOUND);
            }

            responseMessage.setMessages(Collections.singletonList("Password changed successfully"));
            responseMessage.setSucceeded(true);
            user.setPassword(passwordEncoder.encode(request.getPasswordNew()));
            userRepository.save(user);

        }
        return new ResponseEntity<>(responseMessage, HttpStatus.OK);
    }
}
