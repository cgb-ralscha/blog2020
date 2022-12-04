package ch.rasc.hashupgrade;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  AuthenticationManager authenticationManager() {
    return authentication -> {
      throw new AuthenticationServiceException("Cannot authenticate " + authentication);
    };
  }

  @Bean
  public DelegatingSecurityContextRepository delegatingSecurityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new RequestAttributeSecurityContextRepository(),
        new HttpSessionSecurityContextRepository());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    MessageDigestPasswordEncoder md5PE = new MessageDigestPasswordEncoder("MD5");

    Argon2PasswordEncoder argon2PE = new Argon2PasswordEncoder(16, 32, 1, 1 << 17, 5);

    Map<String, PasswordEncoder> encoders = Map.of("argon2", argon2PE);

    DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder(
        "argon2", encoders);
    delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(md5PE);
    return delegatingPasswordEncoder;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(CsrfConfigurer::disable)
        .securityContext(securityContext -> securityContext
            .securityContextRepository(delegatingSecurityContextRepository()))
        .authorizeHttpRequests(
            customizer -> customizer.requestMatchers("/signin.html", "/signin")
                .permitAll().anyRequest().authenticated())
        .exceptionHandling(
            customizer -> customizer.authenticationEntryPoint((request, response, ex) -> {
              response.sendRedirect("/signin.html");
            }));
    return http.build();
  }

}