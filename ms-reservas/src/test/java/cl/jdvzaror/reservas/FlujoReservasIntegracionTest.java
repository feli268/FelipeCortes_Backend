package cl.jdvzaror.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.jdvzaror.reservas.seguridad.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FlujoReservasIntegracionTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokens;

    private String bearer(Long id, String rol) {
        return "Bearer " + tokens.emitir(id, rol.toLowerCase() + "@jdvzaror.cl", rol);
    }

    private String json(String horaInicio, String horaFin) {
        return "{\"dependenciaId\":1,\"fecha\":\"2999-03-15\",\"horaInicio\":\"" + horaInicio
                + "\",\"horaFin\":\"" + horaFin + "\",\"motivo\":\"Celebracion familiar\"}";
    }

    @Test
    void sinTokenSeRechaza() throws Exception {
        mvc.perform(get("/api/dependencias")).andExpect(status().isUnauthorized());
    }

    @Test
    void elCatalogoTraeLasTresDependencias() throws Exception {
        mvc.perform(get("/api/dependencias").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void unVecinoNoPuedeVerTodasLasSolicitudesNiAprobar() throws Exception {
        mvc.perform(get("/api/reservas").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/reservas/1/aprobar").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void laDirectivaNoPuedeSolicitarReservas() throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", bearer(20L, "DIRECTIVA"))
                        .contentType(MediaType.APPLICATION_JSON).content(json("10:00", "12:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSePuedenAprobarDosReservasQueSeSuperponen() throws Exception {
        // dos vecinos piden horarios que se cruzan: ambas quedan pendientes
        String cuerpoA = mvc.perform(post("/api/reservas").header("Authorization", bearer(10L, "VECINO"))
                        .contentType(MediaType.APPLICATION_JSON).content(json("10:00", "12:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        String cuerpoB = mvc.perform(post("/api/reservas").header("Authorization", bearer(11L, "VECINO"))
                        .contentType(MediaType.APPLICATION_JSON).content(json("11:00", "13:00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long idA = ((Number) JsonPath.read(cuerpoA, "$.id")).longValue();
        long idB = ((Number) JsonPath.read(cuerpoB, "$.id")).longValue();

        // la directiva aprueba la primera
        mvc.perform(put("/api/reservas/" + idA + "/aprobar").header("Authorization", bearer(20L, "DIRECTIVA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        // la segunda ya no se puede aprobar: el servidor valida la disponibilidad
        mvc.perform(put("/api/reservas/" + idB + "/aprobar").header("Authorization", bearer(20L, "DIRECTIVA")))
                .andExpect(status().isConflict());

        // y un tercer vecino ya no puede solicitar ese horario
        mvc.perform(post("/api/reservas").header("Authorization", bearer(12L, "VECINO"))
                        .contentType(MediaType.APPLICATION_JSON).content(json("10:30", "11:30")))
                .andExpect(status().isConflict());

        // la disponibilidad solo muestra los bloques, sin datos del solicitante
        mvc.perform(get("/api/reservas/disponibilidad?dependenciaId=1&fecha=2999-03-15")
                        .header("Authorization", bearer(12L, "VECINO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist());

        // el historial de la reserva aprobada tiene 2 cambios: creada y aprobada
        mvc.perform(get("/api/reservas/" + idA + "/historial").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void horaDeTerminoAnteriorDa400() throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", bearer(10L, "VECINO"))
                        .contentType(MediaType.APPLICATION_JSON).content(json("15:00", "14:00")))
                .andExpect(status().isBadRequest());
    }
}
