package com.bbarrett.querymock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.net.URL;

public class TestUtil
{
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void assertJsonEquals(String expectedJsonFile, String actualJson, JSONCompareMode jsonCompareMode)
            throws IOException, JSONException
    {
        String expectedResponse = objectMapper
                .readTree(getResource(expectedJsonFile))
                .toString();
        JSONAssert.assertEquals(expectedResponse, actualJson, jsonCompareMode);
    }

    public static URL getResource(String relativePath)
    {
        return TestUtil.class.getClassLoader().getResource(relativePath);
    }

    public static String getRequestUrl(int port, String endpointPath)
    {
        return "http://localhost:" + port + endpointPath;
    }
}
