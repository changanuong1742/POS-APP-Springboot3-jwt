package com.pos.app.services;

import com.pos.app.dto.request.VerificationCodeRequest;
import com.pos.app.dto.response.ResponseMessage;
import com.pos.app.dto.response.UserResponse;
import com.pos.app.models.User;
import com.pos.app.models.VerificationCode;
import com.pos.app.repositories.UserRepository;
import com.pos.app.repositories.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
@RequiredArgsConstructor
public class VerificationCodeService {
    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    ResponseMessage responseMessage = new ResponseMessage();
    ModelMapper modelMapper = new ModelMapper();

    @Autowired
    private JavaMailSender mailSender;

    public ResponseEntity<?> sendCode(VerificationCodeRequest verificationCodeRequest) {

        Optional<User> optionalUser = userRepository.findById(verificationCodeRequest.getUserid());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            Random random = new Random();
            // Tạo số nguyên ngẫu nhiên trong phạm vi từ 1000 đến 9999
            int randomNumber = random.nextInt(9000) + 1000;


            var verificationCode = VerificationCode.builder()
                    .userId(verificationCodeRequest.getUserid())
                    .type(verificationCodeRequest.getType())
                    .code(String.valueOf(randomNumber))
                    .build();

            verificationCodeRepository.save(verificationCode);

            sendSimpleEmail(user.getEmail(), "Code change mail", String.valueOf(randomNumber));

            responseMessage.setMessages(Collections.singletonList("Success"));
            responseMessage.setSucceeded(true);
            return new ResponseEntity<>(responseMessage, HttpStatus.OK);
        } else {
            responseMessage.setMessages(Collections.singletonList("Fail"));
            responseMessage.setSucceeded(false);
            return new ResponseEntity<>(responseMessage, HttpStatus.NOT_FOUND);
        }
    }


    public void sendSimpleEmail(String toEmail,
                                String subject,
                                String body
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("fromemail@gmail.com");
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);
        mailSender.send(message);
        System.out.println("Mail Send...");
    }

}