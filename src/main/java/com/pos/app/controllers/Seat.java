package com.pos.app.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class Seat {

    @GetMapping("/long")
    @PreAuthorize("hasAuthority('view book')")
    public String findAllBooks2() {
        return "123";
    }
}
