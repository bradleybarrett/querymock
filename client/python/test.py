from querymock import startMock, waitForMocks, stopMocks

def main():
    mock1 = "mock-1"
    mock2 = "mock-2"
    mock3 = "mock-3"

    startMock(mock1, 8090, 8091, "resourceDir", "/wiremock")
    startMock(mock2, 8092, 8093, "resourceDir", "/wiremock")
    startMock(mock3, 8094, 8095, "resourceDir", "/wiremock")
    waitForMocks([mock1, mock2, mock3])

    startMock(mock1, 8090, 8091, "resourceDir", "/wiremock/2")
    waitForMocks([mock1])

    stopMocks([mock1, mock2, mock3])


if __name__ == "__main__":
    main()