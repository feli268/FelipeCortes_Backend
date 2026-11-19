package cl.jdvzaror.finanzas.configuracion;

import cl.jdvzaror.finanzas.modelo.CategoriaMovimiento;
import cl.jdvzaror.finanzas.modelo.TipoMovimiento;
import cl.jdvzaror.finanzas.repositorio.CategoriaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Carga las categorias de ingresos y gastos cuando la tabla esta vacia. */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final CategoriaRepository categorias;

    public DatosIniciales(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @Override
    public void run(String... args) {
        if (categorias.count() == 0) {
            crear("Arriendo", TipoMovimiento.INGRESO, "Pagos informativos de arriendos de dependencias");
            crear("Consumo basico", TipoMovimiento.INGRESO, "Aportes por consumos basicos de la sede");
            crear("Otros ingresos", TipoMovimiento.INGRESO, "Ingresos no clasificados");
            crear("Consumo electrico", TipoMovimiento.GASTO, "Cuenta de electricidad de la sede");
            crear("Agua", TipoMovimiento.GASTO, "Cuenta de agua de la sede");
            crear("Gas", TipoMovimiento.GASTO, "Cuenta de gas de la sede");
            crear("Mantencion", TipoMovimiento.GASTO, "Reparaciones y mantencion de la sede");
            crear("Otros gastos", TipoMovimiento.GASTO, "Gastos no clasificados");
        }
    }

    private void crear(String nombre, TipoMovimiento tipo, String descripcion) {
        CategoriaMovimiento c = new CategoriaMovimiento();
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setDescripcion(descripcion);
        c.setActiva(true);
        categorias.save(c);
    }
}
