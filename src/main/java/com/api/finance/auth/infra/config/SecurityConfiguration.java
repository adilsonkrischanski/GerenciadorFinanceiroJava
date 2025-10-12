package com.api.finance.auth.infra.config;

import com.api.finance.auth.infra.config.filter.TokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
public class SecurityConfiguration {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenFilter tokenFilter;

    @Bean
    public SecurityFilterChain configureSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        // Rotas públicas do Angular SPA
                        .requestMatchers(
                                "/",                   // raiz
                                "/index.html",         // página principal
                                "/favicon.ico",
                                "/assets/**",          // assets do Angular
                                "/**/*.js",            // arquivos JS do build
                                "/**/*.css",           // arquivos CSS do build
                                "/**/*.ico",           // ícones
                                "/auth/**",            // APIs de autenticação
                                "/**/{path:[^.]*}"     // SPA: todas as rotas sem extensão
                        ).permitAll()
                        // Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )
                // Stateless: JWT, sem sessão
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Adiciona filtro de token antes do UsernamePasswordAuthenticationFilter
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    // AuthenticationProvider com BCrypt
    @Bean
    @Primary
    public AuthenticationProvider getAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(configurePasswordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    // AuthenticationManager
    @Bean
    @Primary
    public AuthenticationManager getAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // PasswordEncoder
    @Bean
    @Primary
    public PasswordEncoder configurePasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
