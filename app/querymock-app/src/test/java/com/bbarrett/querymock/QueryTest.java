package com.bbarrett.querymock;

import com.bbarrett.querymock.wiremock.WiremockInstance;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueryTest
{
	String baseDirectory = "query";

	@Autowired
	private WiremockInstance wiremockInstance;

	/* Query find one endpoint with path variable and response body template. */
	@Test
	void test1() throws IOException, JSONException
	{
		String directory = baseDirectory + "/test1";

		/* Configure mock endpoints and set up data. */
		wiremockInstance.reconfigureMocks(TestUtil.getResource(directory).getPath(), "/wiremock");
		String locationId = "locationId_1";

		/* Make a rest call to the mock service. */
		RestTemplate restTemplate = new RestTemplate();
		HashMap<String, Object> pathVars = new HashMap<>();
		pathVars.put("locationId", locationId);
		String outputResponse = restTemplate.getForObject(
				TestUtil.getRequestUrl(wiremockInstance.getWiremockConfigProperties().getPort(),
						"/location/{locationId}"),
				String.class, pathVars);

		/* Assert the response matches the expected json payload. */
		TestUtil.assertJsonEquals(directory + "/expected/response.json",
				outputResponse, JSONCompareMode.STRICT);
	}

	/* Query find one endpoint with path variable and response body template, but return null on not found. */
	@Test
	void test2() throws IOException, JSONException
	{
		String directory = baseDirectory + "/test2";

		/* Configure mock endpoints and set up data. */
		wiremockInstance.reconfigureMocks(TestUtil.getResource(directory).getPath(), "/wiremock");
		String locationId = "locationId_4";

		/* Make a rest call to the mock service. */
		RestTemplate restTemplate = new RestTemplate();
		HashMap<String, Object> pathVars = new HashMap<>();
		pathVars.put("locationId", locationId);
		String outputResponse = restTemplate.getForObject(
				TestUtil.getRequestUrl(wiremockInstance.getWiremockConfigProperties().getPort(),
						"/location/{locationId}"),
				String.class, pathVars);

		/* Assert the response matches the expected json payload. */
		assertThat(outputResponse).isNull();
	}

	/* Query find one endpoint with path variable and no response body template. */
	@Test
	void test3() throws IOException, JSONException
	{
		String directory = baseDirectory + "/test3";

		/* Configure mock endpoints and set up data. */
		wiremockInstance.reconfigureMocks(TestUtil.getResource(directory).getPath(), "/wiremock");
		String status = "open";

		/* Make a rest call to the mock service. */
		RestTemplate restTemplate = new RestTemplate();
		String outputResponse = restTemplate.getForObject(
				TestUtil.getRequestUrl(wiremockInstance.getWiremockConfigProperties().getPort(),
						"/location?status=" + status),
				String.class);

		/* Assert the response matches the expected json payload. */
		TestUtil.assertJsonEquals(directory + "/expected/response.json",
				outputResponse, JSONCompareMode.STRICT);
	}
}
