/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4530962
 * @summary Tests method search using parameter classes in Statement
 * @author Mark Davidson
 */

import java.beans.Statement;

/**
 * Ambiguous method signature should throw an exception.
 * Statement should execute the most specific method.
 */
public class Test4530962 {
    public static void main(String[] args) throws Exception {
        try {
            test(new A(), new Y(), new Y());
            throw new Error("exception expected");
        }
        catch (NoSuchMethodException exception) {
            // should throw an exception
        }
        catch (Exception exception) {
            throw new Error("unexpected exception", exception);
        }
        test(new B(), new Y(), new Y());
        test(new C(), new Z(), new Z());
        test(new D(), new Z(), new Z());
        test(new E(), new Z(), new Z());
    }

    private static void test(Object target, Object... params) throws Exception {
        new Statement(target, "m", params).execute();
    }

    /**
     * All ambiguous method declarations should fail.
     */
    public static class A {
        public void m(X x1, X x2) {
            throw new Error("A.m(X,X) should not be called");
        }

        public void m(X x1, Y y2) {
            throw new Error("A.m(X,Y) should not be called");
        }

        public void m(Y y1, X x2) {
            throw new Error("A.m(Y,X) should not be called");
        }
    }

    /**
     * The most specific method in this case would be the second declaration.
     */
    public static class B {
        public void m(X x1, X x2) {
            throw new Error("B.m(X,X) should not be called");
        }

        public void m(X x1, Y y2) {
            // expected: B.m(X,Y) should be called
        }
    }

    /**
     * The most specific method in this case would be the first declaration.
     */
    public static class C {
        public void m(Y y1, Y y2) {
            // expected: C.m(Y,Y) should be called
        }

        public void m(X x1, X x2) {
            throw new Error("C.m(X,X) should not be called");
        }
    }

    /**
     * Same as the previous case but flip methods.
     */
    public static class D {
        public void m(X x1, X x2) {
            throw new Error("D.m(X,X) should not be called");
        }

        public void m(Y y1, Y y2) {
            // expected: D.m(Y,Y) should be called
        }
    }

    /**
     * The method should be called with (Z,Z).
     */
    public static class E {
        public void m(X x1, X x2) {
            // expected: E.m(X,X) should be called
        }
    }

    public static class X {
    }

    public static class Y extends X {
    }

    public static class Z extends Y {
    }
}
