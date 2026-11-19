package cl.jdvzaror.finanzas.seguridad;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Santiago"));
    }

    @Bean
    public TokenService tokenService(@Value("${jdv.seguridad.secreto}") String secreto,
                                     @Value("${jdv.seguridad.minutos-validez:120}") long minutos,
                                     Clock reloj) {
        return new TokenService(secreto, minutos, reloj);
    }
}
