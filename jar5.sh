javac -d production/ -cp ./artifacts/JarImplementorTest.jar:./lib/* src/java/ru/ifmo/rain/brilyantov/implementor/Implementor.java
cd production/
jar cfm Implementor.jar ../Manifest.txt ru/ifmo/rain/brilyantov/implementor/Implementor.class
cd ..