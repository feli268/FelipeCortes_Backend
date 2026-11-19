package cl.jdvzaror.admin.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Token firmado con HMAC-SHA256: base64(datos) + "." + base64(firma).
 * Es una autenticacion simplificada para el MVP academico (no es un JWT de produccion).
 * Los tres microservicios comparten el mismo secreto para poder validar el token que emite ms-admin.
 */
public class TokenService {

    private static final String ALGORITMO = "HmacSHA256";
    private final byte[] secreto;
    private final long minutosValidez;
    private final Clock reloj;

    public TokenService(String secreto, long minutosValidez, Clock reloj) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.minutosValidez = minutosValidez;
        this.reloj = reloj;
    }

    public String emitir(Long usuarioId, String email, String rol) {
        long expira = reloj.millis() + minutosValidez * 60_000L;
        String datos = usuarioId + "|" + email + "|" + rol + "|" + expira;
        String cuerpo = codificar(datos.getBytes(StandardCharsets.UTF_8));
        return cuerpo + "." + codificar(firmar(cuerpo));
    }

    /** Devuelve la sesion si el token es autentico y no ha expirado; si no, vacio. */
    public Optional<Sesion> validar(String token) {
        try {
            if (token == null) {
                return Optional.empty();
            }
            String[] partes = token.split("\\.");
            if (partes.length != 2) {
                return Optional.empty();
            }
            byte[] firmaEsperada = firmar(partes[0]);
            byte[] firmaRecibida = Base64.getUrlDecoder().decode(partes[1]);
            if (!MessageDigest.isEqual(firmaEsperada, firmaRecibida)) {
                return Optional.empty();
            }
            String datos = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            String[] campos = datos.split("\\|", 4);
            if (campos.length != 4) {
                return Optional.empty();
            }
            long expira = Long.parseLong(campos[3]);
            if (reloj.millis() > expira) {
                return Optional.empty();
            }
            return Optional.of(new Sesion(Long.valueOf(campos[0]), campos[1], campos[2]));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] firmar(String cuerpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto, ALGORITMO));
            return mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
