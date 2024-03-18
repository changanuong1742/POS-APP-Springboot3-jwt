package com.pos.app.controllers;

import com.pos.app.dto.request.VerificationCodeRequest;
import com.pos.app.services.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/verification-code")
@RequiredArgsConstructor
public class VerificationCodeController {
    private final VerificationCodeService verificationCodeService;


    @PostMapping
    public ResponseEntity<?> sendMail(@ModelAttribute VerificationCodeRequest verificationCodeRequest) {

        return ResponseEntity.ok(verificationCodeService.sendCode(verificationCodeRequest));
    }
}
