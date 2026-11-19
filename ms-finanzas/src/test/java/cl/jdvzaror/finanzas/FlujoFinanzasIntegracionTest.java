package cl.jdvzaror.finanzas;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.jdvzaror.finanzas.seguridad.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FlujoFinanzasIntegracionTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    TokenService tokens;

    private String bearer(Long id, String rol) {
        return "Bearer " + tokens.emitir(id, rol.toLowerCase() + "@jdvzaror.cl", rol);
    }

    private long primeraCategoria(String tipo) throws Exception {
        String cuerpo = mvc.perform(get("/api/categorias?tipo=" + tipo).header("Authorization", bearer(30L, "TESORERIA")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$[0].id")).longValue();
    }

    private long registrar(String tipo, long categoriaId, long monto) throws Exception {
        String json = "{\"tipo\":\"" + tipo + "\",\"categoriaId\":" + categoriaId
                + ",\"descripcion\":\"Movimiento de prueba\",\"monto\":" + monto
                + ",\"fechaMovimiento\":\"2026-11-10\"}";
        String cuerpo = mvc.perform(post("/api/movimientos").header("Authorization", bearer(30L, "TESORERIA"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    @Test
    void sinTokenSeRechaza() throws Exception {
        mvc.perform(get("/api/informes/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void unVecinoNoVeLosMovimientosNiPuedeRegistrar() throws Exception {
        mvc.perform(get("/api/movimientos").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/movimientos").header("Authorization", bearer(10L, "VECINO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void montoCeroOCategoriaDeOtroTipoDa400() throws Exception {
        long ingreso = primeraCategoria("INGRESO");
        mvc.perform(post("/api/movimientos").header("Authorization", bearer(30L, "TESORERIA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"INGRESO\",\"categoriaId\":" + ingreso
                                + ",\"descripcion\":\"x\",\"monto\":0,\"fechaMovimiento\":\"2026-11-10\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/movimientos").header("Authorization", bearer(30L, "TESORERIA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"GASTO\",\"categoriaId\":" + ingreso
                                + ",\"descripcion\":\"x\",\"monto\":1000,\"fechaMovimiento\":\"2026-11-10\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void losAnuladosNoCuentanEnElResumenQueVenLosVecinos() throws Exception {
        long ingreso = registrar("INGRESO", primeraCategoria("INGRESO"), 100000);
        long gasto = registrar("GASTO", primeraCategoria("GASTO"), 30000);

        mvc.perform(get("/api/informes/resumen?periodo=2026-11").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresos").value(100000))
                .andExpect(jsonPath("$.gastos").value(30000))
                .andExpect(jsonPath("$.saldo").value(70000));

        mvc.perform(put("/api/movimientos/" + gasto + "/anular").header("Authorization", bearer(30L, "TESORERIA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));

        // anulado: ya no se cuenta, y el resumen de vecinos no trae descripciones ni comprobantes
        mvc.perform(get("/api/informes/resumen?periodo=2026-11").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gastos").value(0))
                .andExpect(jsonPath("$.saldo").value(100000))
                .andExpect(content().string(not(containsString("descripcion"))));

        // anular dos veces da conflicto
        mvc.perform(put("/api/movimientos/" + gasto + "/anular").header("Authorization", bearer(30L, "TESORERIA")))
                .andExpect(status().isConflict());

        // el detalle solo lo ven tesoreria y directiva
        mvc.perform(get("/api/informes/detalle?periodo=2026-11").header("Authorization", bearer(10L, "VECINO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/informes/detalle?periodo=2026-11").header("Authorization", bearer(20L, "DIRECTIVA")))
                .andExpect(status().isOk());

        // un movimiento anulado no recibe comprobantes
        mvc.perform(post("/api/movimientos/" + gasto + "/comprobantes")
                        .header("Authorization", bearer(30L, "TESORERIA")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreArchivo\":\"boleta.jpg\",\"tipoArchivo\":\"image/jpeg\",\"rutaReferencia\":\"/f/boleta.jpg\"}"))
                .andExpect(status().isConflict());

        // uno vigente si
        mvc.perform(post("/api/movimientos/" + ingreso + "/comprobantes")
                        .header("Authorization", bearer(30L, "TESORERIA")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreArchivo\":\"boleta.jpg\",\"tipoArchivo\":\"image/jpeg\",\"rutaReferencia\":\"/f/boleta.jpg\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void unaReservaSoloAdmiteUnPagoVigente() throws Exception {
        String pago = "{\"reservaId\":777,\"monto\":60000,\"fechaPago\":\"2026-12-01\",\"medioInformado\":\"Transferencia\"}";

        mvc.perform(post("/api/pagos").header("Authorization", bearer(30L, "TESORERIA"))
                        .contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("REGISTRADO"));
        mvc.perform(post("/api/pagos").header("Authorization", bearer(30L, "TESORERIA"))
                        .contentType(MediaType.APPLICATION_JSON).content(pago))
                .andExpect(status().isConflict());
    }
}
