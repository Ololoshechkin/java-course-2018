package ru.ifmo.rain.brilyantov.studentdb;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private Comparator<Student> defaultStudentComparator = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);


    private <T extends Comparable<T>> Stream<Group> groupsStreamByComparator(
            Collection<Student> students,
            Comparator<Student> studentComparator
    ) {
        return students
                .stream()
                .collect(Collectors.toMap(
                        Student::getGroup,
                        Stream::of,
                        Stream::concat
                ))
                .entrySet()
                .stream()
                .map(it -> new Group(
                        it.getKey(),
                        it.getValue()
                                .sorted(studentComparator)
                                .collect(Collectors.toList()))
                );
    }

    private <T extends Comparable<T>> List<Group> getGroupsWithComparator(
            Collection<Student> students,
            Comparator<Student> studentComparator
    ) {
        return groupsStreamByComparator(students, studentComparator)
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    private <T extends Comparable<T>> Optional<Group> getLargestGroupComparingByStudentsList(
            Collection<Student> students,
            Function<List<Student>, Integer> studentsCmp
    ) {
        return groupsStreamByComparator(
                students,
                defaultStudentComparator
        ).min(Comparator
                .comparing((Group g) -> studentsCmp.apply(g.getStudents()))
                .reversed()
                .thenComparing(Group::getName)
        );
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsWithComparator(students, defaultStudentComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsWithComparator(students, Comparator.comparing(Student::getId));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupComparingByStudentsList(students, List::size)
                .map(Group::getName)
                .orElse("");
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupComparingByStudentsList(
                students,
                studs -> getDistinctFirstNames(studs).size()
        ).map(Group::getName).orElse("");
    }

    private List<String> mapToFieldsList(Collection<Student> students, Function<Student, String> f) {
        return students
                .stream()
                .map(f)
                .collect(Collectors.toList());
    }

    private Set<String> mapToFieldsSet(Collection<Student> students, Function<Student, String> f) {
        return students
                .stream()
                .map(f)
                .distinct()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToFieldsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToFieldsList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapToFieldsList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToFieldsList(students, it -> it.getFirstName() + " " + it.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToFieldsSet(students, Student::getFirstName);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students
                .stream()
                .min(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students
                .stream()
                .sorted(defaultStudentComparator)
                .collect(Collectors.toList());
    }

    private List<Student> filterStudentsByField(Collection<Student> students, Function<Student, String> field, String name) {
        return students
                .stream()
                .filter(s -> Objects.equals(field.apply(s), name))
                .sorted(defaultStudentComparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterStudentsByField(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterStudentsByField(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return students
                .stream()
                .filter(it -> Objects.equals(it.getGroup(), group))
                .sorted(defaultStudentComparator)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students
                .stream()
                .filter(it -> Objects.equals(it.getGroup(), group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)
                ));
    }

}
