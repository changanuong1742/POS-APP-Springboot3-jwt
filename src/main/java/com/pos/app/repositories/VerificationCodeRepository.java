package com.pos.app.repositories;

import com.pos.app.models.Token;
import com.pos.app.models.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Integer> {

    @Query("SELECT v FROM VerificationCode v WHERE v.userId = :id ORDER BY v.id DESC LIMIT 1")
    VerificationCode findLastFirstByUser(@Param("id") Integer id);
}
