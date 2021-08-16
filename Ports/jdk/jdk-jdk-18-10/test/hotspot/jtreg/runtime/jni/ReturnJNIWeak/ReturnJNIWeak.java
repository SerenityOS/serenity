/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8166188
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @summary Test return of JNI weak global refs from native calls.
 * @run main/othervm/native -Xint ReturnJNIWeak
 * @run main/othervm/native -Xcomp ReturnJNIWeak
 */

public final class ReturnJNIWeak {

    static {
        System.loadLibrary("ReturnJNIWeak");
    }

    private static final class TestObject {
        public final int value;

        public TestObject(int value) {
            this.value = value;
        }
    }

    private static volatile TestObject testObject = null;

    private static native void registerObject(Object o);
    private static native void unregisterObject();
    private static native Object getObject();

    // Create the test object and record it both strongly and weakly.
    private static void remember(int value) {
        TestObject o = new TestObject(value);
        registerObject(o);
        testObject = o;
    }

    // Remove both strong and weak references to the current test object.
    private static void forget() {
        unregisterObject();
        testObject = null;
    }

    // Verify the weakly recorded object
    private static void checkValue(int value) throws Exception {
        Object o = getObject();
        if (o == null) {
            throw new RuntimeException("Weak reference unexpectedly null");
        }
        TestObject t = (TestObject)o;
        if (t.value != value) {
            throw new RuntimeException("Incorrect value");
        }
    }

    // Verify we can create a weak reference and get it back.
    private static void testSanity() throws Exception {
        System.out.println("running testSanity");
        int value = 5;
        try {
            remember(value);
            checkValue(value);
        } finally {
            forget();
        }
    }

    // Verify weak ref value survives across collection if strong ref exists.
    private static void testSurvival() throws Exception {
        System.out.println("running testSurvival");
        int value = 10;
        try {
            remember(value);
            checkValue(value);
            System.gc();
            // Verify weak ref still has expected value.
            checkValue(value);
        } finally {
            forget();
        }
    }

    // Verify weak ref cleared if no strong ref exists.
    private static void testClear() throws Exception {
        System.out.println("running testClear");
        int value = 15;
        try {
          remember(value);
          checkValue(value);
          // Verify still good.
          checkValue(value);
          // Drop reference.
          testObject = null;
          System.gc();
          // Verify weak ref cleared as expected.
          Object recorded = getObject();
          if (recorded != null) {
            throw new RuntimeException("expected clear");
          }
        } finally {
          forget();
        }
    }

    public static void main(String[] args) throws Exception {
        testSanity();
        testSurvival();
        testClear();
    }
}
