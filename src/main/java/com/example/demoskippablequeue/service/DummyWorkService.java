package com.example.demoskippablequeue.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class DummyWorkService {

    @SneakyThrows
    public void doSomeWork(String subject, UUID uuid) {
        log.info("doing some work for subject: {} with id: {}", subject, uuid);
        Thread.sleep(10000);
    }
}
