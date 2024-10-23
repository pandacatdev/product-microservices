package study.microservices.composite.product;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

@TestConfiguration
public class TestSecurityConfig {
  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        .csrf(CsrfSpec::disable)
        .authorizeExchange(exchange
            -> exchange.anyExchange().permitAll());

    return http.build();
  }
}
