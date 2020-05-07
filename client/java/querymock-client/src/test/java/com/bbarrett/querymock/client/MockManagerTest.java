package com.bbarrett.querymock.client;

import org.json.JSONException;
import org.junit.After;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;

public class MockManagerTest
{
    private String locationMock = "location-service-mock";
    private int locationMockAdminPort = 8090;
    private int locationServicePort = 8091;

    @After
    public void cleanUp()
    {
        MockManager.stopMocks(locationMock);
    }

    @Test
    public void testStartWaitStop() throws IOException, JSONException
    {
        String directory = "testStartWaitStop";

        /* Start a new mock instance. */
        MockManager.startMock(locationMock, locationMockAdminPort, locationServicePort,
                TestUtil.getResource(directory).getPath(), "/wiremock");
        MockManager.waitForMocks(locationMock);

        /* Makes an external call to the mock service. */
        String outputResponse = getLocationById("locationId_1");

        TestUtil.assertJsonEquals(directory + "/expected/response.json",
                outputResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void testReconfigureWaitStop() throws IOException, JSONException
    {
        String directory = "testReconfigureWaitStop";

        /* Start a new mock instance. */
        MockManager.startMock(locationMock, locationMockAdminPort, locationServicePort,
                TestUtil.getResource(directory).getPath(), "");
        MockManager.waitForMocks(locationMock);

        /* Start the mock by reconfiguring the existing mock. */
        MockManager.startMock(locationMock, locationMockAdminPort, locationServicePort,
                TestUtil.getResource(directory).getPath(), "/wiremock");
        MockManager.waitForMocks(locationMock);

        /* Makes an external call to the mock service. */
        String outputResponse = getLocationById("locationId_1");

        TestUtil.assertJsonEquals(directory + "/expected/response.json",
                outputResponse, JSONCompareMode.STRICT);
    }

    private String getUrl(int port, String endpointPath)
    {
        return "http://localhost:" + port + endpointPath;
    }

    private String getLocationById(String locationId)
    {
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> pathVars = new HashMap<>();
        pathVars.put("locationId", locationId);
        return restTemplate.getForObject(
                getUrl(locationServicePort, "/location/{locationId}"),
                String.class, pathVars);
    }
}
