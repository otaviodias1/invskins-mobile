package br.uel.invskins.repository;

import br.uel.invskins.model.Skin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkinRepository extends JpaRepository<Skin, Long> {

    Optional<Skin> findByExternalId(String externalId);

    // The unique external_id constraint makes concurrent catalog inserts atomic in MySQL.
    @Modifying
    @Query(value = """
            INSERT INTO skins (external_id, nome, arma, raridade, imagem, preco)
            VALUES (:externalId, :nome, :arma, :raridade, :imagem, :preco)
            ON DUPLICATE KEY UPDATE nome = :nome, arma = :arma, raridade = :raridade,
                                    imagem = :imagem, preco = :preco
            """, nativeQuery = true)
    void salvarDoCatalogo(String externalId, String nome, String arma,
                         String raridade, String imagem, Double preco);
}
