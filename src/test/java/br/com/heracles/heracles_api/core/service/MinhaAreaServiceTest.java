package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MinhaAreaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TreinoRepository treinoRepository;

    private MinhaAreaService service;
    private Usuario marina;
    private Usuario bruno;

    @BeforeEach
    void preparar() {
        service = new MinhaAreaService(usuarioRepository, treinoRepository);

        marina = new Usuario();
        marina.setId(10L);
        marina.setNome("Marina Alves");
        marina.setEmail("marina@ex.com");

        bruno = new Usuario();
        bruno.setId(20L);
        bruno.setNome("Bruno Dias");
        bruno.setEmail("bruno@ex.com");

        given(usuarioRepository.findByEmailIgnoreCase("marina@ex.com")).willReturn(Optional.of(marina));
        given(usuarioRepository.findByEmailIgnoreCase("bruno@ex.com")).willReturn(Optional.of(bruno));
    }

    @Test
    @DisplayName("Consulta as fichas do id de quem o e-mail do token identifica")
    void consultaPeloDonoDoToken() {
        ArgumentCaptor<Long> alunoId = ArgumentCaptor.forClass(Long.class);
        given(treinoRepository.fichasDoAluno(alunoId.capture())).willReturn(List.of());

        service.minhasFichas("marina@ex.com");

        // O id nunca vem da requisicao: e resolvido a partir do token. Sem
        // isso, bastaria trocar um numero para ler a ficha de outro aluno.
        assertThat(alunoId.getValue()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Dois alunos autenticados recebem cada um as suas fichas")
    void cadaUmVeAsSuas() {
        Treino dela = fichaCom("Ficha A - Peito e Triceps");
        Treino dele = fichaCom("Circuito Funcional");
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of(dela));
        given(treinoRepository.fichasDoAluno(20L)).willReturn(List.of(dele));

        assertThat(service.minhasFichas("marina@ex.com"))
                .extracting(TreinoResponse::nome).containsExactly("Ficha A - Peito e Triceps");
        assertThat(service.minhasFichas("bruno@ex.com"))
                .extracting(TreinoResponse::nome).containsExactly("Circuito Funcional");
    }

    @Test
    @DisplayName("Aluno sem ficha vinculada recebe lista vazia, nao erro")
    void semFichaNaoEhErro() {
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of());

        // Nao ter ficha e um estado normal — o aluno acabou de se matricular
        // e o professor ainda nao montou. A tela diz isso; a API nao estoura.
        assertThat(service.minhasFichas("marina@ex.com")).isEmpty();
    }

    @Test
    @DisplayName("A ficha vem com a prescricao completa dos exercicios")
    void fichaVemComPrescricao() {
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of(fichaCom("Ficha A")));

        TreinoResponse ficha = service.minhasFichas("marina@ex.com").get(0);

        assertThat(ficha.exercicios()).hasSize(1);
        assertThat(ficha.exercicios().get(0).nome()).isEqualTo("Supino reto");
        assertThat(ficha.exercicios().get(0).series()).isEqualTo(4);
        assertThat(ficha.exercicios().get(0).repeticoesMin()).isEqualTo(10);
        assertThat(ficha.exercicios().get(0).repeticoesMax()).isEqualTo(12);
        // 4 series x 10 repeticoes minimas.
        assertThat(ficha.volumePrescritoMinimo()).isEqualTo(40);
    }

    @Test
    @DisplayName("Token de usuario que sumiu da base nao vira 500")
    void tokenOrfaoNaoEstoura() {
        assertThatThrownBy(() -> service.minhasFichas("fantasma@ex.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Treino fichaCom(String nome) {
        Treino treino = new Treino();
        treino.setId(1L);
        treino.setNome(nome);
        treino.setFoco("Hipertrofia");
        treino.setNivel("Intermediario");

        Exercicio exercicio = new Exercicio();
        exercicio.setId(1L);
        exercicio.setNome("Supino reto");
        exercicio.setSeries(4);
        exercicio.setRepeticoesMin(10);
        exercicio.setRepeticoesMax(12);
        exercicio.setCarga("70% 1RM");
        exercicio.setOrdem(0);
        treino.adicionarExercicio(exercicio);

        return treino;
    }
}
