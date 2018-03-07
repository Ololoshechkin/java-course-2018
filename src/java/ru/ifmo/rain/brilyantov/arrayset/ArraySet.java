package ru.ifmo.rain.brilyantov.arrayset;

import com.sun.javafx.UnmodifiableArrayList;

import java.util.*;
import java.util.function.Consumer;

//71LDAK


public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private List<T> data;
    private Comparator<? super T> cmp = null;

    public ArraySet() {
        this(Collections.emptyList(), null, false);
    }

    public ArraySet(Comparator<? super T> cmp) {
        this(Collections.emptyList(), cmp, false);
    }

    public ArraySet(Collection<T> data) {
        this(data, null, true);
    }

    public ArraySet(Collection<T> data, Comparator<? super T> comp) {
        this(data, comp, true);
    }

    private ArraySet(Collection<T> data, Comparator<? super T> comp, boolean shouldSort) {
        List<T> sortedData;
        if (shouldSort) {
            TreeSet<T> s = new TreeSet<>(comp);
            s.addAll(data);
            sortedData = new ArrayList<>(s);
        } else if (data instanceof List) {
            sortedData = (List<T>) data;
        } else {
            sortedData = new ArrayList<>(data);
        }
        this.data = Collections.unmodifiableList(sortedData);
        this.cmp = comp;
    }

    private int abstractFindIndex(T t, int delta1, int delta2) {
        int pos = Collections.binarySearch(data, t, cmp);
        return pos >= 0
                ? pos + delta1 // delta1 = 0 => eqal pos
                : ~pos + delta2; // delta2 = -1 =>  last <= pos
    }

    private T findAndGetLower(T t, boolean inclusive) {
        int i = abstractFindIndex(t, inclusive ? 0 : -1, -1);
        return i >= 0 ? data.get(i) : null;
    }

    private T findAndGetUpper(T t, boolean inclusive) {
        int i = abstractFindIndex(t, inclusive ? 0 : 1, 0);
        return i < data.size() ? data.get(i) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(data, o, (Comparator<Object>) cmp) >= 0;
    }

    @Override
    public T lower(T t) {
        return findAndGetLower(t, false);
    }

    @Override
    public T floor(T t) {
        return findAndGetLower(t, true);
    }

    @Override
    public T ceiling(T t) {
        return findAndGetUpper(t, true);
    }

    @Override
    public T higher(T t) {
        return findAndGetUpper(t, false);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst : unsupported");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast : unsupported");
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        data.forEach(action);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(cmp), false);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int left = abstractFindIndex(fromElement, fromInclusive ? 0 : 1, 0);
        int right = abstractFindIndex(toElement, toInclusive ? 0 : -1, -1) + 1;
        return right < left
                ? Collections.emptyNavigableSet()
                : new ArraySet<>(data.subList(left, right), cmp, false);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) return this;
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) return this;
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return cmp;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(data.size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }
}
