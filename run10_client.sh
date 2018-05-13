#!/bin/bash
bash compile10.sh
LIBS=./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar
JARS=./artifacts/ParallelMapperTest.jar:./artifacts/HelloUDPTest.jar
java -cp ./classes:${JARS}:${LIBS} ru.ifmo.rain.brilyantov.helloudp.HelloUDPClient "172.20.10.10" "7211" "Kotlin" "100" "100";