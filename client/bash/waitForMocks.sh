#!/bin/bash
# example usage: ./waitForMocks.sh mock-1
# example usage: ./waitForMocks.sh mock-1 mock-2 mock-3

# Ping mock server on the provided port: ex. pingMockServer 8081
pingMockServer()
{
  echo "$(curl -s -o /dev/null -w %{http_code} "localhost:$1/__admin/mappings?limit=1")"
}

# Poll mock server running on the provided port: ex. pollMockServer 8081
pollMockServer()
{
  local timeout=30 # seconds
  local waitPeriod=2 # seconds
  local timeElapsed=0 # seconds

  while [ "$(pingMockServer $1)" != "200" ];
  do
    if [ $timeElapsed -ge $timeout ]; then
      echo "ERROR Port $1: mock server not up after ${timeout}s, make sure docker has enough resources."
      exit 1
    fi

    echo "Port $1: waiting for mock server"
    ((timeElapsed+=$waitPeriod))
    sleep $waitPeriod;
  done

  echo "Port $1: mock server is UP"
}

# Get the wiremock application port for the provided container name: ex. getPortForContainer mock-1
getPortForContainer()
{
  echo $(docker inspect --format '{{ index .Config.Labels "com.querymock.port"}}' $1)
}

# Poll the mock server running on each of the provided containers.
# Accept container names as space-delimited arguments: ex. ./waitForMocks.sh mock-1 mock-2 mock-3
while [ "$1" != "" ];
do
  port=$(getPortForContainer $1)
  pollMockServer $port
  shift
done
