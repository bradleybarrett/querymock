#!/bin/bash
# example usage: ./startMock.sh -cname my-service-mock -p 8085 -rdir /absolute/path/to/resources

image=bbarrett/querymock:latest
containerName=service-mock
hostPort=8090
hostResourceDir=/absolute/path/to/resources

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
        -p | --hostPort )
            shift
            hostPort=$1
            ;;
        -rdir | --hostResourceDir )
            shift
            hostResourceDir=$1
            ;;
        * )
            exit 1
    esac
    shift
done

containerPort=8081
containerResourceDir=/wiremock

# Use -v to create bindMount so that the directory is created in the container if it does not already exist.
# Refer: https://docs.docker.com/storage/bind-mounts/#differences-between--v-and---mount-behavior
docker run -it -d \
  --name=$containerName \
  -l "com.querymock.port=$hostPort" \
  -p $hostPort:$containerPort \
  -v $hostResourceDir:$containerResourceDir \
  $image \
  --wiremock.directory=$containerResourceDir
