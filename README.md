### QueryMock Overview

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

See the following test data directories for example config data for a mocked endpoint:
* getLocationById: `/src/test/query/test1/wiremock`
* getLocationbyStatus:  `/src/test/query/test3/wiremock`
    
Note: At this time, the data to query cannot be templated with values from the http request. 
The query result is substituted into the templated response payload as-is. 

### Implementation Details

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

### Build & Run the App Code

Build the docker image 
* `./buildImage.sh`

Build the springbot app
* `./gradlew clean build`

Start mock as a springboot app (seed config data is provided under /src/main/resources/wiremock)
* `./gradlew bootrun`
