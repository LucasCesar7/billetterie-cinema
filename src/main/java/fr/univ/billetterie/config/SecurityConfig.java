package fr.univ.billetterie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

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
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics de consultation API REST
                        .requestMatchers(HttpMethod.GET, "/api/films", "/api/films/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/seances", "/api/seances/**").permitAll()

                        // Opérations de programmation (Programmateur ou Admin)
                        .requestMatchers(HttpMethod.POST, "/api/programmation/**").hasAnyRole("PROGRAMMATEUR", "ADMIN")

                        // Opérations d'administration (Admin uniquement)
                        .requestMatchers(HttpMethod.POST, "/api/admin/**").hasRole("ADMIN")

                        // Création de réservation (Client uniquement)
                        .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("CLIENT")

                        // Annulation de réservation (Client ou Admin)
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAnyRole("CLIENT", "ADMIN")

                        // Tout autre appel API requiert une authentification
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Ressources statiques et pages web publiques
                        .requestMatchers(HttpMethod.GET, "/", "/films", "/films/**", "/seances/**", "/reserver/**", "/confirmation/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reserver", "/annuler/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/connexion").permitAll()
                        .requestMatchers(HttpMethod.POST, "/connexion").permitAll()
                        .requestMatchers(HttpMethod.GET, "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // Documentation Swagger / OpenAPI et Actuator Health publics
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Administration web (Admin uniquement)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Programmation web (Programmateur ou Admin)
                        .requestMatchers("/programmation/**").hasAnyRole("PROGRAMMATEUR", "ADMIN")

                        // Toute autre page requiert d'être authentifié
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .loginProcessingUrl("/connexion")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/connexion?erreur=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/deconnexion")
                        .logoutSuccessUrl("/connexion?deconnexion=true")
                        .permitAll()
                );

        return http.build();
    }
}
