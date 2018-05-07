#!/bin/bash
bash compile9.sh
LIBS=./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar
JARS=./artifacts/WebCrawlerTest.jar:./artifacts/IterativeParallelismTest.jar
java -cp ./classes/:${JARS}:${LIBS} info.kgeorgiy.java.advanced.crawler.Tester hard ru.ifmo.ctddev.kgeorgiy.crawler.WebCrawler "$1";