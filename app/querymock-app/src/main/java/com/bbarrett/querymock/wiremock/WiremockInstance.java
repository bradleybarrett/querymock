package com.bbarrett.querymock.wiremock;

import com.bbarrett.querymock.wiremock.extension.BodyTransformer;
import com.bbarrett.querymock.wiremock.extension.QueryTransformer;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Component
public class WiremockInstance
{
    private WireMockServer wireMockServer;
    private WiremockConfigProperties wiremockConfigProperties;

    @Autowired
    public WiremockInstance(WiremockConfigProperties wiremockConfigProperties)
    {
        this.wiremockConfigProperties = wiremockConfigProperties;
    }

    @PostConstruct
    public void init()
    {
        // Create a BodyTransformer extension.
        BodyTransformer bodyTransformer = new BodyTransformer();

        // Create a QueryTransformer extension with the provided query data.
        String dataDirectory = wiremockConfigProperties.getDirectory() + "/querydata";
        QueryDataStore queryDataStore = new QueryDataStore(dataDirectory);
        QueryTransformer queryTransformer = new QueryTransformer(queryDataStore);

        // Create a wiremock instance with the custom extensions.
        wireMockServer = new WireMockServer(wireMockConfig()
                .port(wiremockConfigProperties.getPort())
                .usingFilesUnderDirectory(wiremockConfigProperties.getDirectory())
                .extensions(bodyTransformer, queryTransformer));

        // Start the wiremock instance.
        wireMockServer.start();
    }

    public void reconfigureMocks(String directory)
    {
        wireMockServer.stop();
        wiremockConfigProperties.setDirectory(directory);
        init();
    }

    public WireMockServer getWireMockServer() {
        return wireMockServer;
    }

    public WiremockConfigProperties getWiremockConfigProperties() {
        return wiremockConfigProperties;
    }
}
