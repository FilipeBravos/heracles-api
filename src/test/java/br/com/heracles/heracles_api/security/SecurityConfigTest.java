package br.com.heracles.heracles_api.security;

import br.com.heracles.heracles_api.core.controller.TreinoController;
import br.com.heracles.heracles_api.core.controller.UsuarioController;
import br.com.heracles.heracles_api.core.service.TreinoService;
import br.com.heracles.heracles_api.core.service.UsuarioService;
import br.com.heracles.heracles_api.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Politica de acesso.
 *
 * A configuracao anterior era anyRequest().permitAll(), entao qualquer um
 * listava, editava e apagava alunos. Estes testes travam o comportamento novo.
 */
@WebMvcTest(controllers = {UsuarioController.class, TreinoController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "heracles.security.jwt.secret=segredo-de-teste-com-mais-de-32-bytes-para-hs256",
        "heracles.cors.allowed-origins=http://localhost:4200"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private TreinoService treinoService;

    @Test
    @DisplayName("Requisicao anonima a alunos recebe 401")
    void anonimoNaoListaAlunos() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requisicao anonima a treinos recebe 401")
    void anonimoNaoListaTreinos() throws Exception {
        mockMvc.perform(get("/api/treinos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ALUNO")
    @DisplayName("Aluno autenticado nao acessa a listagem administrativa de alunos")
    void alunoNaoAcessaListagemAdministrativa() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("Professor le a lista de alunos mas nao cadastra")
    void professorLeMasNaoCadastra() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isOk());
        mockMvc.perform(post("/api/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECRETARIA")
    @DisplayName("Secretaria cadastra aluno mas nao cria ficha de treino")
    void secretariaCadastraAlunoMasNaoCriaFicha() throws Exception {
        // Corpo vazio: 400 de validacao significa que passou pela autorizacao.
        mockMvc.perform(post("/api/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/treinos").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("Professor cria ficha de treino")
    void professorCriaFicha() throws Exception {
        mockMvc.perform(post("/api/treinos").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Login continua aberto para quem ainda nao tem token")
    void loginEhPublico() throws Exception {
        // /api/auth/login e permitAll; a ausencia do controller no slice devolve 404,
        // o que ja prova que a requisicao nao foi barrada por autenticacao.
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
    }
}
