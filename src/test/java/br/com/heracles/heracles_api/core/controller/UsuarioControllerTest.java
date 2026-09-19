package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.service.UsuarioService;
import br.com.heracles.heracles_api.exception.GlobalExceptionHandler;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Instanciado direto: o slice @WebMvcTest nao publica um ObjectMapper
    // autowireable, e aqui ele serve so para montar o corpo das requisicoes.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UsuarioService service;

    /**
     * O slice roda com addFilters = false, entao nenhum filtro popula o
     * contexto — e `@AuthenticationPrincipal Jwt` chegaria nulo ao
     * controller. O resolver le do SecurityContextHolder, entao e ali que
     * a autenticacao entra.
     */
    @BeforeEach
    void autenticarComoRecepcao() {
        Jwt token = Jwt.withTokenValue("token-de-teste")
                .header("alg", "HS256")
                .subject("recepcao@heracles.com.br")
                .claim("roles", List.of("SECRETARIA"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioResponse alunoSalvo() {
        return new UsuarioResponse(1L, "Maria Silva", "12345678901", "maria@email.com",
                "(11) 99999-9999", "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20),
                false, 10L, "Plano Mensal", true, TipoPerfil.ALUNO, StatusUsuario.ATIVO,
                LocalDateTime.now(), List.of());
    }

    private Map<String, Object> cadastroValido() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Maria Silva");
        corpo.put("cpf", "123.456.789-01");
        corpo.put("email", "maria@email.com");
        corpo.put("telefone", "(11) 99999-9999");
        corpo.put("tipoPerfil", "ALUNO");
        corpo.put("senha", "SenhaForte123");
        return corpo;
    }

    @Test
    @DisplayName("A resposta de usuario nunca carrega a senha")
    void respostaNaoExpoeSenha() throws Exception {
        // Antes: a entidade era serializada direta e o hash saia em toda listagem.
        given(service.criar(any(), any(), any())).willReturn(alunoSalvo());

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastroValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    @DisplayName("Campos nao declarados no DTO de edicao sao ignorados no bind")
    void edicaoNaoAceitaCamposPrivilegiados() throws Exception {
        given(service.atualizar(eq(1L), any())).willReturn(alunoSalvo());

        // Um cliente malicioso tenta promover a si mesmo junto com a edicao.
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Maria Silva");
        corpo.put("cpf", "12345678901");
        corpo.put("email", "maria@email.com");
        corpo.put("telefone", "(11) 99999-9999");
        corpo.put("tipoPerfil", "ADMIN");
        corpo.put("status", "ATIVO");
        corpo.put("id", 99);

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpo)))
                .andExpect(status().isOk());

        // O DTO Atualizar nao tem esses componentes, entao nada disso chega ao servico.
        ArgumentCaptor<UsuarioRequests.Atualizar> captor =
                ArgumentCaptor.forClass(UsuarioRequests.Atualizar.class);
        verify(service).atualizar(eq(1L), captor.capture());
        assertThat(captor.getValue().nome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("E-mail invalido e senha curta viram 400 com detalhe por campo")
    void validacaoDeCadastro() throws Exception {
        Map<String, Object> corpo = cadastroValido();
        corpo.put("email", "nao-e-um-email");
        corpo.put("senha", "123");
        corpo.put("cpf", "abc");

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").exists())
                .andExpect(jsonPath("$.erros.cpf").exists());
    }

    @Test
    @DisplayName("CPF duplicado devolve 409, nao 500 vindo do banco")
    void cpfDuplicadoDevolve409() throws Exception {
        given(service.criar(any(), any(), any()))
                .willThrow(new RegraNegocioException("Ja existe um cadastro com o CPF informado."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastroValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ja existe um cadastro com o CPF informado."));
    }

    @Test
    @DisplayName("Vincular treinos aceita lista vazia para desvincular tudo")
    void vincularTreinosAceitaListaVazia() throws Exception {
        given(service.sincronizarTreinos(eq(1L), any())).willReturn(alunoSalvo());

        mockMvc.perform(put("/api/usuarios/1/treinos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"treinosIds\":[]}"))
                .andExpect(status().isOk());
    }
}
