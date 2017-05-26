/*
 * Copyright (c) 2017 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */
 
package com.ge.predix.solsvc.service;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 
 * @author 212421693 -
 */
public class HelloControllerIT extends AbstractBaseControllerIT
{
    @Value("${local.server.port}")
    private int localServerPort;

    private URL base;
    private RestTemplate template;

    /**
     * @throws Exception -
     */
    @Before
    public void setUp() throws Exception {
        this.template = new TestRestTemplate();
    }

    /**
     * @throws Exception -
     */
    @SuppressWarnings("nls")
    @Test
    public void getHealth() throws Exception {
        this.base = new URL("http://localhost:" + this.localServerPort + "/health");
        ResponseEntity<String> response = this.template.getForEntity(this.base.toString(), String.class);
        assertThat(response.getBody(), startsWith("{\"status\":\"up\""));
        
    }
    
    /**
     * @throws Exception -
     */
    @SuppressWarnings("nls")
    @Test
    public void getEcho() throws Exception {
        this.base = new URL("http://localhost:" + this.localServerPort + "/echo");
        ResponseEntity<String> response = this.template.getForEntity(this.base.toString(), String.class);
        assertThat(response.getBody(), startsWith("Greetings from Predix Spring Boot! echo="));
        
    }

}
