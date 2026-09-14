package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.AuthDtos;
import br.com.heracles.heracles_api.core.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacaoService service;

    public AuthController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@RequestBody @Valid AuthDtos.LoginRequest request) {
        return service.autenticar(request);
    }
}
