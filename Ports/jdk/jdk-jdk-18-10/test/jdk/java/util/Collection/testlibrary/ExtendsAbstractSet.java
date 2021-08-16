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
import java.util.HashSet;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @library
 *
 * A simple mutable set implementation that provides only default
 * implementations of all methods. ie. none of the Set interface default methods
 * have overridden implementations.
 *
 * @param <E> type of set members
 */
public class ExtendsAbstractSet<E> extends AbstractSet<E> {

    protected final Set<E> set;

    public ExtendsAbstractSet() {
        this(HashSet<E>::new);
    }

    public ExtendsAbstractSet(Collection<E> source) {
        this();
        addAll(source);
    }

    protected ExtendsAbstractSet(Supplier<Set<E>> backer) {
        this.set = backer.get();
    }

    public boolean add(E element) {
        return set.add(element);
    }

    public boolean remove(Object element) {
        return set.remove(element);
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<E> source = set.iterator();

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
        return set.size();
    }
}
