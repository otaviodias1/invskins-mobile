package br.uel.invskins.repository;

import br.uel.invskins.model.Inventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventario from Inventario inventario where inventario.id = :id")
    Optional<Inventario> findByIdForUpdate(Long id);
}
