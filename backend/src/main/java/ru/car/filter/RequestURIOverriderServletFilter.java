package ru.car.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;

public class RequestURIOverriderServletFilter  implements Filter {
    private ObjectMapper objectMapper = new ObjectMapper();

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        if (requestURI.contains("swagger") || requestURI.contains("api-docs")) {
            chain.doFilter(request, response);
        } else if (requestURI.equals(Strings.EMPTY) || requestURI.equals("/")) {
            chain.doFilter(new RequestWrapper(httpRequest, objectMapper), response);
        } else {
            chain.doFilter(request, response);
        }
    }
}