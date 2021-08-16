/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Unit tests for JVMTI IterateOverHeap and IterateOverInstancesOfClass
 * functions.
 *
 */

package nsk.jvmti.unit.heap;

import nsk.share.jvmti.unit.*;
import java.io.PrintStream;

public class BasicIterationTests {

    final static int JCK_STATUS_BASE = 95;
    final static int PASSED = 0;
    final static int FAILED = 2;

    static class Foo { }
    static class Foo2 extends Foo { }
    static class Foo3 extends Foo2 { }
    static class Bar { }

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        test1();
        test2();
        test3();
        test4();
        return PASSED;
    }

    public static void test1() {

        // Test that there aren't any objects tagged - the test uses
        // both the JVMTI_HEAP_OBJECT_EITHER and JVMTI_HEAP_OBJECT_TAGGED
        // filters

        Heap.setTaggedObjectCountCallback();

        Heap.zeroObjectCount();
        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_EITHER);
        if (Heap.getObjectCount() > 0) {
            throw new RuntimeException("Test failed - some objects were tagged");
        }

        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_TAGGED);
        if (Heap.getObjectCount() > 0) {
            throw new RuntimeException("Test failed - some objects were tagged");
        }
    }

    public static void test2() {

        // Test tags two objects and then calls iterateOverHeap
        // for each of the possible object filters

        Heap.setTaggedObjectCountCallback();

        Object o1 = new Object();
        Object o2 = new Object();
        Heap.setTag(o1, 1);
        Heap.setTag(o2, 2);

        Heap.zeroObjectCount();
        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_EITHER);
        if (Heap.getObjectCount() != 2) {
            throw new RuntimeException("Test failed - expected 2 objects to be tagged");
        }

        Heap.zeroObjectCount();
        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_TAGGED);
        if (Heap.getObjectCount() != 2) {
            throw new RuntimeException("Test failed - expected 2 objects to be tagged");
        }

        Heap.zeroObjectCount();
        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_UNTAGGED);
        if (Heap.getObjectCount() != 0) {
            throw new RuntimeException("Test failed - expected 0");
        }

        // clean-up by untagged both objects

        Heap.setTag(o1, 0);
        Heap.setTag(o2, 0);

        Heap.zeroObjectCount();
        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_TAGGED);
        if (Heap.getObjectCount() != 0) {
            throw new RuntimeException("Test failed - expected 0");
        }
    }

    public static void test3() {

        // This test is used to check the 'klass_tag' parameter to
        // the heap object callback. The callback is expected to tag objects
        // with the klass tag

        Heap.setKlassTagTestCallback();

        Heap.setTag(Foo.class, 10);
        Heap.setTag(Bar.class, 20);

        Foo foo = new Foo();
        Bar bar = new Bar();

        Heap.iterateOverHeap(Heap.JVMTI_HEAP_OBJECT_EITHER);

        if (Heap.getTag(foo) != Heap.getTag(Foo.class)) {
            throw new RuntimeException("foo is not tagged as expected");
        }
        if (Heap.getTag(bar) != Heap.getTag(Bar.class)) {
            throw new RuntimeException("bar is not tagged as expected");
        }

        // untag everything

        Heap.setTag(Foo.class, 0);
        Heap.setTag(Bar.class, 0);
        Heap.setTag(foo, 0);
        Heap.setTag(bar, 0);
    }

    static void test4() {

        // This tests IterateOverInstancesOfClass

        // The test tags 50 instances of Foo2 and 50 instances of Foo3.
        // It then iterates over all instances of Foo2. As Foo3 extends
        // Foo2 it means we should get 100 callbacks. We also tag a
        // few random objects to prove that the callback is called for
        // these.

        Foo2[] foo2 = new Foo2[50];
        for (int i=0; i<foo2.length; i++) {
            foo2[i] = new Foo2();
            Heap.setTag(foo2[i], 99);
        }

        Foo3[] foo3 = new Foo3[50];
        for (int i=0; i<foo3.length; i++) {
            foo3[i] = new Foo3();
            Heap.setTag(foo3[i], 99);
        }

        Object o = new Object();
        Heap.setTag(o, 99);

        Object arr[] = new Object[2];
        Heap.setTag(arr, 99);

        Heap.setTotalObjectCountCallback();

        Heap.zeroObjectCount();
        Heap.iterateOverInstancesOfClass(Foo2.class, Heap.JVMTI_HEAP_OBJECT_EITHER);
        int count = Heap.getObjectCount();
        if (count != 100) {
            throw new RuntimeException(count + " instances of Foo2!!!!");
        }

        Heap.zeroObjectCount();
        Heap.iterateOverInstancesOfClass(Foo2.class, Heap.JVMTI_HEAP_OBJECT_TAGGED);
        count = Heap.getObjectCount();
        if (count != 100) {
            throw new RuntimeException(count + " instances of Foo2!!!!");
        }

        Heap.zeroObjectCount();
        Heap.iterateOverInstancesOfClass(Foo3.class, Heap.JVMTI_HEAP_OBJECT_EITHER);
        count = Heap.getObjectCount();
        if (count != 50) {
            throw new RuntimeException(count + " instances of Foo3!!!!");
        }

        Heap.zeroObjectCount();
        Heap.iterateOverInstancesOfClass(Foo3.class, Heap.JVMTI_HEAP_OBJECT_TAGGED);
        count = Heap.getObjectCount();
        if (count != 50) {
            throw new RuntimeException(count + " instances of Foo3!!!!");
        }

    }

}
