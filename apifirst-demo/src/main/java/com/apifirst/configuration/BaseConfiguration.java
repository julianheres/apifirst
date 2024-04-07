package com.apifirst.configuration;

import com.zhourui.retail.petstore.consumer.api.v1.StoreApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(
        value = {StoreApi.class, com.zhourui.retail.petstore.consumer.api.ApiClient.class}
)
public class BaseConfiguration {
    @Value("${ApiFirstDemo.API.PetStoreUrl}")
    private String petStoreUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public StoreApi storeApi() {
        StoreApi storeApi = new StoreApi();
        storeApi.getApiClient().setBasePath(petStoreUrl);
        return storeApi;
    }
}
