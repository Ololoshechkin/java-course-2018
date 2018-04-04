package ru.ifmo.rain.brilyantov.concurrent;


import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> threadPool = new ArrayList<>();
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    public static void startThreads(int threadCount, List<Thread> threads, Function<Integer, Runnable> taskGen) {
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(taskGen.apply(i));
            threads.add(thread);
            thread.start();
        }
    }

    public static void endThreads(List<Thread> threads) throws InterruptedException {
        boolean failed = false;
        InterruptedException exception = new InterruptedException("At least 1 of threads failed to join");
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                exception.addSuppressed(e);
                failed = true;
            }
        }
        if (failed) {
            throw exception;
        }
    }

    public static class ReversedCounter {
        private int value;

        public ReversedCounter(int value) {
            this.value = value;
        }

        public void dec() {
            value--;
        }

        public boolean isZero() {
            return value == 0;
        }

        public boolean decIsZero() {
            dec();
            return isZero();
        }
    }

    public ParallelMapperImpl(int threads) {
        startThreads(threads, threadPool, (i) -> () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Runnable task;
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                    }
                    task.run();
                }
            } catch (InterruptedException ignored) {
            }
        });
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final List<R> mappedValues = new ArrayList<>(Collections.nCopies(args.size(), null));
        final ReversedCounter cnt = new ReversedCounter(args.size());
        synchronized (tasks) {
            for (int i = 0; i < args.size(); i++) {
                final int index = i;
                tasks.add(() -> {
                            mappedValues.set(index, f.apply(args.get(index)));
                            synchronized (cnt) {
                                if (cnt.decIsZero()) {
                                    cnt.notify();
                                }
                            }
                        }
                );
                tasks.notify();
            }
        }
        synchronized (cnt) {
            while (cnt.isZero()) {
                cnt.wait();
            }
        }
        return mappedValues;
    }

    @Override
    public void close() {
        threadPool.forEach(Thread::interrupt);
        try {
            endThreads(threadPool);
        } catch (InterruptedException ignored) {
        }
    }

}
