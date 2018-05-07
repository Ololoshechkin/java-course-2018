#!/bin/bash
bash compile7.sh
LIBS=./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar
JARS=./artifacts/ParallelMapperTest.jar
javac -d classes/ -cp classes/:${LIBS}:${JARS} src/java/ru/ifmo/rain/brilyantov/helloudp/*.java