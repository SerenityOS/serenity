/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8079136
 * @run testng NestedSubList
 * @summary Accessing a nested sublist leads to StackOverflowError
 */

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.fail;

public class NestedSubList {

    static final int NEST_LIMIT = 65536;

    @Test(dataProvider="lists")
    public void testAccessToSublists(List<Integer> list, boolean modifiable) {
        Class<?> cls = list.getClass();
        for (int i = 0; i < NEST_LIMIT; ++i) {
            list = list.subList(0, 1);
        }

        try {
            list.get(0);
            if (modifiable) {
                list.remove(0);
                list.add(0, 42);
            }
        } catch (StackOverflowError e) {
            fail("failed for " + cls);
        }
    }

    @DataProvider
    public static Object[][] lists() {
        final boolean MODIFIABLE = true;
        final boolean NON_MODIFIABLE = false;
        List<Integer> c = Arrays.asList(42);

        return new Object[][] {
            {c, NON_MODIFIABLE},
            {new ArrayList<>(c), MODIFIABLE},
            {new LinkedList<>(c), MODIFIABLE},
            {new MyList(), NON_MODIFIABLE},
            {new Vector<>(c), MODIFIABLE},
            {Collections.singletonList(42), NON_MODIFIABLE},
            {Collections.checkedList(c, Integer.class), NON_MODIFIABLE},
            {Collections.checkedList(new ArrayList<>(c), Integer.class), MODIFIABLE},
            {Collections.checkedList(new LinkedList<>(c), Integer.class), MODIFIABLE},
            {Collections.checkedList(new Vector<>(c), Integer.class), MODIFIABLE},
            {Collections.synchronizedList(c), NON_MODIFIABLE},
            {Collections.synchronizedList(new ArrayList<>(c)), MODIFIABLE},
            {Collections.synchronizedList(new LinkedList<>(c)), MODIFIABLE},
            {Collections.synchronizedList(new Vector<>(c)), MODIFIABLE},
            {Collections.unmodifiableList(c), NON_MODIFIABLE},
            {Collections.unmodifiableList(new ArrayList<>(c)), NON_MODIFIABLE},
            {Collections.unmodifiableList(new LinkedList<>(c)), NON_MODIFIABLE},
            {Collections.unmodifiableList(new Vector<>(c)), NON_MODIFIABLE},
        };
    }

    static class MyList extends AbstractList<Integer> {
        public Integer get(int index) { return 42; }
        public int size() { return 1; }
    }
}
