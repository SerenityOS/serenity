/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080842
 * @summary Ensure Scope impl can cope with remove() when a field and method share the name.
 * @run main RemoveSymbolTest
 */

import java.util.Iterator;
import java.util.LinkedList;

public class RemoveSymbolTest<W> implements Iterable<W> {
    static class Widget {
        private String name;
        Widget(String s) { name = s; }
        @Override public String toString() { return name; }
    }

    private LinkedList<W> data;
    // Instantiate an Iterable instance using a Lambda expression.
    // Causes ClassFormatError if a local variable of type Widget is named after one of the methods.
    private final Iterable<W> myIterator1 = () -> new Iterator<W>() {
        private W hasNext = null;
        private int index = 0;
        @Override public boolean hasNext() { return index < data.size(); }
        @Override public W next() { return data.get(index++); }
    };

    // Instantiate an Iterable instance using an anonymous class.
    // Always works fine regardless of the name of the local variable.
    private final Iterable<W> myIterator2 =
        new Iterable<W>() {
        @Override
        public Iterator<W> iterator() {
            return new Iterator<W>() {
                private W hasNext = null;
                private int index = 0;
                @Override public boolean hasNext() { return index < data.size(); }
                @Override public W next() { return data.get(index++); }
            };
        }
    };
    public RemoveSymbolTest() { data = new LinkedList<>(); }
    public void add(W e) { data.add(e); }
    @Override public String toString() { return data.toString(); }
    @Override public Iterator<W> iterator() { return myIterator1.iterator(); }
    public static void main(String[] args) {
        RemoveSymbolTest<Widget> widgets = new RemoveSymbolTest<>();
        widgets.add(new Widget("W1"));
        widgets.add(new Widget("W2"));
        widgets.add(new Widget("W3"));
        System.out.println(".foreach() call: ");
        widgets.forEach(w -> System.out.println(w + " "));
    }
}
