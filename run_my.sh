#!/bin/bash
bash compile4.sh
java -cp ./classes:./artifacts/JarImplementorTest.jar:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar ru.ifmo.rain.brilyantov.implementor.Implementor -jar "java.lang.Thread" "JARS/file.jar" ;