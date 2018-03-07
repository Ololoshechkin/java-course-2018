package ru.ifmo.rain.brilyantov.studentdb;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private Comparator<Student> defaultStudentComparator = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getGroup)
            .thenComparing(Student::getId);

    private <T extends Comparable<T>> List<Group> groupsByFieldWithTransform(
            Collection<Student> students,
            Comparator<Student> studentComparator,
            Function<Stream<Group>, Stream<Group>> transform
    ) {
        return transform
                .apply(students
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
                        )
                )
                .collect(Collectors.toList());
    }

    private <T extends Comparable<T>> List<Group> getGroupsByField(
            Collection<Student> students,
            Comparator<Student> studentComparator
    ) {
        return groupsByFieldWithTransform(students, studentComparator, Function.identity());
    }

    private <T extends Comparable<T>> Group getLargestGroupWithTransform(
            Collection<Student> students,
            Function<Stream<Group>, Stream<Group>> transform
    ) {
        return groupsByFieldWithTransform(
                students,
                defaultStudentComparator,
                groups -> transform
                        .apply(groups)
                        .sorted(Comparator.comparing(it -> -it.getStudents().size()))
        ).get(0);
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsByField(students, defaultStudentComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsByField(students, Comparator.comparing(Student::getId));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupWithTransform(students, Function.identity()).getName();
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupWithTransform(students, groups -> groups
                .map(g -> {
                    HashSet<String> firstNames = new HashSet<>();
                    return new Group(
                            g.getName(),
                            g.getStudents().stream().filter(it -> {
                                boolean result = !firstNames.contains(it.getFirstName());
                                firstNames.add(it.getFirstName());
                                return result;
                            }).collect(Collectors.toList())
                    );
                })
        ).getName();
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
                .collect(Collectors.toSet());
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
                .get()
                .getFirstName();
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
                        (name1, name2) ->
                                name1.compareTo(name2) < 0 ? name1 : name2
                ));
    }

}
