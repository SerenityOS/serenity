/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     6365166
 * @summary javac (generic) unable to resolve methods
 * @compile NewTest.java
 */

import java.util.*;

public class NewTest<A,B> {
    private List<A> toAdd;

    public NewTest(List<A> toAdd) {
        this.toAdd = toAdd;
    }

    private List<A> getRelated(B b) {
        //some application logic
        //for demo
        return toAdd;
    }

    @SuppressWarnings("unchecked")
    public <L extends List<? super A>,LF extends Factory<L>> L addOrCreate4(B b,L l,LF lf) {
        if (l == null) {
            l = lf.create();
        }
        ((List<? super A>)l).addAll(getRelated(b)); //to get round the compiler bug
        return l;
    }

    public static class ListFactory<T>  implements Factory<List<T>>{
        public List<T> create() {
            return new ArrayList<T>();
        }
    }
    public static interface Factory<T> {
        public T create();
    }

    public static void main(String ... args) {
        ListFactory<Number> lf = new ListFactory<Number>();
        List<Long> longs = new ArrayList<Long>();
        longs.add(new Long(1));
        NewTest<Long,Number> test = new NewTest<Long,Number>(longs);

        List<Number> ret4 = null;

        ret4 = test.addOrCreate4(1, ret4,lf);

    }
}
