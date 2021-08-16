/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8193369
 * @summary Tests that all FieldAccess and FieldModification notifications
            are generated.
 * @requires vm.jvmti
 * @compile FieldAccessWatch.java
 * @run main/othervm/native -agentlib:FieldAccessWatch FieldAccessWatch
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FieldAccessWatch {

    private static final String agentLib = "FieldAccessWatch";

    private static class MyItem {
    }

    private static class MyList {
        public List<MyItem> items = new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        try {
            System.loadLibrary(agentLib);
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Failed to load " + agentLib + " lib");
            System.err.println("java.library.path: " + System.getProperty("java.library.path"));
            throw ex;
        }

        if (!initWatchers(MyList.class, MyList.class.getDeclaredField("items"))) {
            throw new RuntimeException("Watchers initializations error");
        }

        MyList list = new MyList();

        test("[1]items.add(0, object)",() -> list.items.add(0, new MyItem()));
        test("[2]items.add(object)", () -> list.items.add(new MyItem()));
        test("[3]items.add(1, object)", () -> list.items.add(1, new MyItem()));
        test("[4]items.add(object)", () -> list.items.add(new MyItem()));
        test("[5]items.add(1, object)", () -> list.items.add(1, new MyItem()));
    }

    private static void log(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

    // For every access/modify notification native part tries to locate
    // boolean "<field_name>_access"/"<field_name>_modify" field and sets it to true
    private static class TestResult {
        // MyList.items
        public boolean items_access;

        // AbstractList.modCount
        public boolean modCount_access;
        public boolean modCount_modify;

        // ArrayList.size
        public boolean size_access;
        public boolean size_modify;

        // ArrayList.elementData
        public boolean elementData_access;

        // verify that all fields are set to true
        public void verify() {
            Arrays.stream(this.getClass().getDeclaredFields()).forEach(f -> verify(f));
        }

        private void verify(Field f) {
            try {
                if (!f.getBoolean(this)) {
                    throw new RuntimeException(f.getName() + " notification is missed");
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @FunctionalInterface
    private interface TestAction {
        void apply();
    }

    private static void test(String descr, TestAction action) throws Exception {
        log(descr + ": starting");
        TestResult result = new TestResult();
        if (!startTest(result)) {
            throw new RuntimeException("startTest failed");
        }
        action.apply();
        // wait some time to ensure all posted events are handled
        Thread.sleep(500);

        stopTest();

        // check the results
        result.verify();

        log(descr + ": OK");
    }

    private static native boolean initWatchers(Class cls, Field field);
    private static native boolean startTest(TestResult results);
    private static native void stopTest();

}
