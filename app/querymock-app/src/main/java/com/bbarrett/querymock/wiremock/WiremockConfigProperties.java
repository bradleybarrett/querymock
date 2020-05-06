package com.bbarrett.querymock.wiremock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WiremockConfigProperties
{
    private static Logger logger = LoggerFactory.getLogger(WiremockConfigProperties.class);

    @Value("${wiremock.port}")
    private int port;

    @Value("${wiremock.directory}")
    private String directory;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
