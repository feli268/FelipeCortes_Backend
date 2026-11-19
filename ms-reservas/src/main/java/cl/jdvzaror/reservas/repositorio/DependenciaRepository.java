package cl.jdvzaror.reservas.repositorio;

import cl.jdvzaror.reservas.modelo.Dependencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenciaRepository extends JpaRepository<Dependencia, Long> {

    List<Dependencia> findByActivaTrueOrderByIdAsc();
}
