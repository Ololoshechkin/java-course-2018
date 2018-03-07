package ru.ifmo.rain.brilyantov.studentdb;

import info.kgeorgiy.java.advanced.student.Student;

import java.util.Arrays;

public class MainClass {
    public static void main(String[] args) {
        Student[] ss = {
                new Student(3, "Александр", "Абрамов", "M3138"),
                new Student(1, "Анвер", "Амиров", "M3138")
        };
        System.out.println(new StudentDB()
                .findStudentNamesByGroup(Arrays.asList(ss), "M3138")
        );
    }
}
