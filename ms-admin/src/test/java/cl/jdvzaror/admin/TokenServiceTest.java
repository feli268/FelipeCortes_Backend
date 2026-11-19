package cl.jdvzaror.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.jdvzaror.admin.seguridad.Sesion;
import cl.jdvzaror.admin.seguridad.TokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static final String SECRETO = "secreto-de-prueba-0123456789-abcdefghij";
    private final Clock ahora = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void tokenValidoDevuelveLaSesion() {
        TokenService tokens = new TokenService(SECRETO, 60, ahora);
        String token = tokens.emitir(7L, "directiva@jdvzaror.cl", "DIRECTIVA");

        Optional<Sesion> sesion = tokens.validar(token);

        assertTrue(sesion.isPresent());
        assertEquals(7L, sesion.get().usuarioId());
        assertEquals("DIRECTIVA", sesion.get().rol());
        assertEquals("directiva@jdvzaror.cl", sesion.get().email());
    }

    @Test
    void tokenAlteradoSeRechaza() {
        TokenService tokens = new TokenService(SECRETO, 60, ahora);
        String token = tokens.emitir(7L, "vecino@jdvzaror.cl", "VECINO");
        // se cambia el primer caracter del cuerpo para intentar subir de perfil
        String falso = (token.charAt(0) == 'A' ? "B" : "A") + token.substring(1);

        assertTrue(tokens.validar(falso).isEmpty());
    }

    @Test
    void tokenFirmadoConOtroSecretoSeRechaza() {
        String token = new TokenService("otro-secreto-0123456789-abcdefghijklmn", 60, ahora)
                .emitir(7L, "vecino@jdvzaror.cl", "VECINO");

        assertTrue(new TokenService(SECRETO, 60, ahora).validar(token).isEmpty());
    }

    @Test
    void tokenExpiradoSeRechaza() {
        String token = new TokenService(SECRETO, 60, ahora).emitir(7L, "vecino@jdvzaror.cl", "VECINO");
        Clock dosHorasDespues = Clock.offset(ahora, Duration.ofHours(2));

        assertTrue(new TokenService(SECRETO, 60, dosHorasDespues).validar(token).isEmpty());
    }

    @Test
    void textoBasuraSeRechazaSinFallar() {
        TokenService tokens = new TokenService(SECRETO, 60, ahora);

        assertTrue(tokens.validar(null).isEmpty());
        assertTrue(tokens.validar("").isEmpty());
        assertTrue(tokens.validar("abc").isEmpty());
        assertTrue(tokens.validar("a.b.c").isEmpty());
    }
}
