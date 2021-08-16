/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214031 8221413
 * @summary Verify that definite assignment when true works (legal code)
 * @compile DefiniteAssignment1.java
 * @run main DefiniteAssignment1
 */
public class DefiniteAssignment1 {
    public static void main(String[] args) {
        int a = 0;
        boolean b = true;

        {
        int x;

        boolean t1 = (b && switch(a) {
            case 0: yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t1) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t1 = (b && switch(a) {
            case 0: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t1) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t1a = (b && switch(a) {
            case 0: yield (x = 1) == 1;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t1a) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t1b = (switch(a) {
            case 0: yield (x = 1) == 1;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t1b) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t2 = !(b && switch(a) {
            case 0: yield (x = 1) == 1 || true;
            default: yield false;
        }) || x == 1;

        if (!t2) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t2 = !(b && switch(a) {
            case 0: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) || x == 1;

        if (!t2) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t3 = !(switch(a) {
            case 0: yield (x = 1) == 1 || true;
            default: yield false;
        }) || x == 2;

        if (t3) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t3 = !(switch(a) {
            case 0: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) || x == 2;

        if (t3) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        boolean t4 = (b && switch(a) {
            case 0: yield (x = 1) == 1 || true;
            default: throw new IllegalStateException();
        }) && x == 1; //x is definitelly assigned here

        if (!t4) {
            throw new IllegalStateException("Unexpected result.");
        }
        }


        {
        int x;

        boolean t4 = (b && switch(a) {
            case 0: yield (x = 1) == 1 || isTrue();
            default: throw new IllegalStateException();
        }) && x == 1; //x is definitelly assigned here

        if (!t4) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        String s = "a";

        boolean t5 = (switch(s) {
            case "a": yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t5) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        String s = "a";

        boolean t5 = (switch(s) {
            case "a": yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t5) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.B;

        boolean t6 = (switch(e) {
            case B: yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t6) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.B;

        boolean t6 = (switch(e) {
            case B: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t6) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        int t7 = new DefiniteAssignment1().id(switch(0) {
            default -> true;
        } && (x = 1) == 1 && x == 1 ? 2 : -1);

        if (t7 != 2) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;

        int t7 = new DefiniteAssignment1().id(switch(0) {
            default -> isTrue();
        } && (x = 1) == 1 && x == 1 ? 2 : -1);

        if (t7 != 2) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.B;

        boolean t8 = (switch(e) {
            case A: x = 1; yield true;
            case B: yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t8) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.B;

        boolean t8 = (switch(e) {
            case A: x = 1; yield isTrue();
            case B: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t8) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.A;

        boolean t9 = (switch(e) {
            case A: x = 1; yield true;
            case B: yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t9) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.A;

        boolean t9 = (switch(e) {
            case A: x = 1; yield isTrue();
            case B: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!t9) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.C;

        boolean tA = (switch(e) {
            case A: x = 1; yield true;
            case B: yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (tA) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.C;

        boolean tA = (switch(e) {
            case A: x = 1; yield isTrue();
            case B: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (tA) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        final int x;
        E e = E.C;

        boolean tA = (switch(e) {
            case A: x = 1; yield true;
            case B: yield (x = 2) == 2 || true;
            default: yield false;
        }) || (x = 3) == 3; //x is definitelly unassigned here

        if (x != 3) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.A;

        boolean tA = (switch(e) {
            case A: yield isTrue() && (x = 1) == 1;
            case B: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!tA) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.A;

        boolean tA = (switch(e) {
            case A: yield isTrue() && e != E.C ? (x = 1) == 1 && e != E.B : false;
            case B: yield (x = 1) == 1 || true;
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!tA) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        {
        int x;
        E e = E.A;

        boolean tA = (switch(e) {
            case A: yield isTrue() && e != E.C ? (x = 1) == 1 && e != E.B : false;
            case B: yield (x = 1) == 1 || isTrue();
            default: yield false;
        }) && x == 1; //x is definitelly assigned here

        if (!tA) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        { //JDK-8221413: definite assignment for implicit default in switch expressions
        int x;
        E e = E.A;

        int v = (switch(e) {
            case A -> x = 0;
            case B -> x = 0;
            case C -> x = 0;
        });

        if (x != 0 || v != 0) {
            throw new IllegalStateException("Unexpected result.");
        }
        }

        { //JDK-8221413: definite assignment for implicit default in switch expressions
        int x;
        E e = E.A;

        boolean tA = (switch(e) {
            case A -> { x = 1; yield true; }
            case B -> { x = 1; yield true; }
            case C -> { x = 1; yield true; }
        }) && x == 1; //x is definitelly assigned here

        if (!tA) {
            throw new IllegalStateException("Unexpected result.");
        }
        }
    }

    private int id(int v) {
        return v;
    }

    private static boolean isTrue() {
        return true;
    }

    enum E {
        A, B, C;
    }
}
