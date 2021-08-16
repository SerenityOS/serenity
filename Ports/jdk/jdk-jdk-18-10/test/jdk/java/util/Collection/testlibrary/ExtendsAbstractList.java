/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
import java.util.ArrayList;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * @library
 *
 * A simple mutable list implementation that provides only default
 * implementations of all methods. ie. none of the List interface default
 * methods have overridden implementations.
 *
 * @param <E> type of list elements
 */
public class ExtendsAbstractList<E> extends AbstractList<E> {

    protected final List<E> list;

    public ExtendsAbstractList() {
        this(ArrayList<E>::new);
    }

    protected ExtendsAbstractList(Supplier<List<E>> supplier) {
        this.list = supplier.get();
    }

    public ExtendsAbstractList(Collection<E> source) {
        this();
        addAll(source);
    }

    public boolean add(E element) {
        return list.add(element);
    }

    public E get(int index) {
        return list.get(index);
    }

    public boolean remove(Object element) {
        return list.remove(element);
    }

    public E set(int index, E element) {
        return list.set(index, element);
    }

    public void add(int index, E element) {
        list.add(index, element);
    }

    public E remove(int index) {
        return list.remove(index);
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<E> source = list.iterator();

            public boolean hasNext() {
                return source.hasNext();
            }

            public E next() {
                return source.next();
            }

            public void remove() {
                source.remove();
            }
        };
    }

    public int size() {
        return list.size();
    }
}
