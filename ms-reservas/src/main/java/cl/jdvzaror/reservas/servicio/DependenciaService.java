package cl.jdvzaror.reservas.servicio;

import cl.jdvzaror.reservas.dto.DependenciaDatos;
import cl.jdvzaror.reservas.dto.DependenciaDto;
import cl.jdvzaror.reservas.modelo.Dependencia;
import cl.jdvzaror.reservas.repositorio.DependenciaRepository;
import cl.jdvzaror.reservas.seguridad.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DependenciaService {

    private final DependenciaRepository dependencias;

    public DependenciaService(DependenciaRepository dependencias) {
        this.dependencias = dependencias;
    }

    @Transactional(readOnly = true)
    public List<DependenciaDto> listarActivas() {
        return dependencias.findByActivaTrueOrderByIdAsc().stream().map(DependenciaService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DependenciaDto> listarTodas() {
        return dependencias.findAll().stream().map(DependenciaService::aDto).toList();
    }

    @Transactional
    public DependenciaDto crear(DependenciaDatos datos) {
        Dependencia d = new Dependencia();
        aplicar(d, datos);
        return aDto(dependencias.save(d));
    }

    @Transactional
    public DependenciaDto actualizar(Long id, DependenciaDatos datos) {
        Dependencia d = dependencias.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Dependencia no encontrada"));
        aplicar(d, datos);
        return aDto(dependencias.save(d));
    }

    private void aplicar(Dependencia d, DependenciaDatos datos) {
        d.setNombre(datos.nombre().trim());
        d.setDescripcion(datos.descripcion());
        d.setUbicacion(datos.ubicacion());
        d.setCapacidadReferencial(datos.capacidadReferencial());
        d.setActiva(datos.activa() == null || datos.activa());
    }

    static DependenciaDto aDto(Dependencia d) {
        return new DependenciaDto(d.getId(), d.getNombre(), d.getDescripcion(), d.getUbicacion(),
                d.getCapacidadReferencial(), d.isActiva());
    }
}
