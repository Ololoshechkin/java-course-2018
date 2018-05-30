#!/bin/bash
export CLASSPATH=classes/
./compile11.sh
killall -9 rmiregistry
killall -9 java
rmiregistry &
java -cp classes/ ru.ifmo.rain.brilyantov.rmi.Server
