#!/bin/bash
bash compile10.sh
LIBS=./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar
JARS=./artifacts/ParallelMapperTest.jar
java -cp ./classes:${JARS}:${LIBS} ru.ifmo.rain.brilyantov.helloudp.HelloUDPServer "7211" "4";