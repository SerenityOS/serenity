/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Objects.checkIndex/jdk.internal.util.Preconditions.checkIndex tests for int values
 * @run testng CheckIndex
 * @bug 8135248 8142493 8155794
 * @modules java.base/jdk.internal.util
 */

import jdk.internal.util.Preconditions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;

import static org.testng.Assert.*;

public class CheckIndex {

    static class AssertingOutOfBoundsException extends RuntimeException {
        public AssertingOutOfBoundsException(String message) {
            super(message);
        }
    }

    static BiFunction<String, List<Number>, AssertingOutOfBoundsException> assertingOutOfBounds(
            String message, String expCheckKind, Integer... expArgs) {
        return (checkKind, args) -> {
            assertEquals(checkKind, expCheckKind);
            assertEquals(args, List.of(expArgs));
            try {
                args.clear();
                fail("Out of bounds List<Integer> argument should be unmodifiable");
            } catch (Exception e)  {
            }
            return new AssertingOutOfBoundsException(message);
        };
    }

    static BiFunction<String, List<Number>, AssertingOutOfBoundsException> assertingOutOfBoundsReturnNull(
            String expCheckKind, Integer... expArgs) {
        return (checkKind, args) -> {
            assertEquals(checkKind, expCheckKind);
            assertEquals(args, List.of(expArgs));
            return null;
        };
    }

    static final int[] VALUES = {0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, -1, Integer.MIN_VALUE + 1, Integer.MIN_VALUE};

    @DataProvider
    static Object[][] checkIndexProvider() {
        List<Object[]> l = new ArrayList<>();
        for (int index : VALUES) {
            for (int length : VALUES) {
                boolean withinBounds = index >= 0 &&
                                       length >= 0 &&
                                       index < length;
                l.add(new Object[]{index, length, withinBounds});
            }
        }
        return l.toArray(Object[][]::new);
    }

    @Test(dataProvider = "checkIndexProvider")
    public void testCheckIndex(int index, int length, boolean withinBounds) {
        String expectedMessage = withinBounds
                                 ? null
                                 : Preconditions.outOfBoundsExceptionFormatter(IndexOutOfBoundsException::new).
                apply("checkIndex", List.of(index, length)).getMessage();

        BiConsumer<Class<? extends RuntimeException>, IntSupplier> checker = (ec, s) -> {
            try {
                int rIndex = s.getAsInt();
                if (!withinBounds)
                    fail(String.format(
                            "Index %d is out of bounds of [0, %d), but was reported to be within bounds", index, length));
                assertEquals(rIndex, index);
            }
            catch (RuntimeException e) {
                assertTrue(ec.isInstance(e));
                if (withinBounds)
                    fail(String.format(
                            "Index %d is within bounds of [0, %d), but was reported to be out of bounds", index, length));
                else
                    assertEquals(e.getMessage(), expectedMessage);
            }
        };

        checker.accept(AssertingOutOfBoundsException.class,
                     () -> Preconditions.checkIndex(index, length,
                                                    assertingOutOfBounds(expectedMessage, "checkIndex", index, length)));
        checker.accept(IndexOutOfBoundsException.class,
                     () -> Preconditions.checkIndex(index, length,
                                                    assertingOutOfBoundsReturnNull("checkIndex", index, length)));
        checker.accept(IndexOutOfBoundsException.class,
                     () -> Preconditions.checkIndex(index, length, null));
        checker.accept(IndexOutOfBoundsException.class,
                     () -> Objects.checkIndex(index, length));
        checker.accept(ArrayIndexOutOfBoundsException.class,
                     () -> Preconditions.checkIndex(index, length,
                                                    Preconditions.outOfBoundsExceptionFormatter(ArrayIndexOutOfBoundsException::new)));
        checker.accept(StringIndexOutOfBoundsException.class,
                     () -> Preconditions.checkIndex(index, length,
                                                    Preconditions.outOfBoundsExceptionFormatter(StringIndexOutOfBoundsException::new)));
    }


    @DataProvider
    static Object[][] checkFromToIndexProvider() {
        List<Object[]> l = new ArrayList<>();
        for (int fromIndex : VALUES) {
            for (int toIndex : VALUES) {
                for (int length : VALUES) {
                    boolean withinBounds = fromIndex >= 0 &&
                                           toIndex >= 0 &&
                                           length >= 0 &&
                                           fromIndex <= toIndex &&
                                           toIndex <= length;
                    l.add(new Object[]{fromIndex, toIndex, length, withinBounds});
                }
            }
        }
        return l.toArray(Object[][]::new);
    }

    @Test(dataProvider = "checkFromToIndexProvider")
    public void testCheckFromToIndex(int fromIndex, int toIndex, int length, boolean withinBounds) {
        String expectedMessage = withinBounds
                                 ? null
                                 : Preconditions.outOfBoundsExceptionFormatter(IndexOutOfBoundsException::new).
                apply("checkFromToIndex", List.of(fromIndex, toIndex, length)).getMessage();

        BiConsumer<Class<? extends RuntimeException>, IntSupplier> check = (ec, s) -> {
            try {
                int rIndex = s.getAsInt();
                if (!withinBounds)
                    fail(String.format(
                            "Range [%d, %d) is out of bounds of [0, %d), but was reported to be withing bounds", fromIndex, toIndex, length));
                assertEquals(rIndex, fromIndex);
            }
            catch (RuntimeException e) {
                assertTrue(ec.isInstance(e));
                if (withinBounds)
                    fail(String.format(
                            "Range [%d, %d) is within bounds of [0, %d), but was reported to be out of bounds", fromIndex, toIndex, length));
                else
                    assertEquals(e.getMessage(), expectedMessage);
            }
        };

        check.accept(AssertingOutOfBoundsException.class,
                     () -> Preconditions.checkFromToIndex(fromIndex, toIndex, length,
                                                          assertingOutOfBounds(expectedMessage, "checkFromToIndex", fromIndex, toIndex, length)));
        check.accept(IndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromToIndex(fromIndex, toIndex, length,
                                                          assertingOutOfBoundsReturnNull("checkFromToIndex", fromIndex, toIndex, length)));
        check.accept(IndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromToIndex(fromIndex, toIndex, length, null));
        check.accept(IndexOutOfBoundsException.class,
                     () -> Objects.checkFromToIndex(fromIndex, toIndex, length));
        check.accept(ArrayIndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromToIndex(fromIndex, toIndex, length,
                                                          Preconditions.outOfBoundsExceptionFormatter(ArrayIndexOutOfBoundsException::new)));
        check.accept(StringIndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromToIndex(fromIndex, toIndex, length,
                                                          Preconditions.outOfBoundsExceptionFormatter(StringIndexOutOfBoundsException::new)));
    }


