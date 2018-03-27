#!/bin/bash
JAR_TESTER="artifacts/JarImplementorTest.jar"
MY_PACKAGE="ru/ifmo/rain/brilyantov/implementor"
KGEORGY_PACKAGE="info/kgeorgiy/java/advanced/implementor"

javac -d production/ -cp ./${JAR_TESTER} src/java/${MY_PACKAGE}/Implementor.java
cd production/
jar xvf ../${JAR_TESTER} ${KGEORGY_PACKAGE}/Impler.class ${KGEORGY_PACKAGE}/JarImpler.class ${KGEORGY_PACKAGE}/ImplerException.class
jar cfe Implementor.jar ru.ifmo.rain.brilyantov.implementor.Implementor ${MY_PACKAGE}/*.class ${KGEORGY_PACKAGE}/*.class
cd ..