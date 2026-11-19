package cl.jdvzaror.finanzas.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final TokenService tokens;

    public ConfiguracionWeb(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new AutenticacionInterceptor(tokens))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/error");
    }
}
