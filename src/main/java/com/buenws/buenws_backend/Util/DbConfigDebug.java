package com.buenws.buenws_backend.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class DbConfigDebug implements CommandLineRunner {

    @Value("${spring.datasource.url:NOT_SET}")
    private String datasourceUrl;

    @Value("${username:NOT_SET}")
    private String username;

    @Value("${host:NOT_SET}")
    private String host;

    @Override
    public void run(String... args) {
        System.out.println("DEBUG_DB_HOST=" + host);
        System.out.println("DEBUG_DB_USERNAME_SET=" + !"NOT_SET".equals(username));
        System.out.println("DEBUG_DATASOURCE_URL=" + datasourceUrl);
    }
}