package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.DashboardResumoResponse;
import br.com.heracles.heracles_api.core.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public DashboardResumoResponse resumo() {
        return service.resumo();
    }
}
