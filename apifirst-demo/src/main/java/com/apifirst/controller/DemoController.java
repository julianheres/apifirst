package com.apifirst.controller;

import com.zhourui.retail.petstore.consumer.api.v1.StoreApi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController

public class DemoController {
    @Autowired
    private StoreApi storeApi;

    @GetMapping(value = "/clientdemo")
    public ResponseEntity<Map<String, Integer>> clientDemo() {
        Map<String, Integer> m = storeApi.getInventory();
        return new ResponseEntity<>(m,OK);
    }
}
