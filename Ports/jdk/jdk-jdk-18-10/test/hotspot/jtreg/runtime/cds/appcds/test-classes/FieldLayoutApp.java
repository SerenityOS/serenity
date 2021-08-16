/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.util.ArrayList;
import jdk.internal.vm.annotation.Contended;

public class FieldLayoutApp {
    public static void main(String args[]) {
        ArrayList<TestObject> list = new ArrayList<>();

        for (int i=0; i<400; i++) {
            list.add(new Base1());
            list.add(new Child1());
            list.add(new Base2());
            list.add(new Child2());
        }

        verifyAll(list);

        // Make sure the oopmaps are laid out correctly.
        System.gc();
        verifyAll(list);
    }

    static void verifyAll(ArrayList<TestObject> list) {
        for (TestObject obj : list) {
            obj.verify();
        }
    }

    static long lastUID = 0;
    synchronized static long makeUID() {
        return ++lastUID;
    }

    synchronized static void verifyUID(long uid) {
        if (uid <= 0 || uid > lastUID) {
            error("Unexpected UID " + uid + ", must be > 0 and <= " + lastUID);
        }
    }

    static void error(String s) {
        throw new RuntimeException(s);
    }

    static void ensure(boolean b) {
        if (!b) {
            error("Assertion failed");
        }
    }

    static String makeString(long n) {
        return Long.toString(n);
    }

    static class TestObject {
        void verify() {}
    }

    static class Base1 extends TestObject {
        byte b1, b2;
        String s1;
        byte b3, b4;
        String s2;
        long uid;
        long l1;
        int i1;
        long l2;
        int i2;

        Base1() {
            uid = makeUID();
            b1 = 1;
            b2 = 2;
            b3 = 3;
            b4 = 4;
            s1 = makeString(uid + 1);
            s2 = makeString(uid + 2);
            i1 = 101;
            i2 = 102;
            l1 = 1001;
            l2 = 1002;
        }

        void verify() {
            super.verify();
            ensure(b1 == 1);
            ensure(b2 == 2);
            ensure(b3 == 3);
            ensure(b4 == 4);
            verifyUID(uid);
            ensure(s1.equals(makeString(uid + 1)));
            ensure(s2.equals(makeString(uid + 2)));
            ensure(i1 == 101);
            ensure(i2 == 102);
            ensure(l1 == 1001);
            ensure(l2 == 1002);
        }
    }

    // Base1 is archived but Child1 is loaded dynamically at runtime. Base1 may be
    // archived with different field layout options that those used during runtime.
    static class Child1 extends Base1 {
        byte cb1, cb2;
        String cs1;
        byte cb3, cb4;
        String cs2;
        long cuid;
        long cl1;
        int ci1;
        long cl2;
        int ci2;

        Child1() {
            cuid = makeUID();
            cb1 = 1;
            cb2 = 2;
            cb3 = 3;
            cb4 = 4;
            cs1 = makeString(cuid + 1);
            cs2 = makeString(cuid + 2);
            ci1 = 101;
            ci2 = 102;
            cl1 = 1001;
            cl2 = 1002;
        }

        void verify() {
            super.verify();
            ensure(cb1 == 1);
            ensure(cb2 == 2);
            ensure(cb3 == 3);
            ensure(cb4 == 4);
            verifyUID(uid);
            ensure(cs1.equals(makeString(cuid + 1)));
            ensure(cs2.equals(makeString(cuid + 2)));
            ensure(ci1 == 101);
            ensure(ci2 == 102);
            ensure(cl1 == 1001);
            ensure(cl2 == 1002);

            // Check the fields declared by the super class:
            ensure(b1 == 1); // javac should generate a FieldRef of FieldLayoutApp$Child1.b1:B,
                             // even though b1 is declared in the super class.
            ensure(b2 == 2);
            ensure(b3 == 3);
            ensure(b4 == 4);
            verifyUID(uid);
            ensure(s1.equals(makeString(uid + 1)));
            ensure(s2.equals(makeString(uid + 2)));

            ensure(i1 == 101);
            ensure(i2 == 102);
            ensure(l1 == 1001);
            ensure(l2 == 1002);
        }
    }

    // Same as Base1 - minus the i1, i2, l1, l2 fields, plus some @Contended annotations
    static class Base2 extends TestObject {
        byte b1, b2;
        String s1;
        @Contended byte b3, b4;
        @Contended String s2;
        long uid;

        Base2() {
            uid = makeUID();
            b1 = 1;
            b2 = 2;
            b3 = 3;
            b4 = 4;
            s1 = makeString(uid + 1);
            s2 = makeString(uid + 2);
        }

        void verify() {
            super.verify();
            ensure(b1 == 1);
            ensure(b2 == 2);
            ensure(b3 == 3);
            ensure(b4 == 4);
            verifyUID(uid);
            ensure(s1.equals(makeString(uid + 1)));
            ensure(s2.equals(makeString(uid + 2)));
        }
    }

    // Same as Child2 - minus the ci1, ci2, cl1, cl2 fields, plus some @Contended annotations
    //
    // Base2 is archived but Child2 is loaded dynamically at runtime. Base2 may be
    // archived with different field layout options that those used during runtime.
    static class Child2 extends Base2 {
        byte cb1, cb2;
        @Contended String cs1;
        byte cb3, cb4;
        String cs2;
        @Contended long cuid;

        Child2() {
            cuid = makeUID();
            cb1 = 1;
            cb2 = 2;
            cb3 = 3;
            cb4 = 4;
            cs1 = makeString(cuid + 1);
            cs2 = makeString(cuid + 2);
        }

        void verify() {
            super.verify();
            ensure(cb1 == 1);
            ensure(cb2 == 2);
            ensure(cb3 == 3);
            ensure(cb4 == 4);
            verifyUID(uid);
            ensure(cs1.equals(makeString(cuid + 1)));
            ensure(cs2.equals(makeString(cuid + 2)));

            // Check the fields declared by the super class:
            ensure(b1 == 1); // javac should generate a FieldRef of FieldLayoutApp$Child2.b1:B,
                             // even though b1 is declared in the super class.
            ensure(b2 == 2);
            ensure(b3 == 3);
            ensure(b4 == 4);
            verifyUID(uid);
            ensure(s1.equals(makeString(uid + 1)));
            ensure(s2.equals(makeString(uid + 2)));
        }
    }
}


