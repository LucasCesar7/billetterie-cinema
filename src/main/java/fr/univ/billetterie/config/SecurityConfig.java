package fr.univ.billetterie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails client = User.builder()
                .username("client")
                .password(passwordEncoder.encode("client123"))
                .roles("CLIENT")
                .build();

        UserDetails programmateur = User.builder()
                .username("programmateur")
                .password(passwordEncoder.encode("prog123"))
                .roles("PROGRAMMATEUR")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(client, programmateur, admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics de consultation
                        .requestMatchers(HttpMethod.GET, "/api/films", "/api/films/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/seances", "/api/seances/**").permitAll()

                        // Documentation Swagger / OpenAPI et Actuator Health publics
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Opérations de programmation (Programmateur ou Admin)
                        .requestMatchers(HttpMethod.POST, "/api/programmation/**").hasAnyRole("PROGRAMMATEUR", "ADMIN")

                        // Opérations d'administration (Admin uniquement)
                        .requestMatchers(HttpMethod.POST, "/api/admin/**").hasRole("ADMIN")

                        // Création de réservation (Client uniquement)
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("CLIENT")

                        // Annulation de réservation (Client ou Admin)
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAnyRole("CLIENT", "ADMIN")

                        // Tout autre appel requiert une authentification
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
