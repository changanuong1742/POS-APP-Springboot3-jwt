package com.pos.app.controllers;

import com.pos.app.models.Image;
import com.pos.app.repositories.ImageRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    @Value("${minio.bucketName}")

    private String bucketName;
    private final MinioClient minioClient;

    @Autowired
    private ImageRepository imageRepository;

    @PostMapping
    public ResponseEntity<?> saveImage(@RequestParam String name, @RequestParam String type, @RequestPart("file") MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        InputStream inputStream = file.getInputStream();
       minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(file.getOriginalFilename())
                .stream(inputStream, inputStream.available(), -1)
                .build());

//        Image image = Image.builder().name(name).type(type).fileByte(file.getBytes()).imagePath(imagePath).build();
//        imageRepository.save(image);
//        image.setFileByte(null);
//        return ResponseEntity.ok(image);
        return ResponseEntity.ok(file.getOriginalFilename());
    }

    @GetMapping
    public ResponseEntity<List<Image>> getListImage() {
        List<Image> imageList = imageRepository.findAll();
        return ResponseEntity.ok(imageList);
    }
}
