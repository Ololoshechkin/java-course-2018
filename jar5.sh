javac -d production/ -cp ./classes:./artifacts/JarImplementorTest.jar:./lib/hamcrest-core-1.3.jar:./lib/jsoup-1.8.1.jar:./lib/junit-4.11.jar:./lib/quickcheck-0.6.jar src/java/ru/ifmo/rain/brilyantov/implementor/Implementor.java
cd production/
jar cfm Implementor.jar ../Manifest.txt ru/ifmo/rain/brilyantov/implementor/Implementor.class
cd ..