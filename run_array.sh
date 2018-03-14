#!bin/bash
bash compile_array.sh
echo $1
java -cp ./classes:./artifacts/StudentTest.jar:./artifacts/ArraySetTest.jar:./artifacts/WalkTest.jar:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar info.kgeorgiy.java.advanced.arrayset.Tester NavigableSet ru.ifmo.rain.brilyantov.arrayset.ArraySet $1