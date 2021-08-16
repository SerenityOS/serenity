/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8256377
 * @summary Based on test/jdk/java/lang/ref/ReferenceRefersTo.java.
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

public class TestReferenceRefersTo {
    private static final void fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }

    // Test java.lang.ref.Reference::refersTo0 intrinsic.
    private static final void test(Reference ref,
                                   Object expectedValue,
                                   Object unexpectedValue,
                                   String kind) throws Exception {
        if ((expectedValue != null) && ref.refersTo(null)) {
            fail(kind + " refers to null");
        }
        if (!ref.refersTo(expectedValue)) {
            fail(kind + " doesn't refer to expected value");
        }
        if (ref.refersTo(unexpectedValue)) {
            fail(kind + " refers to unexpected value");
        }
    }

    // Test java.lang.ref.PhantomReference::refersTo0 intrinsic.
    private static final void test_phantom(PhantomReference ref,
                                           Object expectedValue,
                                           Object unexpectedValue) throws Exception {
        String kind = "phantom";
        if ((expectedValue != null) && ref.refersTo(null)) {
            fail(kind + " refers to null");
        }
        if (!ref.refersTo(expectedValue)) {
            fail(kind + " doesn't refer to expected value");
        }
        if (ref.refersTo(unexpectedValue)) {
            fail(kind + " refers to unexpected value");
        }

    }

    private static final void test_weak(WeakReference ref,
                                        Object expectedValue,
                                        Object unexpectedValue) throws Exception {
        test(ref, expectedValue, unexpectedValue, "weak");
    }

    private static final void test_soft(SoftReference ref,
                                        Object expectedValue,
                                        Object unexpectedValue) throws Exception {
        test(ref, expectedValue, unexpectedValue, "soft");
    }

    public static void main(String[] args) throws Exception {
        var queue = new ReferenceQueue<Object>();

        var obj0 = new Object();
        var obj1 = new Object();
        var obj2 = new Object();
        var obj3 = new Object();

        var pref = new PhantomReference(obj0, queue);
        var wref = new WeakReference(obj1);
        var sref = new SoftReference(obj2);

        System.out.println("Warmup");
        for (int i = 0; i < 10000; i++) {
            test_phantom(pref, obj0, obj3);
            test_weak(wref, obj1, obj3);
            test_soft(sref, obj2, obj3);
        }

        System.out.println("Testing starts");
        test_phantom(pref, obj0, obj3);
        test_weak(wref, obj1, obj3);
        test_soft(sref, obj2, obj3);

        System.out.println("Cleaning references");
        pref.clear();
        wref.clear();
        sref.clear();

        System.out.println("Testing after cleaning");
        test_phantom(pref, null, obj3);
        test_weak(wref, null, obj3);
        test_soft(sref, null, obj3);
    }
}
