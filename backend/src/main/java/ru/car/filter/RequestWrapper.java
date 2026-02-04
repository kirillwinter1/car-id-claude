package ru.car.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.*;

public class RequestWrapper extends HttpServletRequestWrapper {
    private byte[] cachedBody;
    private String url;


    public RequestWrapper(HttpServletRequest request, ObjectMapper objectMapper) throws IOException {
        // So that other request method behave just like before
        super(request);
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = StreamUtils.copyToByteArray(requestInputStream);

        JsonNode jsonNode = objectMapper.readTree(cachedBody);
        if (jsonNode.get("method") != null) {
            url = "/api/" + jsonNode.get("method").asText();
        } else {
            url = super.getRequestURI();
        }
    }

    @Override
    public String getRequestURI() {
        return url;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }
}
