#!/bin/bash
# example usage: ./startMock.sh -cname my-service-mock -ap 8085 -wp 8086 -rdir /absolute/path/to/resources -wdir /wiremock

image=bbarrett/querymock:latest
containerName=service-mock
adminPort=8090
wiremockPort=8091
resourceDir=/absolute/path/to/resources
wiremockSubDir=/wiremock

while [ "$1" != "" ]; do
    case $1 in
        -i | --image )
            shift
            image=$1
            ;;
        -cname | --containerName )
            shift
            containerName=$1
            ;;
        -ap | --adminPort )
            shift
            adminPort=$1
            ;;
        -wp | --wiremockPort )
            shift
            wiremockPort=$1
            ;;
        -rdir | --resourceDir )
            shift
            resourceDir=$1
            ;;
        -wdir | --wiremockSubDir )
            shift
            wiremockSubDir=$1
            ;;
        * )
            exit 1
    esac
    shift
done

containerAdminPort=8080
containerWiremockPort=8081
containerResourceDir=/wiremock

adminPortLabel=com.querymock.admin.port
wiremockPortLabel=com.querymock.wiremock.port

# Use -v to create bindMount so that the directory is created in the container if it does not already exist.
# Refer: https://docs.docker.com/storage/bind-mounts/#differences-between--v-and---mount-behavior
startNewMock()
{
  echo "startNewMock() for $containerName"
  docker run -d \
  --name=$containerName \
  -l "$adminPortLabel=$adminPort" \
  -l "$wiremockPortLabel=$wiremockPort" \
  -p $adminPort:$containerAdminPort \
  -p $wiremockPort:$containerWiremockPort \
  -v $resourceDir:$containerResourceDir \
  $image \
  --resource.directory=$containerResourceDir \
  --resource.subdirectory.wiremock=$wiremockSubDir
}

# Reconfigure mock server on the provided admin port: ex. reconfigureMock 8081 /wiremock/sub/directory
reconfigureMock()
{
  echo "reconfigureMock() on port $1 with wiremock subdirectory $2"
  echo "$(curl -s -o /dev/null -w %{http_code} "localhost:$1/querymock/reconfigure?subdirectory=$2")"
}

# Get the wiremock application port for the provided container name: ex. getPortForContainer mock-1 portLabel
getAdminPortForMock()
{
  echo $(docker inspect --format '{{ index .Config.Labels "com.querymock.admin.port"}}' $1)
}

getWiremockPortForMock()
{
  echo $(docker inspect --format '{{ index .Config.Labels "com.querymock.wiremock.port"}}' $1)
}

# example usage: ./stopMocks.sh mock-1 mock-2 mock-3

# Stop the existing mock with the provided containerName
stopExistingMock()
{
  echo "stopExistingMock() for $containerName"
  docker rm -f $containerName
}

# Start the mock instance and reuse an existing instance if possible.
existingAdminPort=$(getAdminPortForMock $containerName);
existingWiremockPort=$(getWiremockPortForMock $containerName);

if [ -z "$existingAdminPort" ] && [ -z "$existingWiremockPort"]
then
  # Start a new mock - no mock exists with the same name and port mappings
  startNewMock
elif [ "$existingAdminPort" -ne $adminPort ] || [ "$existingWiremockPort" -ne $wiremockPort ]
then
  # Stop the existing mock and start a new mock - the new mock requires different ports than the existing mock
  stopExistingMock
  startNewMock
else
  # Reconfigure the existing instance
  reconfigureMock $adminPort $wiremockSubDir
fi