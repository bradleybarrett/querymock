package com.bbarrett.querymock.controller;

import com.bbarrett.querymock.wiremock.WiremockInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/querymock")
public class QueryMockController
{
    private WiremockInstance wiremockInstance;

    @Autowired
    public QueryMockController(WiremockInstance wiremockInstance)
    {
        this.wiremockInstance = wiremockInstance;
    }

    @GetMapping("/reconfigure")
    public ResponseEntity<Object> reconfigureMockServer(@RequestParam(value = "subdirectory") String subdirectory)
    {
        wiremockInstance.reconfigureMocks(wiremockInstance.getWiremockConfigProperties().getDirectory(), subdirectory);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
