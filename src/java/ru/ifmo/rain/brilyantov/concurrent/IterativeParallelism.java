package ru.ifmo.rain.brilyantov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP, ScalarIP {

    private ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        this(null);
    }

    private <T> List<T> merge(Stream<? extends Stream<? extends T>> list) {
        return list.flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.map(Object::toString).collect(Collectors.joining()),
                list -> list.collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.filter(predicate),
                this::merge
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.map(f),
                this::merge
        );
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> getMin = list -> list.min(comparator).get();
        return runInParallel(threads, values, getMin, getMin);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.allMatch(predicate),
                list -> list.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runInParallel(
                threads,
                values,
                list -> list.anyMatch(predicate),
                list -> list.anyMatch(Boolean::booleanValue)
        );
    }

    private <T, M, R> R runInParallel(
            int threads,
            List<? extends T> values,
            Function<Stream<? extends T>, M> mapFunction,
            Function<? super Stream<M>, R> reduceFunction
    ) throws InterruptedException {
        return reduceTask(
                reduceFunction,
                mapper != null
                        ? mapper.map(mapFunction, partition(threads, values)).stream()
                        : mapTask(mapFunction, partition(threads, values))
        );
    }

    private <T> List<Stream<? extends T>> partition(int threads, List<? extends T> values) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Expected > 0 number of threads, but found : " + threads);
        }
        List<Stream<? extends T>> partitionedList = new ArrayList<>();
        int blockSize = values.size() / threads + (values.size() % threads != 0 ? 1 : 0);
        for (int leftBound = 0; leftBound < values.size(); leftBound += blockSize) {
            int rightBound = Math.min(leftBound + blockSize, values.size());
            partitionedList.add(values.subList(leftBound, rightBound).stream());
        }
        return partitionedList;
    }

    private <T, M> Stream<M> mapTask(
            Function<Stream<? extends T>, M> mapper,
            List<Stream<? extends T>> inputValues
    ) throws InterruptedException {
        List<M> intermediateValues = new ArrayList<>(Collections.nCopies(inputValues.size(), null));
        List<Thread> workingThreads = new ArrayList<>();
        ParallelMapperImpl.startThreads(inputValues.size(), workingThreads, i -> () -> intermediateValues.set(
                i, mapper.apply(inputValues.get(i))
        ));
        ParallelMapperImpl.endThreads(workingThreads);
        return intermediateValues.stream();
    }

    private <M, R> R reduceTask(
            Function<? super Stream<M>, R> reduceFunction,
            Stream<M> intermediateValues
    ) {
        return reduceFunction.apply(intermediateValues);
    }

}
