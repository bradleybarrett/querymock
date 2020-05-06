#!/bin/bash

./startMock.sh \
-cname mock-1 \
-p 8091 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/src/main/resources/wiremock

./startMock.sh \
-cname mock-2 \
-p 8092 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/src/main/resources/wiremock

./startMock.sh \
-cname mock-3 \
-p 8093 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/src/main/resources/wiremock

./waitForMocks.sh mock-1 mock-2 mock-3

./stopMocks.sh mock-1 mock-2 mock-3