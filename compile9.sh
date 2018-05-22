#!/bin/bash
LIBS=./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar
JARS=./artifacts/WebCrawlerTest.jar:./classes:./artifacts/JarImplementorTest.jar
bash compile4.sh
javac -d classes/ -cp ${JARS}:${LIBS} src/java/ru/ifmo/rain/brilyantov/crawler/*.java
