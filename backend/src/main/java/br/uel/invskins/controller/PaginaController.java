package br.uel.invskins.controller;

import br.uel.invskins.model.Inventario;
import br.uel.invskins.service.InventarioService;
import br.uel.invskins.service.SkinService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Controller
public class PaginaController {

    private final InventarioService inventarioService;
    private final SkinService skinService;

    public PaginaController(
            InventarioService inventarioService,
            SkinService skinService) {
        this.inventarioService = inventarioService;
        this.skinService = skinService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("inventarios", inventarioService.listar());
        return "index";
    }

    @GetMapping("/inventarios")
    public String listarInventarios(
            @RequestParam(required = false) Long editarId,
            Model model) {

        model.addAttribute("inventarios", inventarioService.listar());
        if (editarId != null) {
            inventarioService.buscarPorId(editarId)
                    .ifPresent(inv -> model.addAttribute("inventarioEdicao", inv));
        }

        return "inventarios";
    }


    @PostMapping("/inventarios")
    public String criarInventario(@RequestParam String nome,
                                  @RequestParam(required = false) String descricao) {
        Inventario novo = new Inventario();
        novo.setNome(nome);
        novo.setDescricao(descricao);
        inventarioService.criar(novo);

        return "redirect:/inventarios";
    }

    // 2. ROTA ADICIONADA: Editar um inventário existente via formulário HTML
    @PostMapping("/inventarios/{id}/editar")
    public String editarInventario(@PathVariable Long id,
                                   @RequestParam String nome,
                                   @RequestParam(required = false) String descricao) {
        Inventario dadosAtualizados = new Inventario();
        dadosAtualizados.setNome(nome);
        dadosAtualizados.setDescricao(descricao);

        inventarioService.atualizar(id, dadosAtualizados)
                .orElseThrow(() -> new NoSuchElementException("Inventário não encontrado"));

        return "redirect:/inventarios";
    }

    // 3. ROTA ADICIONADA: Excluir um inventário via formulário HTML
    @PostMapping("/inventarios/{id}/deletar")
    public String excluirInventario(@PathVariable Long id) {
        if (!inventarioService.excluir(id)) {
            throw new NoSuchElementException("Inventário não encontrado");
        }
        return "redirect:/inventarios";
    }

    @GetMapping("/inventarios/{id}")
    public String detalheInventario(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String nome,
            Model model) {

        return inventarioService.buscarPorId(id)
                .map(inventario -> {
                    model.addAttribute("inventario", inventario);
                    model.addAttribute("skinsBusca", skinService.buscarSkins(nome));
                    model.addAttribute("termoBusca", nome);
                    return "inventario-detalhe";
                })
                .orElse("redirect:/inventarios");
    }

    @PostMapping("/inventarios/{id}/itens")
    public String adicionarItem(
            @PathVariable Long id,
            @RequestParam String skinExternalId,
            @RequestParam(defaultValue = "1") Integer quantidade) {

        inventarioService.adicionarItem(id, skinExternalId, quantidade)
                .orElseThrow(() -> new NoSuchElementException("Inventário não encontrado"));
        return "redirect:/inventarios/" + id;
    }

    // 4. ROTA ADICIONADA: Adicionar item diretamente a partir da página do Catálogo
    @PostMapping("/inventarios/adicionar-item")
    public String adicionarItemPeloCatalogo(
            @RequestParam Long inventarioId,
            @RequestParam String skinExternalId,
            @RequestParam(defaultValue = "1") Integer quantidade) {

        inventarioService.adicionarItem(inventarioId, skinExternalId, quantidade)
                .orElseThrow(() -> new NoSuchElementException("Inventário não encontrado"));
        return "redirect:/inventarios/" + inventarioId;
    }

    @PostMapping("/inventarios/{id}/itens/{itemId}/remover")
    public String removerItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {

        if (!inventarioService.removerItem(id, itemId)) {
            throw new NoSuchElementException("Item não encontrado neste inventário");
        }
        return "redirect:/inventarios/" + id;
    }

    @GetMapping("/skins")
    public String catalogo(
            @RequestParam(defaultValue = "") String nome,
            Model model) {

        model.addAttribute("skins", skinService.buscarSkins(nome));
        model.addAttribute("termo", nome);
        model.addAttribute("inventarios", inventarioService.listar());

        return "catalogo";
    }
}
