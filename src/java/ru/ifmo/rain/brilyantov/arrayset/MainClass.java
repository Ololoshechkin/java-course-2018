package ru.ifmo.rain.brilyantov.arrayset;

import java.util.ArrayList;
import java.util.Comparator;

public class MainClass {
    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(3);
        list.add(2);
        list.add(0);
        list.add(-1);
        list.add(4);
        ArraySet<Integer> s = new ArraySet<Integer>(list);//, (o1, o2) -> 0);
        for (Integer x : s) {
            System.out.println(x);
        }
        System.out.println("ceiling(2) (>=) :" + s.ceiling(2));
        System.out.println("floor(2) (<=) :" + s.floor(2));
        System.out.println("higher(2) (>)  :" + s.higher(2));
        System.out.println("lower(2) (<) :" + s.lower(2));
        System.out.println("tailSet : ");
        for (Integer x : s.tailSet(2)) {
            System.out.println(x);
        }
    }

}
