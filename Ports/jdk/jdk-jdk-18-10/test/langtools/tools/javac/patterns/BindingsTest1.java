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

/*
 * @test
 * @bug 8231827
 * @summary Basic tests for bindings from instanceof
 * @compile BindingsTest1.java
 * @run main BindingsTest1
 */

public class BindingsTest1 {
    public static boolean Ktrue() { return true; }
    public static void main(String[] args) {
        Object o1 = "hello";
        Integer i = 42;
        Object o2 = i;
        Object o3 = "there";


        // Test for (e matches P).T = { binding variables in P }
        if (o1 instanceof String s) {
            s.length();
        }

        // Test for e1 && e2.T = union(e1.T, e2.T)
        if (o1 instanceof String s && o2 instanceof Integer in) {
            s.length();
            in.intValue();
        }

        // test for e1&&e2 - include e1.T in e2
        if (o1 instanceof String s && s.length()>0) {
            System.out.print("done");
        }

        // Test for (e1 || e2).F = union(e1.F, e2.F)
        if (!(o1 instanceof String s) || !(o3 instanceof Integer in)){
        } else {
            s.length();
            i.intValue();
        }

        // Test for e1||e2 - include e1.F in e2

        if (!(o1 instanceof String s) || s.length()>0) {
            System.out.println("done");
        }

        // Test for e1 ? e2: e3 - include e1.T in e2
        if (o1 instanceof String s ? s.length()>0 : false) {
            System.out.println("done");
        }

        // Test for e1 ? e2 : e3 - include e1.F in e3
        if (!(o1 instanceof String s) ? false : s.length()>0){
            System.out.println("done");
        }

        // Test for (!e).T = e.F

        if (!(!(o1 instanceof String s) || !(o3 instanceof Integer in))){
            s.length();
            i.intValue();
        }

        // Test for (!e).F = e.T
        if (!(o1 instanceof String s)) {

        } else {
            s.length();
        }

        L1: {
            if (o1 instanceof String s) {
                s.length();
            } else {
                break L1;
            }
            s.length();
        }

        L2: {
            if (!(o1 instanceof String s)) {
                break L2;
            } else {
                s.length();
            }
            s.length();
        }

        L4: {
            if (!(o1 instanceof String s)) {
                break L4;
            }
            s.length();
        }

        {
            while (!(o1 instanceof String s)) {
            }

            s.length();
        }

        L5: {
            while (!(o1 instanceof String s)) {
            }

            s.length();
        }

        {
            L6: for ( ;!(o1 instanceof String s); ) {

            }

            s.length();
        }

        {
            L7: do {

            } while (!(o1 instanceof String s));

            s.length();
        }

        {
            while (!(o1 instanceof String s)) {
                L8: break L8;
            }

            s.length();
        }

        {
            for ( ;!(o1 instanceof String s); ) {
                L9: break L9;
            }

            s.length();
        }

        {
            do {
                L10: break L10;
            } while (!(o1 instanceof String s));

            s.length();
        }

        if (o1 instanceof String s) {
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.length();
                }
            };
            r1.run();
            Runnable r2 = () -> {
                s.length();
            };
            r2.run();
            String s2 = s;
        }

        if (o1 instanceof final String s) {
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.length();
                }
            };
            r1.run();
            Runnable r2 = () -> {
                s.length();
            };
            r2.run();
            String s2 = s;
        }

        boolean result = (o1 instanceof String a1) ? (o1 instanceof String a2) : (!(o1 instanceof String a3));
        boolean result2 = (o1 instanceof String a1) ? (o1 instanceof String a2) : (!(switch (0) { default -> false; }));

        //binding in an expression lambda:
        if (!((VoidPredicate) () -> o1 instanceof String str && !str.isEmpty()).get()) {
            throw new AssertionError();
        }

        //binding in an block lambda:
        if (!((VoidPredicate) () -> o1 instanceof String str && !str.isEmpty()).get()) {
            throw new AssertionError();
        }

        //binding in an anonymous class:
        if (!new VoidPredicate() { public boolean get() { return o1 instanceof String str && !str.isEmpty();} }.get()) {
            throw new AssertionError();
        }

        if (!switch (i) {
            default:
                if (!(o1 instanceof String str)) {
                    yield false;
                }
                if (str.isEmpty()) {
                    yield true;
                }
                yield true;
            }) {
            throw new AssertionError();
        }

        //binding in an anonymous class:
        if (!(invokeOnce("") instanceof String s)) {
            throw new AssertionError();
        }

        System.out.println("BindingsTest1 complete");
    }

    interface VoidPredicate {
        public boolean get();
    }
    static boolean id(boolean b) {
        return b;
    }
    private static boolean invoked;
    static Object invokeOnce(Object val) {
        if (invoked) {
            throw new IllegalStateException();
        } else {
            invoked = true;
            return val;
        }
    }
}
