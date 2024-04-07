package com.apifirst.controller;

import com.zhourui.retail.petstore.producer.api.v1.StoreApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class StoreController implements StoreApi {
    @Override
    public ResponseEntity<Map<String, Integer>> getInventory() {
        // your code
        Map<String, Integer> m = Map.of(
                "sugar", 1000,
                "wheat", 250
        );
        return new ResponseEntity<>(m, HttpStatus.OK);
    }
}
