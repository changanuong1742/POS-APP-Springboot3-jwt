package com.pos.app.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "verification_codes")
public class VerificationCode {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userId;
    private String type;
    private String code;
    private Instant created_at;

    // Sử dụng @PrePersist để tự động cập nhật created_at trước khi thêm mới
    @PrePersist
    public void prePersist() {
        this.created_at = Instant.now();
    }
}
