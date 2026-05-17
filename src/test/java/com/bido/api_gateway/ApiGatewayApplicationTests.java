package com.bido.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

	@MockitoBean(name = "reactiveRedisTemplate")
	ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate;

	@MockitoBean(name = "reactiveStringRedisTemplate")
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

	@MockitoBean(name = "reactiveRedisRouteDefinitionTemplate")
    ReactiveRedisTemplate<String, Object> reactiveRedisRouteDefinitionTemplate;

	@Test
	void contextLoads() {}

}
