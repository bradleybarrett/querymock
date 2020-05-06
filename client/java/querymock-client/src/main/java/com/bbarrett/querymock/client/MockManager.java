package com.bbarrett.querymock.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MockManager
{
    private static final String portLabel = "com.querymock.port";
    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * Start a mock instance.
     *
     * @param name Container name of the mock instance.
     * @param port Host port mapped to wiremock instance of the container.
     * @param resourceDir Absolute path of the wiremock resource files on the host.
     */
    public static void startMock(String name, int port, String resourceDir)
    {
        startMock(name, port, resourceDir, "bbarrett/querymock:latest");
    }

    /**
     * Start a mock instance.
     *
     * @param name Container name of the mock instance.
     * @param port Host port mapped to wiremock instance of the container.
     * @param resourceDir Absolute path of the wiremock resource files on the host.
     * @param image Docker image to start (name of some QueryMock image on the host).
     */
    public static void startMock(String name, int port, String resourceDir, String image)
    {
        int containerWiremockPort = 8081;
        String containerWiremockDirectory = "/wiremock";

        executeCommand("docker", "run", "-it", "-d",
                "--name", name,
                "-l", portLabel + "=" + port,
                "-p", port + ":" + containerWiremockPort,
                "-v", resourceDir + ":" + containerWiremockDirectory,
                image,
                "--wiremock.directory=" + containerWiremockDirectory);
    }

    /**
     * Stop the provided mock instances.
     *
     * @param names Container names of mock instances to stop.
     */
    public static void stopMocks(String... names)
    {
        Arrays.asList(names).forEach(name ->
                executeCommand("docker", "rm", "-f", name));
    }

    /**
     * Wait for the provided mock instances to start.
     * Throws an exception if all mocks do not start within 30s.
     * Mock instances are polled every 2s.
     *
     * @param names Container names of mock instances to poll.
     */
    public static void waitForMocks(String... names)
    {
        int timeout = 30 * 1000; // ms
        int waitPeriod = 2 * 1000; // ms
        waitForMocks(timeout, waitPeriod, Arrays.asList(names));
    }

    /**
     * Wait for the provided mock instances to start.
     * Throws an exception if all mocks do not start within the timeout.
     *
     * @param timeout Time in ms to wait for all mocks to come up.
     * @param waitPeriod Period in ms to poll a mock instance.
     * @param names Container names of mock instances to poll.
     */
    public static void waitForMocks(int timeout, int waitPeriod, List<String> names)
    {
        int timeElapsed = 0; // ms
        for (String name : names)
        {
            while (!mockServerIsUp(name))
            {
                if (timeElapsed >= timeout)
                {
                    throw new RuntimeException(MessageFormat.format(
                            "Mock server {0} not up after {1}ms, make sure docker has enough resources.",
                            name, timeout));
                }
                timeElapsed += waitPeriod;
                sleep(waitPeriod);
            }
        }
    }

    private static boolean mockServerIsUp(String name)
    {
        HttpStatus status;
        try
        {
            ResponseEntity<Object> responseEntity = restTemplate.getForEntity(
                    getUrl(getPortForMock(name), "/__admin/mappings?limit=1"),
                    Object.class);
            status = responseEntity.getStatusCode();
        }
        catch (ResourceAccessException resourceAccessException)
        {
            status = null;
        }

        return HttpStatus.OK.equals(status);
    }

    private static Integer getPortForMock(String name)
    {
        String inspectTemplate = "{{ index .Config.Labels \"" + portLabel + "\"}}";
        InputStream inputStream = executeCommand("docker", "inspect", "--format", inspectTemplate, name);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        Integer port;
        try
        {
            port = Integer.valueOf(bufferedReader.readLine());
            bufferedReader.close();
        }
        catch (IOException ioException)
        {
            port = null;
            ioException.printStackTrace();
        }

        return port;
    }

    private static String getUrl(Integer port, String endpointPath)
    {
        return "http://localhost:" + port + endpointPath;
    }

    private static void sleep(int waitPeriod)
    {
        try {
            Thread.sleep(waitPeriod);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    private static InputStream executeCommand(String... command)
    {
        return executeCommand(Arrays.asList(command));
    }

    private static InputStream executeCommand(List<String> command)
    {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        InputStream inputStream;
        try
        {
            Process process = processBuilder.start();
            process.waitFor();
            inputStream = process.getInputStream();
        }
        catch (IOException | InterruptedException exception)
        {
            exception.printStackTrace();
            inputStream = null;
        }

        return inputStream;
    }
}
