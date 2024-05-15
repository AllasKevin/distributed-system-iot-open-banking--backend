package com.rs.aragog;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

@Configuration
public class config {

    @Value("${trust-store}")
    private Resource trustStore;

    @Value("${trust-store-password}")
    private String trustStorePassword;

    @Value("${key-store}")
    private Resource keyStore;

    @Value("${key-store-password}")
    private char[] keyStorePassword;

    @Value("${key-password}")
    private char[] keyPassword;

    @Bean
    public RestTemplate restTemplateWithTrustStore(RestTemplateBuilder builder) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray())
                .loadKeyMaterial(keyStore.getURL(), keyStorePassword, keyPassword)
                .setProtocol("TLSv1.2")
                .build();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        System.out.println("RestTemplate bean created");
        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    @Bean
    public TinkRequestHelper tinkRequestHelper(){
        return new TinkRequestHelper();
    }


}
