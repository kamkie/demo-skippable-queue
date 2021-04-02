package com.example.demoskippablequeue.component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.Executors;

@Slf4j
class SubjectAwareExecutorTest {

    @Test
    void execute() {
        SubjectAwareExecutor subjectAwareExecutor = new SubjectAwareExecutor(Executors.newFixedThreadPool(2));

        subjectAwareExecutor.execute("aa", () -> run(UUID.randomUUID()));
        subjectAwareExecutor.execute("aa", () -> run(UUID.randomUUID()));
        subjectAwareExecutor.execute("aa", () -> run(UUID.randomUUID()));
        subjectAwareExecutor.execute("bb", () -> run(UUID.randomUUID()));
        subjectAwareExecutor.execute("bb", () -> run(UUID.randomUUID()));
        subjectAwareExecutor.execute("aa", () -> run(UUID.randomUUID()));

        subjectAwareExecutor.join();
    }

    @SneakyThrows
    private static void run(UUID uuid) {
        log.info("executing some task {}", uuid);
        Thread.sleep(5000);
        log.info("done some some task {}", uuid);
    }
}
