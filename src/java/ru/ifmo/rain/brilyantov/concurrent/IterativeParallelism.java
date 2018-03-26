package ru.ifmo.rain.brilyantov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP, ScalarIP {

    private <T> Function<? super Stream<List<T>>, List<T>> merge() {
        return list -> list.flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.map(Object::toString).collect(Collectors.joining()),
                list -> list.map(Object::toString).collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.filter(predicate).collect(Collectors.toList()),
                merge()
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.map(f).collect(Collectors.toList()),
                merge()
        );
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> getMin = list -> list.max(comparator).get();
        return runInParallel(threads, values, getMin, getMin);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.allMatch(predicate),
                list -> list.allMatch(it -> it)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.anyMatch(predicate),
                list -> list.anyMatch(it -> it)
        );
    }

    private <T, R> R runInParallel(
            int threads,
            List<? extends T> values,
            Function<Stream<? extends T>, R> mapper,
            Function<? super Stream<R>, R> reducer
    ) throws InterruptedException {
        return reduce(
                map(
                        threads,
                        partition(threads, values),
                        mapper
                ).stream(),
                reducer
        );
    }

    private <T> List<Stream<? extends T>> partition(int threads, List<? extends T> values) {
        List<Stream<? extends T>> partitionedList = new ArrayList<>();
        int blockSize = values.size() / threads + (values.size() % threads != 0 ? 1 : 0);
        for (int leftBound = 0; leftBound < values.size(); leftBound += blockSize) {
            int rightBound = Math.min(leftBound + blockSize, values.size()) - 1;
            partitionedList.add(values.subList(leftBound, rightBound).stream());
        }
        return partitionedList;
    }

    private <T, R> List<R> map(
            List<Stream<? extends T>> inputValues,
            Function<Stream<? extends T>, R> mapper
    ) throws InterruptedException {
        List<R> intermediateValues = new ArrayList<>(Collections.nCopies(inputValues.size(), null));
        List<Thread> workingThreads = new ArrayList<>();
        for (int i = 0; i < intermediateValues.size(); i++) {
            final int index = i;
            Thread thread = new Thread(() -> intermediateValues.set(
                    index,
                    mapper.apply(inputValues.get(index))
            ));
            workingThreads.add(thread);
            thread.start();
        }
        for (Thread thread : workingThreads) {
            thread.join();
        }
        return intermediateValues;
    }

    private <R> R reduce(
            Stream<R> intermediateValues,
            Function<? super Stream<R>, R> reducer
    ) throws InterruptedException {
        return reducer.apply(intermediateValues);
    }

}
