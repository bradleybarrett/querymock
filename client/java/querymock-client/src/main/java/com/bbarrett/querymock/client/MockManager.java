package com.bbarrett.querymock.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

public class MockManager
{
    private static final String adminPortLabel = "com.querymock.port.admin";
    private static final String wiremockPortLabel = "com.querymock.port.wiremock";
    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * Start a mock instance using the wiremock resources located at the wiremockSubDir within the resourceDir.
     * Reuse an existing querymock instance if one already exists with the provided name and port mappings.
     *
     * @param name Container name of the mock instance.
     * @param adminPort Host port mapped to the rest controller of the container.
     * @param wiremockPort Host port mapped to wiremock instance of the container.
     * @param resourceDir Absolute path of host resource directory with data for all test scenarios.
     * @param wiremockSubDir Relative path to wiremock resources for the test scenario.
     */
    public static void startMock(String name, int adminPort, int wiremockPort, String resourceDir,
                                 String wiremockSubDir)
    {
        startMock(name, adminPort, wiremockPort, resourceDir, wiremockSubDir, "bbarrett/querymock:latest");
    }

    /**
     * Start a mock instance using the wiremock resources located at the wiremockSubDir within the resourceDir.
     * Reuse an existing querymock instance if one already exists with the provided name and port mappings.
     *
     * @param name Container name of the mock instance.
     * @param adminPort Host port mapped to the rest controller of the container.
     * @param wiremockPort Host port mapped to wiremock instance of the container.
     * @param resourceDir Absolute path of host resource directory with data for all test scenarios.
     * @param wiremockSubDir Relative path to wiremock resources for the test scenario.
     * @param image Docker image to start (name of some QueryMock image on the host).
     */
    public static void startMock(String name, int adminPort, int wiremockPort, String resourceDir,
                                 String wiremockSubDir, String image)
    {
        /* check if mock exists with name and port mappings */
        Integer existingAdminPort = getPortForMock(name, adminPortLabel);
        Integer existingWiremockPort = getPortForMock(name, wiremockPortLabel);
        if (existingAdminPort == null && existingWiremockPort == null)
        {
            /* start a new instance */
            startNewMock(name, adminPort, wiremockPort, resourceDir, wiremockSubDir, image);
        }
        else if (!Integer.valueOf(adminPort).equals(existingAdminPort)
                || !Integer.valueOf(wiremockPort).equals(existingWiremockPort))
        {
            /* stop the existing instance because the new instance requires different ports */
            stopMocks(name);
            startNewMock(name, adminPort, wiremockPort, resourceDir, wiremockSubDir, image);
        }
        else /* a mock instance exists with all the requested ports */
        {
            /* reconfigure the existing instance */
            reconfigureMock(adminPort, wiremockSubDir);
        }
    }

    /**
     * Stop the provided mock instances.
     *
     * Most tests should not call this method. Always be prefer reconfiguring an existing mock to starting a new mock.
     * Reconfiguring is about 10x faster than starting a new instances. Reconfiguring n mocks takes about 1s.
     * Starting a new instance for n mocks takes about 10s (depending on the memory and compute resources provided
     * to docker).
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
                    getUrl(getPortForMock(name, wiremockPortLabel), "/__admin/mappings?limit=1"),
                    Object.class);
            status = responseEntity.getStatusCode();
        }
        catch (ResourceAccessException resourceAccessException)
        {
            status = null;
        }

        return HttpStatus.OK.equals(status);
    }

    private static Integer getPortForMock(String name, String portLabel)
    {
        String inspectTemplate = "{{ index .Config.Labels \"" + portLabel + "\"}}";
        InputStream inputStream = executeCommand("docker", "inspect", "--format", inspectTemplate, name);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        Integer port;
        try
        {
            String portString = bufferedReader.readLine();
            if (StringUtils.isEmpty(portString))
                port = null;
            else
                port = Integer.valueOf(portString);
            bufferedReader.close();
        }
        catch (IOException ioException)
        {
            port = null;
            ioException.printStackTrace();
        }

        return port;
    }

    private static void reconfigureMock(int adminPort, String wiremockSubDir)
    {
        HttpStatus status;
        try
        {
            ResponseEntity<Object> responseEntity = restTemplate.getForEntity(
                    getUrl(adminPort, "/querymock/reconfigure?subdirectory=" + wiremockSubDir),
                    Object.class);
            status = responseEntity.getStatusCode();
        }
        catch (ResourceAccessException resourceAccessException)
        {
            status = null;
            resourceAccessException.printStackTrace();
        }

        if (!HttpStatus.OK.equals(status))
        {
            throw new RuntimeException(MessageFormat.format(
                    "Request to reconfigure mock failed for adminPort {0} and wiremockSubDir {1}",
                    adminPort, wiremockSubDir));
        }
    }

    private static void startNewMock(String name, int adminPort, int wiremockPort, String resourceDir,
                                     String wiremockSubDir, String image)
    {
        int containerAdminPort = 8080;
        int containerWiremockPort = 8081;
        String containerResourceDirectory = "/wiremock";

        executeCommand("docker", "run", "-d",
                "--name", name,
                "-l", adminPortLabel + "=" + adminPort,
                "-l", wiremockPortLabel + "=" + wiremockPort,
                "-p", adminPort + ":" + containerAdminPort,
                "-p", wiremockPort + ":" + containerWiremockPort,
                "-v", resourceDir + ":" + containerResourceDirectory,
                image,
                "--resource.directory=" + containerResourceDirectory,
                "--resource.subdirectory.wiremock=" + wiremockSubDir);
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
