#!/usr/bin/env bash
./compile11.sh
#killall -9 rmiregistry
#killall -9 java
#rmiregistry &
java -cp ./classes:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar org.junit.runner.JUnitCore ru.ifmo.rain.brilyantov.rmi.tests.PersonTests