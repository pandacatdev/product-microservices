package study.microservices.composite.product;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        .authorizeExchange(exchanges ->
            exchanges
                .pathMatchers("/openapi/**").permitAll()
                .pathMatchers("/webjars/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers(HttpMethod.POST, "/product-composite/**").hasAuthority("SCOPE_product:write")
                .pathMatchers(DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write")
                .pathMatchers(GET, "/product-composite/**").hasAuthority("SCOPE_product:read")
                .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults()));

    return http.build();
  }
}
