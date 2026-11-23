package br.uel.invskins;

import br.uel.invskins.model.Inventario;
import br.uel.invskins.repository.InventarioRepository;
import br.uel.invskins.repository.ItemInventarioRepository;
import br.uel.invskins.repository.SkinRepository;
import br.uel.invskins.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendRegressionTests {
    private static final String SKIN = "ak47-redline-ft";

    @Autowired MockMvc mvc;
    @Autowired InventarioService service;
    @Autowired InventarioRepository inventarios;
    @Autowired ItemInventarioRepository itens;
    @Autowired SkinRepository skins;
    @Autowired JdbcTemplate jdbc;

    private Long inventarioId;

    @BeforeEach
    void prepararInventario() {
        itens.deleteAll();
        inventarios.deleteAll();
        skins.deleteAll();
        inventarioId = service.criar(new Inventario("Teste", "Descricao")).getId();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejeitaQuantidadeInvalidaEmTodasAsRotas(int quantidade) throws Exception {
        service.adicionarItem(inventarioId, SKIN, 2);
        Long itemId = itens.findAll().getFirst().getId();

        mvc.perform(post("/api/inventarios/{id}/itens", inventarioId)
                        .param("skinExternalId", SKIN).param("quantidade", String.valueOf(quantidade)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/inventarios/{id}/itens/{itemId}", inventarioId, itemId)
                        .param("quantidade", String.valueOf(quantidade)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/inventarios/{id}/itens", inventarioId)
                        .param("skinExternalId", SKIN).param("quantidade", String.valueOf(quantidade)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/inventarios/adicionar-item").param("inventarioId", inventarioId.toString())
                        .param("skinExternalId", SKIN).param("quantidade", String.valueOf(quantidade)))
                .andExpect(status().isBadRequest());

        assertEquals(2, itens.findById(itemId).orElseThrow().getQuantidade());
    }

    @Test
    void rejeitaOverflowSemAlterarQuantidade() throws Exception {
        service.adicionarItem(inventarioId, SKIN, Integer.MAX_VALUE);
        mvc.perform(post("/api/inventarios/{id}/itens", inventarioId)
                        .param("skinExternalId", SKIN).param("quantidade", "1"))
                .andExpect(status().isBadRequest());
        assertEquals(Integer.MAX_VALUE, itens.findAll().getFirst().getQuantidade());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejeitaNomeEmBrancoNosFormularios(String nome) throws Exception {
        verificarFormularioInvalido(nome, "Descricao");
    }

    @Test
    void rejeitaCamposLongosNosFormularios() throws Exception {
        verificarFormularioInvalido("n".repeat(101), "Descricao");
        verificarFormularioInvalido("Teste", "d".repeat(256));
    }

    private void verificarFormularioInvalido(String nome, String descricao) throws Exception {
        mvc.perform(post("/inventarios").param("nome", nome).param("descricao", descricao))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/inventarios/{id}/editar", inventarioId)
                        .param("nome", nome).param("descricao", descricao))
                .andExpect(status().isBadRequest());
        assertEquals(1, inventarios.count());
        Inventario original = inventarios.findById(inventarioId).orElseThrow();
        assertEquals("Teste", original.getNome());
        assertEquals("Descricao", original.getDescricao());
    }

    @Test
    void skinDesconhecidaRetorna404NasRotasHtml() throws Exception {
        mvc.perform(post("/inventarios/{id}/itens", inventarioId)
                        .param("skinExternalId", "<script>alert(1)</script>").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
        mvc.perform(post("/inventarios/adicionar-item").param("inventarioId", inventarioId.toString())
                        .param("skinExternalId", "inexistente"))
                .andExpect(status().isNotFound());
        assertEquals(0, itens.count());
    }

    @Test
    void formulariosNaoConfirmamOperacoesSobreInventarioInexistente() throws Exception {
        mvc.perform(post("/inventarios/{id}/editar", -1).param("nome", "Teste"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/inventarios/{id}/deletar", -1)).andExpect(status().isNotFound());
        mvc.perform(post("/inventarios/{id}/itens", -1).param("skinExternalId", SKIN))
                .andExpect(status().isNotFound());
        mvc.perform(post("/inventarios/adicionar-item").param("inventarioId", "-1")
                        .param("skinExternalId", SKIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoRemoveItemDeOutroInventario() throws Exception {
        service.adicionarItem(inventarioId, SKIN, 1);
        Long itemId = itens.findAll().getFirst().getId();
        Long outro = service.criar(new Inventario("Outro", null)).getId();
        mvc.perform(post("/inventarios/{id}/itens/{itemId}/remover", outro, itemId))
                .andExpect(status().isNotFound());
        assertTrue(itens.existsById(itemId));
    }

    @Test
    void adicoesConcorrentesCriamUmaSkinEUmItem() throws Exception {
        executarConcorrentemente(8, index -> service.adicionarItem(inventarioId, SKIN, 1));
        assertEquals(1, skins.count());
        assertEquals(1, itens.count());
        assertEquals(8, itens.findAll().getFirst().getQuantidade());
    }

    @Test
    void adicoesConcorrentesPreservamQuantidadeExistente() throws Exception {
        service.adicionarItem(inventarioId, SKIN, 1);
        executarConcorrentemente(8, index -> service.adicionarItem(inventarioId, SKIN, 1));
        assertEquals(1, itens.count());
        assertEquals(9, itens.findAll().getFirst().getQuantidade());
    }

    @Test
    void inventariosDiferentesCompartilhamSkinCriadaConcorrentemente() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ids.add(service.criar(new Inventario("Inventario " + i, null)).getId());
        }
        executarConcorrentemente(8, index -> service.adicionarItem(ids.get(index), SKIN, 1));
        assertEquals(1, skins.count());
        assertEquals(8, itens.count());
        assertEquals(1, jdbc.queryForObject("select count(distinct skin_id) from itens_inventario", Integer.class));
    }

    @Test
    void operacoesValidasContinuamFuncionando() throws Exception {
        mvc.perform(post("/api/inventarios/{id}/itens", inventarioId).param("skinExternalId", SKIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].quantidade").value(1))
                .andExpect(jsonPath("$.valorTotal").value(35.5));
        Long itemId = itens.findAll().getFirst().getId();
        mvc.perform(put("/api/inventarios/{id}/itens/{itemId}", inventarioId, itemId).param("quantidade", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(106.5));
        mvc.perform(post("/inventarios/{id}/editar", inventarioId).param("nome", "Atualizado"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/inventarios"));
        mvc.perform(delete("/api/inventarios/{id}/itens/{itemId}", inventarioId, itemId))
                .andExpect(status().isNoContent());
        assertEquals(0, itens.count());
        mvc.perform(post("/inventarios/{id}/deletar", inventarioId))
                .andExpect(status().is3xxRedirection());
        assertFalse(inventarios.existsById(inventarioId));
    }

    private void executarConcorrentemente(int quantidade, IntConsumer operacao) throws Exception {
        CountDownLatch prontos = new CountDownLatch(quantidade);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(quantidade)) {
            List<Future<?>> tarefas = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                int index = i;
                tarefas.add(executor.submit(() -> {
                    prontos.countDown();
                    try {
                        if (!inicio.await(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("Tempo limite aguardando inicio");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    operacao.accept(index);
                }));
            }
            try {
                assertTrue(prontos.await(10, TimeUnit.SECONDS));
            } finally {
                inicio.countDown();
            }
            for (Future<?> tarefa : tarefas) {
                tarefa.get(30, TimeUnit.SECONDS);
            }
        }
    }
}
