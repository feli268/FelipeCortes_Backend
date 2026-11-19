package cl.jdvzaror.reservas.configuracion;

import cl.jdvzaror.reservas.modelo.Dependencia;
import cl.jdvzaror.reservas.repositorio.DependenciaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Carga las 3 dependencias de la junta cuando la tabla esta vacia. */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final DependenciaRepository dependencias;

    public DatosIniciales(DependenciaRepository dependencias) {
        this.dependencias = dependencias;
    }

    @Override
    public void run(String... args) {
        if (dependencias.count() == 0) {
            crear("Sede social", "Salon principal para reuniones y celebraciones", 80);
            crear("Sala multiuso", "Sala para talleres y reuniones pequenas", 30);
            crear("Cancha comunitaria", "Cancha techada para actividades deportivas", 40);
        }
    }

    private void crear(String nombre, String descripcion, int capacidad) {
        Dependencia d = new Dependencia();
        d.setNombre(nombre);
        d.setDescripcion(descripcion);
        d.setUbicacion("Los Jazmines 425, Cerrillos");
        d.setCapacidadReferencial(capacidad);
        d.setActiva(true);
        dependencias.save(d);
    }
}
