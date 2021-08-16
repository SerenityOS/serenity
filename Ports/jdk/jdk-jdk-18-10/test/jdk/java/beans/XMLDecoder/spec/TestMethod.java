/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests <method> element
 * @run main/othervm -Djava.security.manager=allow TestMethod
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

public final class TestMethod extends AbstractTest {
    public static final String XML
            = "<java>\n"
            + " <new class=\"TestMethod$A\">\n"
            + "  <method name=\"m\">\n"
            + "   <new class=\"TestMethod$Y\"/>\n"
            + "   <new class=\"TestMethod$Y\"/>\n"
            + "  </method>\n"
            + " </new>\n"
            + " <new class=\"TestMethod$B\">\n"
            + "  <method name=\"m\">\n"
            + "   <new class=\"TestMethod$Y\"/>\n"
            + "   <new class=\"TestMethod$Y\"/>\n"
            + "  </method>\n"
            + " </new>\n"
            + " <new class=\"TestMethod$C\">\n"
            + "  <method name=\"m\">\n"
            + "   <new class=\"TestMethod$Z\"/>\n"
            + "   <new class=\"TestMethod$Z\"/>\n"
            + "  </method>\n"
            + " </new>\n"
            + " <new class=\"TestMethod$D\">\n"
            + "  <method name=\"m\">\n"
            + "   <new class=\"TestMethod$Z\"/>\n"
            + "   <new class=\"TestMethod$Z\"/>\n"
            + "  </method>\n"
            + " </new>\n"
            + " <new class=\"TestMethod$E\">\n"
            + "  <method name=\"m\">\n"
            + "   <new class=\"TestMethod$Z\"/>\n"
            + "   <new class=\"TestMethod$Z\"/>\n"
            + "  </method>\n"
            + " </new>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestMethod().test(true);
    }

    private NoSuchMethodException exception;

    @Override
    public void exceptionThrown(Exception exception) {
        if (this.exception != null) {
            // only one exception allowed
            super.exceptionThrown(exception);
        } else if (exception instanceof NoSuchMethodException) {
            // expected exception: ambiguous methods are found
            this.exception = (NoSuchMethodException) exception;
        } else {
            super.exceptionThrown(exception);
        }
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        this.exception = null;
        validate(decoder, A.class);
        validate(decoder, B.class);
        validate(decoder, C.class);
        validate(decoder, D.class);
        validate(decoder, E.class);
        if (this.exception == null) {
            throw new Error("NoSuchMethodException expected");
        }
    }

    private static void validate(XMLDecoder decoder, Class type) {
        if (!type.equals(decoder.readObject().getClass())) {
            throw new Error("unexpected class");
        }
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
