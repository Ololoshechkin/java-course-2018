#!/bin/bash
bash compile8.sh
java -cp ./classes:./artifacts/ParallelMapperTest.jar:./artifacts/IterativeParallelismTest.jar:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar info.kgeorgiy.java.advanced.mapper.Tester scalar ru.ifmo.rain.brilyantov.concurrent.ParallelMapperImpl,ru.ifmo.rain.brilyantov.concurrent.IterativeParallelism "$1";
