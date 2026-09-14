package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.service.TreinoService;
import br.com.heracles.heracles_api.exception.GlobalExceptionHandler;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TreinoController.class)
@AutoConfigureMockMvc(addFilters = false) // regras de acesso sao cobertas em SecurityConfigTest
@Import(GlobalExceptionHandler.class)
class TreinoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Instanciado direto: o slice @WebMvcTest nao publica um ObjectMapper
    // autowireable, e aqui ele serve so para montar o corpo das requisicoes.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TreinoService service;

    private Map<String, Object> fichaValida() {
        return Map.of(
                "nome", "Ficha A - Peito e Triceps",
                "foco", "Hipertrofia",
                "nivel", "Intermediario",
                "exercicios", List.of(Map.of(
                        "nome", "Supino Reto com Barra",
                        "series", 4,
                        "repeticoesMin", 10,
                        "repeticoesMax", 12,
                        "carga", "Ate a falha na ultima serie",
                        "observacoes", "Descanso de 60s"))
        );
    }

    @Test
    @DisplayName("POST /api/treinos esta mapeado e devolve 201 com Location")
    void criarTreinoEstaRoteado() throws Exception {
        // Regressao do defeito original: salvarTreino nao tinha @PostMapping,
        // entao esta rota respondia 405 e criar ficha era impossivel.
        given(service.criar(any())).willReturn(
                new TreinoResponse(7L, "Ficha A - Peito e Triceps", "Hipertrofia", "Intermediario", List.of(), 0));

        mockMvc.perform(post("/api/treinos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fichaValida())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/treinos/7")))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("Ficha sem exercicios e rejeitada com 400 e mensagem por campo")
    void fichaSemExerciciosEhRejeitada() throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "Ficha vazia", "foco", "Hipertrofia", "nivel", "Iniciante",
                "exercicios", List.of()));

        mockMvc.perform(post("/api/treinos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"))
                .andExpect(jsonPath("$.erros.exercicios").exists());
    }

    @Test
    @DisplayName("Campos obrigatorios em branco viram 400, nao 500")
    void camposObrigatoriosEmBranco() throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "", "foco", "", "nivel", "",
                "exercicios", List.of(Map.of(
                        "nome", "X", "series", 3, "repeticoesMin", 10, "repeticoesMax", 10))));

        mockMvc.perform(post("/api/treinos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").exists())
                .andExpect(jsonPath("$.erros.foco").exists())
                .andExpect(jsonPath("$.erros.nivel").exists());
    }

    @Test
    @DisplayName("Faixa de repeticoes invertida e rejeitada com 400")
    void faixaDeRepeticoesInvertida() throws Exception {
        // 4x12 a 10 nao existe: o maximo nao pode ser menor que o minimo.
        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "Ficha A", "foco", "Hipertrofia", "nivel", "Iniciante",
                "exercicios", List.of(Map.of(
                        "nome", "Supino Reto", "series", 4,
                        "repeticoesMin", 12, "repeticoesMax", 10))));

        mockMvc.perform(post("/api/treinos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"));
    }

    @Test
    @DisplayName("Series fora do limite e rejeitada com 400")
    void seriesForaDoLimite() throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "Ficha A", "foco", "Hipertrofia", "nivel", "Iniciante",
                "exercicios", List.of(Map.of(
                        "nome", "Supino Reto", "series", 99,
                        "repeticoesMin", 10, "repeticoesMax", 10))));

        mockMvc.perform(post("/api/treinos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").exists());
    }

    @Test
    @DisplayName("Treino inexistente devolve 404, nao 500")
    void treinoInexistenteDevolve404() throws Exception {
        // Antes: RuntimeException sem handler, que chegava ao cliente como 500.
        given(service.buscarPorId(999L)).willThrow(RecursoNaoEncontradoException.de("Treino", 999L));

        mockMvc.perform(get("/api/treinos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    @Test
    @DisplayName("DELETE devolve 204 e propaga 404 quando a ficha nao existe")
    void deletarFicha() throws Exception {
        mockMvc.perform(delete("/api/treinos/1")).andExpect(status().isNoContent());

        willThrow(RecursoNaoEncontradoException.de("Treino", 42L)).given(service).deletar(42L);
        mockMvc.perform(delete("/api/treinos/42")).andExpect(status().isNotFound());
    }
}
