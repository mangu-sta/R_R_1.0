package com.release.rr.domain.test.controller;

import com.release.rr.global.redis.dao.LoginRedisDao;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/redis-test")
public class RedisTestController {

    private final LoginRedisDao loginRedisDao;

    @GetMapping("/set")
    public String testSet() {
        loginRedisDao.setValues("test", "testUser", Duration.ofMinutes(5));
        return "OK";
    }

    @GetMapping("/get")
    public String testGet() {
        Object value = loginRedisDao.getValues("test");
        return "값 = " + value;
    }

    
}
