from typing import List
from time import sleep
import subprocess
import requests

adminPortLabel = "com.querymock.port.admin"
wiremockPortLabel = "com.querymock.port.wiremock"

class Error(Exception):
    """Base class for exceptions in this module."""
    pass


class ReconfigurationError(Error):
    def __init__(self, url, message):
        self.url = url
        self.message = message


class WaitTimeoutError(Error):
    def __init__(self, names, message):
        self.names = names
        self.message = message


def _getUrl(port: int, endpointPath: str) -> str:
    return "http://localhost:" + str(port) + endpointPath


def _startNewMock(name: str, adminPort: int, wiremockPort: int, resourceDir: str, wiremockSubDir: str, image: str):
    containerAdminPort = 8080
    containerWiremockPort = 8081
    containerResourceDirectory = "/wiremock"

    print(f'Start new mock with name: {name}, adminPort: {adminPort}, wiremockPort: {wiremockPort}')
    subprocess.run(["docker", "run", "-d",
        "--name", name,
        "-l", adminPortLabel + "=" + str(adminPort),
        "-l", wiremockPortLabel + "=" + str(wiremockPort),
        "-p", str(adminPort) + ":" + str(containerAdminPort),
        "-p", str(wiremockPort) + ":" + str(containerWiremockPort),
        "-v", resourceDir + ":" + containerResourceDirectory,
        image,
        "--resource.directory=" + containerResourceDirectory,
        "--resource.subdirectory.wiremock=" + wiremockSubDir])


def _getPortForMock(name: str, portLabel: str) -> int:
    inspectTemplate = "{{ index .Config.Labels \"" + portLabel + "\" }}"
    completedProcess = subprocess.run(["docker", "inspect", "--format", inspectTemplate, name], capture_output=True)
    port = completedProcess.stdout.splitlines()[0].decode()
    try:
        return int(port)
    except ValueError:
        return None


def _mockServerIsUp(name: str) -> bool:
    url = _getUrl(_getPortForMock(name, wiremockPortLabel), "/__admin/mappings?limit=1")
    try:
        return requests.get(url).status_code == 200
    except:
        return False


def _reconfigureMock(adminPort: int, wiremockSubDir: str):
    print(f'Reconfigure mock running on adminPort: {adminPort} to use wiremockSubDir: {wiremockSubDir}')
    url = _getUrl(adminPort, "/querymock/reconfigure?subdirectory=" + wiremockSubDir)
    reponse = requests.get(url)

    if reponse.status_code != 200:
        raise ReconfigurationError(url, 
            f'Mock is server is running on {adminPort}, but response has status {reponse.status_code} for GET request to url {url}')


def startMockWithImage(name: str, adminPort: int, wiremockPort: int, resourceDir: str, wiremockSubDir: str, image: str):
    # check if a mock exists with name and port mappings
    existingAdminPort = _getPortForMock(name, adminPortLabel)
    existingWiremockPort = _getPortForMock(name, wiremockPortLabel)
    if (existingAdminPort is None and existingWiremockPort is None):
        # start a new instance
        _startNewMock(name, adminPort, wiremockPort, resourceDir, wiremockSubDir, image)
    
    # check if the existing mock has the requested ports
    elif (adminPort != existingAdminPort or wiremockPort != existingWiremockPort):
        # stop the existing instance because the new instance requires different ports
        stopMocks([name])
        _startNewMock(name, adminPort, wiremockPort, resourceDir, wiremockSubDir, image)
    
    else: # a mock instance exists with all the requested ports
        # reconfigure the existing instance
        _reconfigureMock(adminPort, wiremockSubDir)


def startMock(name: str, adminPort: int, wiremockPort: int, resourceDir: str, wiremockSubDir: str):
    startMockWithImage(name, adminPort, wiremockPort, resourceDir, wiremockSubDir, "bbarrett/querymock:latest")


def waitForMocksWithTimeout(timeout: float, waitPeriod: float, names: List[str]):
    timeElapsed = 0.0 # sec
    for name in names:
        print(f'Waiting for mock: {name}')
        while not _mockServerIsUp(name):
            if timeElapsed >= timeout:
                raise WaitTimeoutError(names, 
                        f'Mock server {name} not up after {timeout}s, make sure docker has enough resources.')
            timeElapsed += waitPeriod
            sleep(waitPeriod)
        print(f'Mock is up: {name}')


def waitForMocks(names: List[str]):
    timeout = 30.0 # sec
    waitPeriod = 2.0 # sec
    waitForMocksWithTimeout(timeout, waitPeriod, names)


def stopMocks(names: List[str]):
    for name in names:
        print(f'Stoping mock: {name}')
        subprocess.run(["docker", "rm", "-f", name])