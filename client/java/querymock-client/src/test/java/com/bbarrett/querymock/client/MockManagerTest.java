package com.bbarrett.querymock.client;

import org.junit.After;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class MockManagerTest
{
    private String locationMock = "location-service-mock";
    private int locationPort = 8090;

    @Test
    public void test()
    {
        MockManager.startMock(locationMock, locationPort, getPathToResource("wiremock"));
        MockManager.waitForMocks(locationMock);

        /* Code under test that makes an http call to an external service. */
        Object result = getLocationById("locationId_1");

        assertThat(result).isNotNull();
    }

    @After
    public void cleanUp()
    {
        MockManager.stopMocks(locationMock);
    }

    private static String getPathToResource(String relativePath)
    {
        URL url = MockManagerTest.class.getClassLoader().getResource(relativePath);
        return url.getPath();
    }

    private String getUrl(int port, String endpointPath)
    {
        return "http://localhost:" + port + endpointPath;
    }

    private Object getLocationById(String locationId)
    {
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> pathVars = new HashMap<>();
        pathVars.put("locationId", locationId);
        return restTemplate.getForObject(
                getUrl(locationPort, "/location/{locationId}"),
                String.class, pathVars);
    }
}
