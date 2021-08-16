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

package jdk.test.lib;

import java.util.Objects;

/**
 * Asserts that can be used for verifying assumptions in tests.
 *
 * An assertion will throw a {@link RuntimeException} if the assertion isn't true.
 * All the asserts can be imported into a test by using a static import:
 *
 * <pre>
 * {@code
 * import static jdk.test.lib.Asserts.*;
 * }
 *
 * Always provide a message describing the assumption if the line number of the
 * failing assertion isn't enough to understand why the assumption failed. For
 * example, if the assertion is in a loop or in a method that is called
 * multiple times, then the line number won't provide enough context to
 * understand the failure.
 * </pre>
 */
public class Asserts {

    /**
     * Shorthand for {@link #assertLessThan(Comparable, Comparable)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertLessThan(Comparable, Comparable)
     */
    public static <T extends Comparable<T>> void assertLT(T lhs, T rhs) {
        assertLessThan(lhs, rhs);
    }

    /**
     * Shorthand for {@link #assertLessThan(Comparable, Comparable, String)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @see #assertLessThan(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertLT(T lhs, T rhs, String msg) {
        assertLessThan(lhs, rhs, msg);
    }

    /**
     * Calls {@link #assertLessThan(Comparable, Comparable, String)} with a default message.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertLessThan(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertLessThan(T lhs, T rhs) {
        assertLessThan(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is less than {@code rhs}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static <T extends Comparable<T>>void assertLessThan(T lhs, T rhs, String msg) {
        if (!(compare(lhs, rhs, msg) < 0)) {
            msg = Objects.toString(msg, "assertLessThan")
                    + ": expected that " + Objects.toString(lhs)
                    + " < " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Shorthand for {@link #assertLessThanOrEqual(Comparable, Comparable)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertLessThanOrEqual(Comparable, Comparable)
     */
    public static <T extends Comparable<T>> void assertLTE(T lhs, T rhs) {
        assertLessThanOrEqual(lhs, rhs);
    }

    /**
     * Shorthand for {@link #assertLessThanOrEqual(Comparable, Comparable, String)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @see #assertLessThanOrEqual(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertLTE(T lhs, T rhs, String msg) {
        assertLessThanOrEqual(lhs, rhs, msg);
    }

    /**
     * Calls {@link #assertLessThanOrEqual(Comparable, Comparable, String)} with a default message.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertLessThanOrEqual(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertLessThanOrEqual(T lhs, T rhs) {
        assertLessThanOrEqual(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is less than or equal to {@code rhs}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static <T extends Comparable<T>> void assertLessThanOrEqual(T lhs, T rhs, String msg) {
        if (!(compare(lhs, rhs, msg) <= 0)) {
            msg = Objects.toString(msg, "assertLessThanOrEqual")
                    + ": expected that " + Objects.toString(lhs)
                    + " <= " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Shorthand for {@link #assertEquals(Object, Object)}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertEquals(Object, Object)
     */
    public static void assertEQ(Object lhs, Object rhs) {
        assertEquals(lhs, rhs);
    }

    /**
     * Shorthand for {@link #assertEquals(Object, Object, String)}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @see #assertEquals(Object, Object, String)
     */
    public static void assertEQ(Object lhs, Object rhs, String msg) {
        assertEquals(lhs, rhs, msg);
    }

