/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.SuppressWarnings;

import static jdk.test.lib.Asserts.*;

/*
 * @test
 * @library /test/lib
 * @summary Tests the different assertions in the Assert class
 */
public class AssertsTest {
    private static class Foo implements Comparable<Foo> {
        final int id;
        public Foo(int id) {
            this.id = id;
        }

        public int compareTo(Foo f) {
            return new Integer(id).compareTo(new Integer(f.id));
        }
        public String toString() {
            return "Foo(" + Integer.toString(id) + ")";
        }
    }

    public static void main(String[] args) throws Exception {
        testLessThan();
        testLessThanOrEqual();
        testEquals();
        testGreaterThanOrEqual();
        testGreaterThan();
        testNotEquals();
        testNull();
        testNotNull();
        testTrue();
        testFalse();
        testFail();
    }

    private static void testLessThan() throws Exception {
        expectPass(Assertion.LT, 1, 2);

        expectFail(Assertion.LT, 2, 2);
        expectFail(Assertion.LT, 2, 1);
        expectFail(Assertion.LT, null, 2);
        expectFail(Assertion.LT, 2, null);
    }

    private static void testLessThanOrEqual() throws Exception {
        expectPass(Assertion.LTE, 1, 2);
        expectPass(Assertion.LTE, 2, 2);

        expectFail(Assertion.LTE, 3, 2);
        expectFail(Assertion.LTE, null, 2);
        expectFail(Assertion.LTE, 2, null);
    }

    private static void testEquals() throws Exception {
        expectPass(Assertion.EQ, 1, 1);
        expectPass(Assertion.EQ, (Integer)null, (Integer)null);

        Foo f1 = new Foo(1);
        expectPass(Assertion.EQ, f1, f1);

        Foo f2 = new Foo(1);
        expectFail(Assertion.EQ, f1, f2);
        expectFail(Assertion.LTE, null, 2);
        expectFail(Assertion.LTE, 2, null);
    }

    private static void testGreaterThanOrEqual() throws Exception {
        expectPass(Assertion.GTE, 1, 1);
        expectPass(Assertion.GTE, 2, 1);

        expectFail(Assertion.GTE, 1, 2);
        expectFail(Assertion.GTE, null, 2);
        expectFail(Assertion.GTE, 2, null);
    }

    private static void testGreaterThan() throws Exception {
        expectPass(Assertion.GT, 2, 1);

        expectFail(Assertion.GT, 1, 1);
        expectFail(Assertion.GT, 1, 2);
        expectFail(Assertion.GT, null, 2);
        expectFail(Assertion.GT, 2, null);
    }

    private static void testNotEquals() throws Exception {
        expectPass(Assertion.NE, null, 1);
        expectPass(Assertion.NE, 1, null);

        Foo f1 = new Foo(1);
        Foo f2 = new Foo(1);
        expectPass(Assertion.NE, f1, f2);

        expectFail(Assertion.NE, (Integer)null, (Integer)null);
        expectFail(Assertion.NE, f1, f1);
        expectFail(Assertion.NE, 1, 1);
    }

    private static void testNull() throws Exception {
        expectPass(Assertion.NULL, (Integer)null);

        expectFail(Assertion.NULL, 1);
    }

    private static void testNotNull() throws Exception {
        expectPass(Assertion.NOTNULL, 1);

        expectFail(Assertion.NOTNULL, (Integer)null);
    }

    private static void testTrue() throws Exception {
        expectPass(Assertion.TRUE, true);

        expectFail(Assertion.TRUE, false);
    }

    private static void testFalse() throws Exception {
        expectPass(Assertion.FALSE, false);

        expectFail(Assertion.FALSE, true);
    }

    private static void testFail() throws Exception {
        try {
            fail();
        } catch (RuntimeException re) {
            assertEquals("fail", re.getMessage());
        }

        try {
            fail("Failure");
        } catch (RuntimeException re) {
            assertEquals("Failure", re.getMessage());
        }

        Exception e = new Exception("the cause");
        try {
            fail("Fail w/ cause", e);
        } catch (RuntimeException re) {
            assertEquals("Fail w/ cause", re.getMessage());
            assertEquals(e, re.getCause(), "Cause mismatch");
        }

        try {
            fail(1, 2, "Different", "vs");
        } catch (RuntimeException re) {
            assertEquals("Different <1> vs <2>", re.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> void expectPass(Assertion assertion, T ... args)
        throws Exception {
        Assertion.run(assertion, args);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> void expectFail(Assertion assertion, T ... args)
        throws Exception {
        try {
            Assertion.run(assertion, args);
        } catch (RuntimeException e) {
            return;
        }
        throw new Exception("Expected " + Assertion.format(assertion, (Object[]) args) +
                            " to throw a RuntimeException");
    }

}

enum Assertion {
    LT, LTE, EQ, GTE, GT, NE, NULL, NOTNULL, FALSE, TRUE;

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> void run(Assertion assertion, T ... args) {
        String msg = "Expected " + format(assertion, (Object[])args) + " to pass";
        switch (assertion) {
            case LT:
                assertLessThan(args[0], args[1], msg);
                break;
            case LTE:
                assertLessThanOrEqual(args[0], args[1], msg);
                break;
            case EQ:
                assertEquals(args[0], args[1], msg);
                break;
            case GTE:
                assertGreaterThanOrEqual(args[0], args[1], msg);
                break;
            case GT:
                assertGreaterThan(args[0], args[1], msg);
                break;
            case NE:
                assertNotEquals(args[0], args[1], msg);
                break;
            case NULL:
                assertNull(args == null ? args : args[0], msg);
                break;
            case NOTNULL:
                assertNotNull(args == null ? args : args[0], msg);
                break;
            case FALSE:
                assertFalse((Boolean) args[0], msg);
                break;
            case TRUE:
                assertTrue((Boolean) args[0], msg);
                break;
            default:
                // do nothing
        }
    }

    public static String format(Assertion assertion, Object ... args) {
        switch (assertion) {
            case LT:
                return asString("assertLessThan", args);
            case LTE:
                return asString("assertLessThanOrEqual", args);
            case EQ:
                return asString("assertEquals", args);
            case GTE:
                return asString("assertGreaterThanOrEquals", args);
            case GT:
                return asString("assertGreaterThan", args);
            case NE:
                return asString("assertNotEquals", args);
            case NULL:
                return asString("assertNull", args);
            case NOTNULL:
                return asString("assertNotNull", args);
            case FALSE:
                return asString("assertFalse", args);
            case TRUE:
                return asString("assertTrue", args);
            default:
                return "";
        }
    }

    private static String asString(String assertion, Object ... args) {
        if (args == null) {
            return String.format("%s(null)", assertion);
        }
        if (args.length == 1) {
            return String.format("%s(%s)", assertion, args[0]);
        } else {
            return String.format("%s(%s, %s)", assertion, args[0], args[1]);
        }
    }
}
