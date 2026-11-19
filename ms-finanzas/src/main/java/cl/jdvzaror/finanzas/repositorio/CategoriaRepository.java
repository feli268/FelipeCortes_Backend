package cl.jdvzaror.finanzas.repositorio;

import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaMovimiento, Long> {

    List<CategoriaMovimiento> findByActivaTrueOrderByTipoAscNombreAsc();

    List<CategoriaMovimiento> findByActivaTrueAndTipoOrderByNombreAsc(TipoMovimiento tipo);

    Optional<CategoriaMovimiento> findByNombreIgnoreCase(String nombre);
}
