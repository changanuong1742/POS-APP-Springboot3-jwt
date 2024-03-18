package com.pos.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private MultipartFile file;
    private boolean removedImage;
    private String codeVerification;
}
