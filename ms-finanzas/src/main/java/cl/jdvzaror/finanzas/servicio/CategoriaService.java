package cl.jdvzaror.finanzas.servicio;

import cl.jdvzaror.finanzas.dto.CategoriaDatos;
import cl.jdvzaror.finanzas.dto.CategoriaDto;
import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.repositorio.CategoriaRepository;
import cl.jdvzaror.finanzas.seguridad.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;

    public CategoriaService(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listarActivas(TipoMovimiento tipo) {
        List<CategoriaMovimiento> lista = tipo == null
                ? categorias.findByActivaTrueOrderByTipoAscNombreAsc()
                : categorias.findByActivaTrueAndTipoOrderByNombreAsc(tipo);
        return lista.stream().map(CategoriaService::aDto).toList();
    }

    @Transactional
    public CategoriaDto crear(CategoriaDatos datos) {
        if (categorias.findByNombreIgnoreCase(datos.nombre().trim()).isPresent()) {
            throw ApiException.conflicto("Ya existe una categoria con ese nombre");
        }
        CategoriaMovimiento c = new CategoriaMovimiento();
        aplicar(c, datos);
        return aDto(categorias.save(c));
    }

    @Transactional
    public CategoriaDto actualizar(Long id, CategoriaDatos datos) {
        CategoriaMovimiento c = categorias.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Categoria no encontrada"));
        aplicar(c, datos);
        return aDto(categorias.save(c));
    }

    private void aplicar(CategoriaMovimiento c, CategoriaDatos datos) {
        c.setNombre(datos.nombre().trim());
        c.setTipo(datos.tipo());
        c.setDescripcion(datos.descripcion());
        c.setActiva(datos.activa() == null || datos.activa());
    }

    static CategoriaDto aDto(CategoriaMovimiento c) {
        return new CategoriaDto(c.getId(), c.getNombre(), c.getTipo().name(), c.getDescripcion(), c.isActiva());
    }
}
