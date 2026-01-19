package com.lhamacorp.knotes.context;

import com.lhamacorp.knotes.client.AuthClient;
import com.lhamacorp.knotes.client.AuthClient.CurrentUser;
import com.lhamacorp.knotes.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static java.util.Collections.emptyList;

@Component
public class ServiceContextFilter extends OncePerRequestFilter {

    private final AuthClient authClient;

    public ServiceContextFilter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
            response.setHeader("Access-Control-Max-Age", "3600");
            return;
        }

        String path = request.getRequestURI();
        if (path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            CurrentUser user = extractCurrentUser(request);

            if (user != null) {
                UserContextHolder.set(new UserContext(user.id(), user.username(), user.roles()));

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    UserContextHolder.clear();
                }
            } else {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: User not found");
            }
        } catch (UnauthorizedException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized: Invalid token");
        }
    }

    private CurrentUser extractCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        return token == null
                ? new CurrentUser("1", "anon", emptyList())
                : authClient.current(token);
    }

}