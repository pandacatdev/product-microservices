package study.microservices.eurakaserver;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  private final String username;
  private final String password;

  @Autowired
  public SecurityConfig(
      @Value("${app.eureka-username}") String username,
      @Value("${app.eureka-password}") String password
  ) {
    this.username = username;
    this.password = password;
  }

  @Bean
  public InMemoryUserDetailsManager userDetailsService() {
    return new InMemoryUserDetailsManager(
        User.withUsername(username)
            .password("{noop}" + password)
            .roles("USER")
            .build()
    );
  }

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
        // Disable CSRF to allow services to register themselves with Eureka
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(withDefaults());
    return http.build();
  }
}
