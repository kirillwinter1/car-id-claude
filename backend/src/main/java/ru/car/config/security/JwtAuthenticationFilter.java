package ru.car.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.car.model.security.SecurityUser;
import ru.car.service.security.JwtService;
import ru.car.service.security.SecurityUserService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_NAME = "Authorization";
    private final JwtService jwtService;
    private final SecurityUserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Получаем токен из заголовка
        var authHeader = request.getHeader(HEADER_NAME);
        var uri = request.getRequestURI();
//        log.debug("Authorization {} {}", uri, authHeader);
        if (StringUtils.isEmpty(authHeader) || !StringUtils.startsWith(authHeader, BEARER_PREFIX)) {
            log.debug("NotAuthorized {} WEB sessionId = {} ", uri, request.getSession().getId());
            filterChain.doFilter(request, response);
            return;
        }

        // Обрезаем префикс и получаем имя пользователя из токена
        var jwt = authHeader.substring(BEARER_PREFIX.length());
        var telephone = jwtService.extractTelephone(jwt);
        var authId = jwtService.extractAuthId(jwt);

        log.debug("Authorization {} telephone = {}, authId = {} ", uri, telephone, authId);
        if (StringUtils.isNotEmpty(telephone) && SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityUser userDetails = userService.loadUserByUsername(telephone, authId);

            try {
                // Если токен валиден, то аутентифицируем пользователя
                if (jwtService.isTokenValid(jwt, userDetails)) {
//                    log.debug("Authorization {} telephone = {} is valid ", uri, telephone);
                    SecurityContext context = SecurityContextHolder.createEmptyContext();

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    context.setAuthentication(authToken);
                    SecurityContextHolder.setContext(context);
                } else {
                    log.debug("Authorization {} telephone = {} is invalid ", uri, telephone);
                }
            } catch (RuntimeException e) {
                log.debug("Authorization {} telephone = {} is invalid ", uri, telephone);
                throw e;
            }

        }
        filterChain.doFilter(request, response);
    }
}