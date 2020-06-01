## Table of Contents

* [QueryMock Overview](#1)
* [Example 1: Get location by id](#2)
* [Example 2: Get location by status](#3)
* [Write a Test with the Client (pseudo code example)](#4)
* [Implementation Details](#5)
* [Build the Docker Image](#6)

## QueryMock Overview <a name="1"></a>

A fast, containerized mock server that can mock query endpoints! 

Run tests in seconds: start the mock servers once and reconfigure on the fly for subsequent tests.

Configured with json files and managed by simple, language-specific clients. Clients provided at this time: Bash, Java, Python.

Define an endpoint mapping and data set, then query that data using mvel expressions templated with request info.

Example endpoints mocked by a query:
* Find a record by id.
* Find all records with a particular status.
* Find all records with a status in a certain range of values.

Useful for mocking query endpoints called multiple times in a single test. 
In this case, QueryMock lets you create one mock endpoint with dynamic data instead of multiple mock endpoints with static data.
This reduces mock complexity and makes tests easier to write and maintain.

Language-specific clients execute docker commands to manage mock servers. Each mock server is run as a container configured by json files. 

QueryMock is implemented as a customized instance of WireMock with extensions for response templating and query support.
This tool provides all the existing features of WireMock along with templates and queries.

For each mock endpoint, specify the...
1. Data to query
    * json payload
2. Query expression to evaluate
    * mvel expression templated with request data
3. Response payload to hold the query result
    * json payload templated with request data

Note: At this time, the data to query cannot be templated with values from the http request. 
The query result is substituted into the templated response payload as-is.

## Example 1: Get location by id <a name="2"></a>

Endpoint to mock: 
* ```GET /location/{locationId}```
* Query result is populated in the "Data" field of the reponse.

Test directory with config files:
```
/mappings
    getLocationById.json     <=== Endpoint Mapping
/__files
    queryLocationById.json   <=== Query Details
/querydata
    location.json            <=== Query Data
```

/mappings/getLocationById.json
```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/location/([A-Za-z0-9-_]+)"
  },
  "response": {
    "status": 200,
    "bodyFileName": "queryLocationById.json",
    "transformers": ["body-transformer", "query-transformer"],
    "transformerParameters": {
      "urlRegex": "/location/(?<id>.*?)"
    }
  }
}
```

/__files/queryLocationById.json
```json
{
  "query": "locationId == '$(id)'",
  "data": "location.json",
  "findOne": true,
  "bodyTemplate": {
    "Data": "{{ result }}"
  }
}
```

/querydata/location.json
```json
[
  {
    "locationId": "locationId_1",
    "status": "open"
  },
  {
    "locationId": "locationId_2",
    "status": "open"
  },
  {
    "locationId": "locationId_3",
    "status": "closed"
  }
]
```

Input Request: ```GET /location/locationId_1```

Body of response returned by the mock server:
```json
{
  "Data": {
    "locationId": "locationId_1",
    "status": "open"
  }
}
```

Input Request: ```GET /location/locationId_3```

Body of response returned by the mock server:
```json
{
  "Data": {
    "locationId": "locationId_3",
    "status": "closed"
  }
}
```

## Example 2: Get locations by status <a name="3"></a>

#### Endpoint to mock:
* ```GET /location?status=someValue```
* Response is the query result as a list.

#### Test directory with config files:
```
/mappings
    getLocationByStatus.json     <=== Endpoint Mapping
/__files
    queryLocationByStatus.json   <=== Query Details
/querydata
    location.json                <=== Query Data
```

/mappings/getLocationByStatus.json
```json
{
  "request": {
    "method": "GET",
    "urlPattern": "/location\\?status=([A-Za-z0-9-_]+)"
  },
  "response": {
    "status": 200,
    "bodyFileName": "queryLocationByStatus.json",
    "transformers": ["body-transformer", "query-transformer"]
  }
}
```

/__files/queryLocationByStatus.json
```json
{
  "query": "status == '$(status)'",
  "data": "location.json",
  "findOne": false
}
```

/querydata/location.json
```json
[
  {
    "locationId": "locationId_1",
    "status": "open"
  },
  {
    "locationId": "locationId_2",
    "status": "open"
  },
  {
    "locationId": "locationId_3",
    "status": "closed"
  }
]
```
#### Responses from mock server:

Input request: ```GET /location?status=open```

Response body:
```json
[
  {
    "locationId": "locationId_1",
    "status": "open"
  },
  {
    "locationId": "locationId_2",
    "status": "open"
  }
]
```

Input request: ```GET /location?status=closed```

Response body:
```json
[
  {
    "locationId": "locationId_3",
    "status": "closed"
  }
]
```

## Write a Test with the Client (pseudo code example) <a name="4"></a>

#### Test resource directory with config json files for each test:
```
/test
    /resources
        /test1
            /wiremock
                /location
                    /mappings
                        getLocationById.json
                    /__files
                        queryLocationById.json
                    /querydata
                        location.json
                /item
                    /mappings
                        getItemById.json
                    /__files
                        queryItemById.json
                    /querydata
                        item.json
        /test2
            /wiremock
                /location
                    /mappings
                        getLocationById.json
                    /__files
                        queryLocationById.json
                    /querydata
                        location.json
```

#### Test Pseudo Code (utilizes mock reconfiguration for faster execution times)
```
String baseDirectory = "/test/resources"
String locationMockName = "location-mock"
String itemMockName = "item-mock"

// utility method to ensure existing location-mock instances are reconfigured whenever possible
startLocationMock(String wiremockSubDirectory)
{
    querymock.startMock(locationMockName, 8081, 8091, baseDirectory, wiremockSubDirectory)
}

// utility method to ensure existing item-mock instances are reconfigured whenever possible
startItemMock(String wiremockSubDirectory)
{
    querymock.startMock(itemMockName, 8082, 8092, baseDirectory, wiremockSubDirectory)
}

test1()
{
    startLocationMock("/test1/wiremock/location")
    startItemMock("/test1/wiremock/item")
    querymock.waitForMocks(locationMockName, itemMockName)

    // execute test code here...
}

test2()
{
    startLocationMock("/test2/wiremock/location) // will reconfigure the existing location-mock
    querymock.waitForMocks(locationMockName)

    // execute test code here...
}

runAllTests()
{
    // execute tests
    test1()
    test2()
    
    // clean-up mocks
    querymock.stopMocks(locationMockName, itemMockName)
}
```

## Implementation Details <a name="5"></a>

The wiremock instance is run as a spring application in a docker container.
Configuration files for the wiremock stubs (mappings, files, and querydata) are provided to the container as a bindmount directory.
The container with bindmount config allows the wiremock instance to be configured and run the same way for any programming language.
Simple language-specific clients can be created to execute docker commands which start and stop the mock instance.

The BodyTransformer wiremock extension is used for templating the query and response with information from the request.
Refer to the BodyTransformer documentation for additional details on the replacement syntax and supported template features: 
https://github.com/opentable/wiremock-body-transformer.

At start-up, data from the /querydata directory is loaded into memory. 
Data is queried for a mock endpoint using the QueryDetails specified in the body file of the endpoint mapping.
Query data can be reused for multiple endpoints.

## Build the Docker Image <a name="6"></a>
```shell script
# navigate to the app directory and build the docker image
cd app/querymock-app && ./docker-build.sh && cd ../..

# check that the image has been created (will print some info if present)
docker images | grep querymock
```
