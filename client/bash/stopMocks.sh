#!/bin/bash
# example usage: ./stopMocks.sh mock-1
# example usage: ./stopMocks.sh mock-1 mock-2 mock-3

# Stop and remove the container for each provided container name.
# Accept the container names as space-delimited arguments.
echo "Stopping mocks:"
while [ "$1" != "" ];
do
  docker rm -f $1
  shift
done