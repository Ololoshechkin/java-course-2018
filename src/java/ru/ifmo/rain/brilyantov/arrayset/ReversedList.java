package ru.ifmo.rain.brilyantov.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

class ReversedList<T> extends AbstractList<T> implements RandomAccess {

    private final List<T> data;
    private final boolean dataIsReversed;

    ReversedList(List<T> data) {
        if (!(data instanceof RandomAccess)) {
            throw new IllegalArgumentException("expected random access list!");
        } if (data instanceof ReversedList) {
            this.data = ((ReversedList<T>) data).data;
            dataIsReversed = !((ReversedList<T>) data).dataIsReversed;
        } else {
            this.data = data;
            dataIsReversed = true;
        }
    }

    @Override
    public T get(int index) {
        return dataIsReversed ? data.get(size() - index - 1) : data.get(index);
    }

    @Override
    public int size() {
        return data.size();
    }

}