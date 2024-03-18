package com.pos.app.dto.response;

import com.pos.app.models.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserResponse {
    Long id;
    String firstname;
    String lastname;
    String email;
    List<Image> images;
    String error;
}
