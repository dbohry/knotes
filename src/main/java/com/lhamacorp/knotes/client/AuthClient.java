package com.lhamacorp.knotes.client;

import com.lhamacorp.knotes.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpMethod.GET;

@Component
public class AuthClient {

    private final RestTemplate rest;
    private final String baseUrl;

    public AuthClient(RestTemplate rest, @Value("${auth.api}") String baseUrl) {
        this.rest = rest;
        this.baseUrl = baseUrl;
    }

    @Cacheable(value = "current", key = "#token")
    public CurrentUser current(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(null, headers);

        try {
            ResponseEntity<CurrentUser> response = rest.exchange(baseUrl + "/users/current", GET, entity, CurrentUser.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new UnauthorizedException("Unexpected response status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            throw new UnauthorizedException("Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new UnauthorizedException("An error occurred: " + e.getMessage());
        }
    }

    public record CurrentUser(String id, String username, List<String> roles) {
    }

}
