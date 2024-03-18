package com.pos.app.repositories;

import com.pos.app.models.Image;
import com.pos.app.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {

}
