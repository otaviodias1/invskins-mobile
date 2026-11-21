package br.uel.invskins.service;

import br.uel.invskins.model.Inventario;
import br.uel.invskins.model.ItemInventario;
import br.uel.invskins.model.Skin;
import br.uel.invskins.repository.InventarioRepository;
import br.uel.invskins.repository.ItemInventarioRepository;
import br.uel.invskins.repository.SkinRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Validated
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ItemInventarioRepository itemInventarioRepository;
    private final SkinRepository skinRepository;
    private final SkinService skinService;

    public InventarioService(InventarioRepository inventarioRepository,
                              ItemInventarioRepository itemInventarioRepository,
                              SkinRepository skinRepository,
                              SkinService skinService) {
        this.inventarioRepository = inventarioRepository;
        this.itemInventarioRepository = itemInventarioRepository;
        this.skinRepository = skinRepository;
        this.skinService = skinService;
    }

    @Transactional(readOnly = true)
    public List<Inventario> listar() {
        return inventarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Inventario> buscarPorId(Long id) {
        return inventarioRepository.findById(id);
    }

    public Inventario criar(@NotNull @Valid Inventario dadosRecebidos) {
        // Monta um Inventario novo a partir só do nome/descrição — ignora id/itens
        // que o cliente eventualmente tenha mandado no corpo da requisição.
        Inventario novo = new Inventario(dadosRecebidos.getNome(), dadosRecebidos.getDescricao());
        return inventarioRepository.save(novo);
    }

    @Transactional
    public Optional<Inventario> atualizar(Long id, @NotNull @Valid Inventario dadosRecebidos) {
        return inventarioRepository.findByIdForUpdate(id).map(inventario -> {
            inventario.setNome(dadosRecebidos.getNome());
            inventario.setDescricao(dadosRecebidos.getDescricao());
            return inventarioRepository.save(inventario);
        });
    }

    @Transactional
    public boolean excluir(Long id) {
        return inventarioRepository.findByIdForUpdate(id).map(inventario -> {
            inventarioRepository.delete(inventario);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<Inventario> adicionarItem(Long inventarioId,
            @NotBlank(message = "A skin é obrigatória") String skinExternalId,
            @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade) {
        // Serialize mutations of an inventory before reading its items.
        Optional<Inventario> inventarioOpt = inventarioRepository.findByIdForUpdate(inventarioId);
        if (inventarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Skin skin = obterOuCriarSkin(skinExternalId);
        int qtd = quantidade != null ? quantidade : 1;

        Optional<ItemInventario> existente =
                itemInventarioRepository.findByInventarioIdAndSkinId(inventarioId, skin.getId());

        if (existente.isPresent()) {
            ItemInventario item = existente.get();
            if (item.getQuantidade() > Integer.MAX_VALUE - qtd) {
                throw new IllegalArgumentException("A quantidade total excede o limite permitido");
            }
            item.setQuantidade(item.getQuantidade() + qtd);
            itemInventarioRepository.save(item);
        } else {
            Inventario inventario = inventarioOpt.get();
            ItemInventario item = new ItemInventario(inventario, skin, qtd);
            inventario.adicionarItem(item);
            itemInventarioRepository.save(item);
        }

        return inventarioOpt;
    }

    @Transactional
    public Optional<Inventario> atualizarQuantidade(Long inventarioId, Long itemId,
            @NotNull(message = "A quantidade é obrigatória")
            @Positive(message = "A quantidade deve ser maior que zero") Integer quantidade) {
        Optional<Inventario> inventario = inventarioRepository.findByIdForUpdate(inventarioId);
        if (inventario.isEmpty()) {
            return Optional.empty();
        }
        return itemInventarioRepository.findByIdAndInventarioId(itemId, inventarioId)
                .map(item -> {
                    item.setQuantidade(quantidade);
                    itemInventarioRepository.save(item);
                    return inventario.get();
                });
    }

    @Transactional
    public boolean removerItem(Long inventarioId, Long itemId) {
        Optional<Inventario> inventario = inventarioRepository.findByIdForUpdate(inventarioId);
        if (inventario.isEmpty()) {
            return false;
        }
        return itemInventarioRepository.findByIdAndInventarioId(itemId, inventarioId)
                .map(item -> {
                    inventario.get().removerItem(item);
                    return true;
                })
                .orElse(false);
    }

    private Skin obterOuCriarSkin(String externalId) {

        Skin doCatalogo = skinService.buscarPorExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Skin não encontrada no catálogo: " + externalId));

        skinRepository.salvarDoCatalogo(
                doCatalogo.getExternalId(),
                doCatalogo.getNome(),
                doCatalogo.getArma(),
                doCatalogo.getRaridade(),
                doCatalogo.getImagem(),
                doCatalogo.getPreco()
        );

        return skinRepository.findByExternalId(externalId).orElseThrow();
    }
}
