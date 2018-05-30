#!/bin/bash
./compile11.sh
java -cp classes/ ru.ifmo.rain.brilyantov.rmi.Client $@
