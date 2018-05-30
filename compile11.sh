#!/bin/bash
javac -d classes/ src/11task/ru/ifmo/rain/brilyantov/rmi/*.java
javac -cp ./classes/:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar -d classes/ test/ru/ifmo/rain/brilyantov/rmi/tests/*.java