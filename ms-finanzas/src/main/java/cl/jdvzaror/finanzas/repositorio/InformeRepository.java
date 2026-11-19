package cl.jdvzaror.finanzas.repositorio;

import cl.jdvzaror.finanzas.modelo.InformeFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InformeRepository extends JpaRepository<InformeFinanciero, Long> {
}
