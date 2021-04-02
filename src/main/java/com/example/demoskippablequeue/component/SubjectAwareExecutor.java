package com.example.demoskippablequeue.component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class SubjectAwareExecutor implements DisposableBean {

    private final ExecutorService workerExecutor;
    private final ExecutorService deflectorExecutor = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String, Bucket> workTracking = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(true);


    public SubjectAwareExecutor(@Qualifier("workExecutor") ExecutorService workerExecutor) {
        this.workerExecutor = workerExecutor;
        deflectorExecutor.execute(this::startTasks);
    }

    @SneakyThrows(InterruptedException.class)
    public void execute(String subject, Runnable runnable) {
        workTracking.putIfAbsent(subject, new Bucket());
        Bucket bucket = workTracking.get(subject);
        Semaphore semaphore = bucket.semaphore;
        boolean acquired = semaphore.tryAcquire();
        if (!acquired) {
            enqueue(subject, bucket, runnable);
            return;
        }

        executeOnPool(subject, semaphore, runnable);
    }

    private void executeOnPool(String subject, Semaphore semaphore, Runnable runnable) {
        try {
            log.debug("scheduling task for subject {}", subject);
            workerExecutor.execute(() -> {
                try {
                    runnable.run();
                } finally {
                    semaphore.release();
                }
            });
        } catch (Exception e) {
            log.warn("exception starting task with subject {}", subject, e);
            semaphore.release();
        }
    }

    @SneakyThrows(InterruptedException.class)
    private void startTasks() {
        while (isRunning.get()) {
            if (workTracking.isEmpty()) {
                Thread.sleep(100);
            }
            workTracking.forEach((subject, bucket) -> {
                Semaphore semaphore = bucket.semaphore;
                boolean acquired = semaphore.tryAcquire();
                if (acquired) {
                    try {
                        Runnable runnable = bucket.tasks.take();
                        executeOnPool(subject, semaphore, runnable);
                    } catch (Exception e) {
                        log.warn("exception starting task with subject {}", subject, e);
                        semaphore.release();
                    }
                }
            });
            Thread.sleep(10);
        }
    }

    private void enqueue(String subject, Bucket bucket, Runnable runnable) throws InterruptedException {
        log.debug("enqueuing task with subject {}", subject);
        bucket.tasks.put(runnable);
    }

    @Override
    public void destroy() throws Exception {
        isRunning.set(false);
        workerExecutor.shutdown();
    }

    @SneakyThrows(InterruptedException.class)
    public void join() {
        deflectorExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static class Bucket {
        private final Semaphore semaphore;
        private final BlockingQueue<Runnable> tasks;

        public Bucket() {
            this.semaphore = new Semaphore(1);
            this.tasks = new ArrayBlockingQueue<>(100);
        }
    }
}