    /**
     * Calls {@link #assertEquals(java.lang.Object, java.lang.Object, java.lang.String)} with a default message.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertEquals(Object, Object, String)
     */
    public static void assertEquals(Object lhs, Object rhs) {
        assertEquals(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is equal to {@code rhs}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertEquals(Object lhs, Object rhs, String msg) {
        if ((lhs != rhs) && ((lhs == null) || !(lhs.equals(rhs)))) {
            msg = Objects.toString(msg, "assertEquals")
                    + ": expected " + Objects.toString(lhs)
                    + " to equal " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Calls {@link #assertSame(java.lang.Object, java.lang.Object, java.lang.String)} with a default message.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertSame(Object, Object, String)
     */
    public static void assertSame(Object lhs, Object rhs) {
        assertSame(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is the same as {@code rhs}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertSame(Object lhs, Object rhs, String msg) {
        if (lhs != rhs) {
            msg = Objects.toString(msg, "assertSame")
                    + ": expected " + Objects.toString(lhs)
                    + " to equal " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Shorthand for {@link #assertGreaterThanOrEqual(Comparable, Comparable)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertGreaterThanOrEqual(Comparable, Comparable)
     */
    public static <T extends Comparable<T>> void assertGTE(T lhs, T rhs) {
        assertGreaterThanOrEqual(lhs, rhs);
    }

    /**
     * Shorthand for {@link #assertGreaterThanOrEqual(Comparable, Comparable, String)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @see #assertGreaterThanOrEqual(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertGTE(T lhs, T rhs, String msg) {
        assertGreaterThanOrEqual(lhs, rhs, msg);
    }

    /**
     * Calls {@link #assertGreaterThanOrEqual(Comparable, Comparable, String)} with a default message.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertGreaterThanOrEqual(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertGreaterThanOrEqual(T lhs, T rhs) {
        assertGreaterThanOrEqual(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is greater than or equal to {@code rhs}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static <T extends Comparable<T>> void assertGreaterThanOrEqual(T lhs, T rhs, String msg) {
        if (!(compare(lhs, rhs, msg) >= 0)) {
            msg = Objects.toString(msg, "assertGreaterThanOrEqual")
                    + ": expected " + Objects.toString(lhs)
                    + " >= " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Shorthand for {@link #assertGreaterThan(Comparable, Comparable)}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertGreaterThan(Comparable, Comparable)
     */
    public static <T extends Comparable<T>> void assertGT(T lhs, T rhs) {
        assertGreaterThan(lhs, rhs);
    }

    /**
     * Shorthand for {@link #assertGreaterThan(Comparable, Comparable, String)}.
     *
     * @param <T> a type
     * @param lhs the left hand value
     * @param rhs the right hand value
     * @param msg A description of the assumption; {@code null} for a default message.
     * @see #assertGreaterThan(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertGT(T lhs, T rhs, String msg) {
        assertGreaterThan(lhs, rhs, msg);
    }

    /**
     * Calls {@link #assertGreaterThan(Comparable, Comparable, String)} with a default message.
     *
     * @param <T> a type
     * @param lhs the left hand value
     * @param rhs the right hand value
     * @see #assertGreaterThan(Comparable, Comparable, String)
     */
    public static <T extends Comparable<T>> void assertGreaterThan(T lhs, T rhs) {
        assertGreaterThan(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is greater than {@code rhs}.
     *
     * @param <T> a type
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static <T extends Comparable<T>> void assertGreaterThan(T lhs, T rhs, String msg) {
        if (!(compare(lhs, rhs, msg) > 0)) {
            msg = Objects.toString(msg, "assertGreaterThan")
                    + ": expected " + Objects.toString(lhs)
                    + " > " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Shorthand for {@link #assertNotEquals(Object, Object)}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertNotEquals(Object, Object)
     */
    public static void assertNE(Object lhs, Object rhs) {
        assertNotEquals(lhs, rhs);
    }

    /**
     * Shorthand for {@link #assertNotEquals(Object, Object, String)}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @see #assertNotEquals(Object, Object, String)
     */
    public static void assertNE(Object lhs, Object rhs, String msg) {
        assertNotEquals(lhs, rhs, msg);
    }

    /**
     * Calls {@link #assertNotEquals(Object, Object, String)} with a default message.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @see #assertNotEquals(Object, Object, String)
     */
    public static void assertNotEquals(Object lhs, Object rhs) {
        assertNotEquals(lhs, rhs, null);
    }

    /**
     * Asserts that {@code lhs} is not equal to {@code rhs}.
     *
     * @param lhs The left hand side of the comparison.
     * @param rhs The right hand side of the comparison.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertNotEquals(Object lhs, Object rhs, String msg) {
        if ((lhs == rhs) || (lhs != null && lhs.equals(rhs))) {
            msg = Objects.toString(msg, "assertNotEquals")
                    + ": expected " + Objects.toString(lhs)
                    + " to not equal " + Objects.toString(rhs);
            fail(msg);
        }
    }

    /**
     * Calls {@link #assertNull(Object, String)} with a default message.
     *
     * @param o The reference assumed to be null.
     * @see #assertNull(Object, String)
     */
    public static void assertNull(Object o) {
        assertNull(o, null);
    }

    /**
     * Asserts that {@code o} is null.
     *
     * @param o The reference assumed to be null.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertNull(Object o, String msg) {
        assertEquals(o, null, msg);
    }

    /**
     * Calls {@link #assertNotNull(Object, String)} with a default message.
     *
     * @param o The reference assumed <i>not</i> to be null,
     * @see #assertNotNull(Object, String)
     */
    public static void assertNotNull(Object o) {
        assertNotNull(o, null);
    }

    /**
     * Asserts that {@code o} is <i>not</i> null.
     *
     * @param o The reference assumed <i>not</i> to be null,
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertNotNull(Object o, String msg) {
        assertNotEquals(o, null, msg);
    }

    /**
     * Calls {@link #assertFalse(boolean, String)} with a default message.
     *
     * @param value The value assumed to be false.
     * @see #assertFalse(boolean, String)
     */
    public static void assertFalse(boolean value) {
        assertFalse(value, null);
    }

    /**
     * Asserts that {@code value} is {@code false}.
     *
     * @param value The value assumed to be false.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertFalse(boolean value, String msg) {
        if (value) {
            msg = Objects.toString(msg, "assertFalse")
                    + ": expected false, was true";
            fail(msg);
        }
    }

    /**
     * Calls {@link #assertTrue(boolean, String)} with a default message.
     *
     * @param value The value assumed to be true.
     * @see #assertTrue(boolean, String)
     */
    public static void assertTrue(boolean value) {
        assertTrue(value, null);
    }

    /**
     * Asserts that {@code value} is {@code true}.
     *
     * @param value The value assumed to be true.
     * @param msg A description of the assumption; {@code null} for a default message.
     * @throws RuntimeException if the assertion is not true.
     */
    public static void assertTrue(boolean value, String msg) {
        if (!value) {
            msg = Objects.toString(msg, "assertTrue")
                    + ": expected true, was false";
            fail(msg);
        }
    }

    private static <T extends Comparable<T>> int compare(T lhs, T rhs, String msg) {
        if (lhs == null || rhs == null) {
            fail(lhs, rhs, msg + ": values must be non-null:", ",");
        }
        return lhs.compareTo(rhs);
    }

/**
     * Asserts that two strings are equal.
     *
     * If strings are not equals, then exception message
     * will contain {@code msg} followed by list of mismatched lines.
     *
     * @param str1 First string to compare.
     * @param str2 Second string to compare.
     * @param msg A description of the assumption.
     * @throws RuntimeException if strings are not equal.
     */
    public static void assertStringsEqual(String str1, String str2,
                                          String msg) {
        String lineSeparator = System.getProperty("line.separator");
        String str1Lines[] = str1.split(lineSeparator);
        String str2Lines[] = str2.split(lineSeparator);

        int minLength = Math.min(str1Lines.length, str2Lines.length);
        String longestStringLines[] = ((str1Lines.length == minLength) ?
                                       str2Lines : str1Lines);

        boolean stringsAreDifferent = false;

        StringBuilder messageBuilder = new StringBuilder(msg);

        messageBuilder.append("\n");

        for (int line = 0; line < minLength; line++) {
            if (!str1Lines[line].equals(str2Lines[line])) {
                messageBuilder.append(String.
                                      format("[line %d] '%s' differs " +
                                             "from '%s'\n",
                                             line,
                                             str1Lines[line],
                                             str2Lines[line]));
                stringsAreDifferent = true;
            }
        }

        if (minLength < longestStringLines.length) {
            String stringName = ((longestStringLines == str1Lines) ?
                                 "first" : "second");
            messageBuilder.append(String.format("Only %s string contains " +
                                                "following lines:\n",
                                                stringName));
            stringsAreDifferent = true;
            for(int line = minLength; line < longestStringLines.length; line++) {
                messageBuilder.append(String.
                                      format("[line %d] '%s'", line,
                                             longestStringLines[line]));
            }
        }

        if (stringsAreDifferent) {
            fail(messageBuilder.toString());
        }
    }

    /**
     * Returns a string formatted with a message and expected and actual values.
     * @param lhs the actual value
     * @param rhs  the expected value
     * @param message the actual value
     * @param relation the asserted relationship between lhs and rhs
     * @return a formatted string
     */
    public static String format(Object lhs, Object rhs, String message, String relation) {
        StringBuilder sb = new StringBuilder(80);
        if (message != null) {
            sb.append(message);
            sb.append(' ');
        }
        sb.append("<");
        sb.append(Objects.toString(lhs));
        sb.append("> ");
        sb.append(Objects.toString(relation, ","));
        sb.append(" <");
        sb.append(Objects.toString(rhs));
        sb.append(">");
        return sb.toString();
    }

    /**
     * Fail reports a failure with message fail.
     *
     * @throws RuntimeException always
     */
    public static void fail() {
        fail("fail");
    }

    /**
     * Fail reports a failure with a message.
     * @param message for the failure
     * @throws RuntimeException always
     */
    public static void fail(String message) {
        throw new RuntimeException(message);
    }

    /**
     * Fail reports a failure with a formatted message.
     *
     * @param lhs the actual value
     * @param rhs the expected value
     * @param message to be format before the expected and actual values
     * @param relation the asserted relationship between lhs and rhs
     * @throws RuntimeException always
     */
    public static void fail(Object lhs, Object rhs, String message, String relation) {
        throw new RuntimeException(format(lhs, rhs, message, relation));
    }

    /**
     * Fail reports a failure with a message and a cause.
     * @param message to be format before the expected and actual values
     * @param cause the exception that caused this failure
     * @throws RuntimeException always
     */
    public static void fail(String message, Throwable cause) {
        throw new RuntimeException(message, cause);
    }

}
