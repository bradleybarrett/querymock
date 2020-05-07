#!/bin/bash

./startMock.sh \
-cname mock-1 \
-ap 8090 \
-wp 8091 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/app/querymock-app/src/test/resources/query \
-wdir /test1/wiremock

./waitForMocks.sh mock-1

./startMock.sh \
-cname mock-1 \
-ap 8090 \
-wp 8091 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/app/querymock-app/src/test/resources/query \
-wdir /test1/wiremock

./startMock.sh \
-cname mock-2 \
-ap 8092 \
-wp 8093 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/app/querymock-app/src/test/resources/query \
-wdir /test1/wiremock

./startMock.sh \
-cname mock-3 \
-ap 8094 \
-wp 8095 \
-rdir /Users/bbarrett/git-repos/cloned-repos/querymock/app/querymock-app/src/test/resources/query \
-wdir /test1/wiremock

./waitForMocks.sh mock-1 mock-2 mock-3

./stopMocks.sh mock-1 mock-2 mock-3