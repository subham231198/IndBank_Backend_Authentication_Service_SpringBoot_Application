package com.example.Ind.Auth.AuthenticationService.Utility;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ConnectionKeepAlive {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.host.authentication.service}")
    private String selfHost;

    @Value("${app.url.authentication.service.health}")
    private String healthUrl;



    @Scheduled(fixedDelay = 20000)
    public void keepAliveAuthorization() {
        try {
            String url = selfHost + healthUrl;
            restTemplate.headForHeaders(url);
            log.debug("Health ping to Own Service successful");
        }
        catch (Exception e) {
            log.debug("Own Service health ping failed: {}", e.getMessage());
        }
    }
}