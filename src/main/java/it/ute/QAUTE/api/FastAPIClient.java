package it.ute.QAUTE.api;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
@Component
public class FastAPIClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String[] servers = {
            "http://100.81.7.74:5000",
            "api máy toxic AI"
    };
}
