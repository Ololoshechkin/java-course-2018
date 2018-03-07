#!bin/bash
bash compile.sh
java -cp ./classes:./artifacts/StudentTest.jar:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar ru.ifmo.rain.brilyantov.studentdb.MainClass $1 $2