/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6187118
 * @summary Tests encoding of immutable list that creates itself
 * @run main/othervm -Djava.security.manager=allow Test6187118
 * @author Sergey Malenkov
 */

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Test6187118 extends AbstractTest {
    public static void main(String[] args) {
        new Test6187118().test(true);
    }

    protected ImmutableList<String> getObject() {
        return new ImmutableList<String>();
    }

    protected ImmutableList<String> getAnotherObject() {
        return new ImmutableList<String>().add("1").add("2").add("3").add("4");
    }

    protected void initialize(XMLEncoder encoder) {
        encoder.setPersistenceDelegate(
                ImmutableList.class,
                new PersistenceDelegate() {
                    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                        return oldInstance.equals(newInstance);
                    }

                    protected Expression instantiate(Object oldInstance, Encoder out) {
                        ImmutableList list = (ImmutableList) oldInstance;
                        if (!list.hasEntries()) {
                            return getExpression(oldInstance, ImmutableList.class, "new");
                        }
                        Object object = list.getLast();
                        ImmutableList shortenedList = list.removeLast();
                        return getExpression(oldInstance, shortenedList, "add", object);
                    }

                    private Expression getExpression(Object value, Object target, String method, Object... args) {
                        return new Expression(value, target, method, args);
                    }
                }
        );
    }

    public static final class ImmutableList<T> implements Iterable {
        private final List<T> list = new ArrayList<T>();

        public ImmutableList() {
        }

        private ImmutableList(Iterable<T> iterable) {
            for (T object : iterable) {
                this.list.add(object);
            }
        }

        public Iterator<T> iterator() {
            return Collections.unmodifiableList(this.list).iterator();
        }

        public ImmutableList<T> add(T object) {
            ImmutableList<T> list = new ImmutableList<T>(this.list);
            list.list.add(object);
            return list;
        }

        public ImmutableList<T> removeLast() {
            ImmutableList<T> list = new ImmutableList<T>(this.list);
            int size = list.list.size();
            if (0 < size) {
                list.list.remove(size - 1);
            }
            return list;
        }

        public T getLast() {
            int size = this.list.size();
            return (0 < size)
                    ? this.list.get(size - 1)
                    : null;
        }

        public boolean hasEntries() {
            return 0 < this.list.size();
        }

        public boolean equals(Object object) {
            if (object instanceof ImmutableList) {
                ImmutableList list = (ImmutableList) object;
                return this.list.equals(list.list);
            }
            return false;
        }

        public int hashCode() {
            return this.list.hashCode();
        }

        public String toString() {
            return this.list.toString();
        }
    }
}
