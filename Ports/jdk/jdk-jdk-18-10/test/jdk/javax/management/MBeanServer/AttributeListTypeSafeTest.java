/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6336968
 * @summary Test adding non-Attribute values to an AttributeList.
 * @author Eamonn McManus
 */

import java.util.Collections;
import java.util.List;
import javax.management.Attribute;
import javax.management.AttributeList;

public class AttributeListTypeSafeTest {

    private static String failure;

    public static void main(String[] args) throws Exception {
        // Test calling asList after adding non-Attribute by various means
        for (Op op : Op.values()) {
            AttributeList alist = new AttributeList();
            alist.add(new Attribute("foo", "bar"));
            doOp(alist, op);
            String what = "asList() after calling " + op + " with non-Attribute";
            try {
                List<Attribute> lista = alist.asList();
                fail(what + ": succeeded but should not have");
            } catch (IllegalArgumentException e) {
                System.out.println("OK: " + what + ": got IllegalArgumentException");
            }
        }

        // Test adding non-Attribute by various means after calling asList
        for (Op op : Op.values()) {
            AttributeList alist = new AttributeList();
            List<Attribute> lista = alist.asList();
            lista.add(new Attribute("foo", "bar"));
            String what = op + " with non-Attribute after calling asList()";
            try {
                doOp(alist, op);
                fail(what + ": succeeded but should not have");
            } catch (IllegalArgumentException e) {
                System.out.println("OK: " + what + ": got IllegalArgumentException");
            }
        }

        if (failure == null)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static enum Op {
        ADD("add(Object)"), ADD_AT("add(int, Object)"),
        ADD_ALL("add(Collection)"), ADD_ALL_AT("add(int, Collection)"),
        SET("set(int, Object)");

        private Op(String what) {
            this.what = what;
        }

        @Override
        public String toString() {
            return what;
        }

        private final String what;
    }

    private static void doOp(AttributeList alist, Op op) {
        Object x = "oops";
        switch (op) {
            case ADD: alist.add(x); break;
            case ADD_AT: alist.add(0, x); break;
            case ADD_ALL: alist.add(Collections.singleton(x)); break;
            case ADD_ALL_AT: alist.add(0, Collections.singleton(x)); break;
            case SET: alist.set(0, x); break;
            default: throw new AssertionError("Case not covered");
        }
    }

    private static void fail(String why) {
        System.out.println("FAIL: " + why);
        failure = why;
    }

}