    @DataProvider
    static Object[][] checkFromIndexSizeProvider() {
        List<Object[]> l = new ArrayList<>();
        for (int fromIndex : VALUES) {
            for (int size : VALUES) {
                for (int length : VALUES) {
                    // Explicitly convert to long
                    long lFromIndex = fromIndex;
                    long lSize = size;
                    long lLength = length;
                    // Avoid overflow
                    long lToIndex = lFromIndex + lSize;

                    boolean withinBounds = lFromIndex >= 0L &&
                                           lSize >= 0L &&
                                           lLength >= 0L &&
                                           lFromIndex <= lToIndex &&
                                           lToIndex <= lLength;
                    l.add(new Object[]{fromIndex, size, length, withinBounds});
                }
            }
        }
        return l.toArray(Object[][]::new);
    }

    @Test(dataProvider = "checkFromIndexSizeProvider")
    public void testCheckFromIndexSize(int fromIndex, int size, int length, boolean withinBounds) {
        String expectedMessage = withinBounds
                                 ? null
                                 : Preconditions.outOfBoundsExceptionFormatter(IndexOutOfBoundsException::new).
                apply("checkFromIndexSize", List.of(fromIndex, size, length)).getMessage();

        BiConsumer<Class<? extends RuntimeException>, IntSupplier> check = (ec, s) -> {
            try {
                int rIndex = s.getAsInt();
                if (!withinBounds)
                    fail(String.format(
                            "Range [%d, %d + %d) is out of bounds of [0, %d), but was reported to be withing bounds", fromIndex, fromIndex, size, length));
                assertEquals(rIndex, fromIndex);
            }
            catch (RuntimeException e) {
                assertTrue(ec.isInstance(e));
                if (withinBounds)
                    fail(String.format(
                            "Range [%d, %d + %d) is within bounds of [0, %d), but was reported to be out of bounds", fromIndex, fromIndex, size, length));
                else
                    assertEquals(e.getMessage(), expectedMessage);
            }
        };

        check.accept(AssertingOutOfBoundsException.class,
                     () -> Preconditions.checkFromIndexSize(fromIndex, size, length,
                                                            assertingOutOfBounds(expectedMessage, "checkFromIndexSize", fromIndex, size, length)));
        check.accept(IndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromIndexSize(fromIndex, size, length,
                                                            assertingOutOfBoundsReturnNull("checkFromIndexSize", fromIndex, size, length)));
        check.accept(IndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromIndexSize(fromIndex, size, length, null));
        check.accept(IndexOutOfBoundsException.class,
                     () -> Objects.checkFromIndexSize(fromIndex, size, length));
        check.accept(ArrayIndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromIndexSize(fromIndex, size, length,
                                                            Preconditions.outOfBoundsExceptionFormatter(ArrayIndexOutOfBoundsException::new)));
        check.accept(StringIndexOutOfBoundsException.class,
                     () -> Preconditions.checkFromIndexSize(fromIndex, size, length,
                                                            Preconditions.outOfBoundsExceptionFormatter(StringIndexOutOfBoundsException::new)));
    }

    @Test
    public void uniqueMessagesForCheckKinds() {
        BiFunction<String, List<Number>, IndexOutOfBoundsException> f =
                Preconditions.outOfBoundsExceptionFormatter(IndexOutOfBoundsException::new);

        List<String> messages = new ArrayList<>();
        // Exact arguments
        messages.add(f.apply("checkIndex", List.of(-1, 0)).getMessage());
        messages.add(f.apply("checkFromToIndex", List.of(-1, 0, 0)).getMessage());
        messages.add(f.apply("checkFromIndexSize", List.of(-1, 0, 0)).getMessage());
        // Unknown check kind
        messages.add(f.apply("checkUnknown", List.of(-1, 0, 0)).getMessage());
        // Known check kind with more arguments
        messages.add(f.apply("checkIndex", List.of(-1, 0, 0)).getMessage());
        messages.add(f.apply("checkFromToIndex", List.of(-1, 0, 0, 0)).getMessage());
        messages.add(f.apply("checkFromIndexSize", List.of(-1, 0, 0, 0)).getMessage());
        // Known check kind with fewer arguments
        messages.add(f.apply("checkIndex", List.of(-1)).getMessage());
        messages.add(f.apply("checkFromToIndex", List.of(-1, 0)).getMessage());
        messages.add(f.apply("checkFromIndexSize", List.of(-1, 0)).getMessage());
        // Null arguments
        messages.add(f.apply(null, null).getMessage());
        messages.add(f.apply("checkNullArguments", null).getMessage());
        messages.add(f.apply(null, List.of(-1)).getMessage());

        assertEquals(messages.size(), messages.stream().distinct().count());
    }
}
