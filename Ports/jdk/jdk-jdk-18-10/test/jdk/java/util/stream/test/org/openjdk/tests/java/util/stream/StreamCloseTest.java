/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary close handlers and closing streams
 * @bug 8044047 8147505
 */

package org.openjdk.tests.java.util.stream;

import java.util.Arrays;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.util.stream.LambdaTestHelpers.countTo;
import static java.util.stream.ThrowableHelper.checkNPE;
import static java.util.stream.ThrowableHelper.checkISE;

@Test(groups = { "serialization-hostile" })
public class StreamCloseTest extends OpTestCase {
    public void testNullCloseHandler() {
        checkNPE(() -> Stream.of(1).onClose(null));
    }

    public void testEmptyCloseHandler() {
        try (Stream<Integer> ints = countTo(100).stream()) {
            ints.forEach(i -> {});
        }
    }

    public void testOneCloseHandler() {
        final boolean[] holder = new boolean[1];
        Runnable closer = () -> { holder[0] = true; };

        try (Stream<Integer> ints = countTo(100).stream()) {
            ints.onClose(closer);
            ints.forEach(i -> {});
        }
        assertTrue(holder[0]);

        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().onClose(closer)) {
            ints.forEach(i -> {});
        }
        assertTrue(holder[0]);

        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().filter(e -> true).onClose(closer)) {
            ints.forEach(i -> {});
        }
        assertTrue(holder[0]);

        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().filter(e -> true).onClose(closer).filter(e -> true)) {
            ints.forEach(i -> {});
        }
        assertTrue(holder[0]);
    }

    public void testTwoCloseHandlers() {
        final boolean[] holder = new boolean[2];
        Runnable close1 = () -> { holder[0] = true; };
        Runnable close2 = () -> { holder[1] = true; };

        try (Stream<Integer> ints = countTo(100).stream()) {
            ints.onClose(close1).onClose(close2);
            ints.forEach(i -> {});
        }
        assertTrue(holder[0] && holder[1]);

        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().onClose(close1).onClose(close2)) {
            ints.forEach(i -> {});
        }
        assertTrue(holder[0] && holder[1]);

        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().filter(e -> true).onClose(close1).onClose(close2)) {
            ints.forEach(i -> {});
        }
        assertTrue(holder[0] && holder[1]);

        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().filter(e -> true).onClose(close1).onClose(close2).filter(e -> true)) {
            ints.forEach(i -> {});
        }
        assertTrue(holder[0] && holder[1]);
    }

    public void testCascadedExceptions() {
        final boolean[] holder = new boolean[3];
        boolean caught = false;
        Runnable close1 = () -> { holder[0] = true; throw new RuntimeException("1"); };
        Runnable close2 = () -> { holder[1] = true; throw new RuntimeException("2"); };
        Runnable close3 = () -> { holder[2] = true; throw new RuntimeException("3"); };

        try (Stream<Integer> ints = countTo(100).stream()) {
            ints.onClose(close1).onClose(close2).onClose(close3);
            ints.forEach(i -> {});
        }
        catch (RuntimeException e) {
            assertCascaded(e, 3);
            assertTrue(holder[0] && holder[1] && holder[2]);
            caught = true;
        }
        assertTrue(caught);

        Arrays.fill(holder, false);
        caught = false;
        try (Stream<Integer> ints = countTo(100).stream().onClose(close1).onClose(close2).onClose(close3)) {
            ints.forEach(i -> {});
        }
        catch (RuntimeException e) {
            assertCascaded(e, 3);
            assertTrue(holder[0] && holder[1] && holder[2]);
            caught = true;
        }
        assertTrue(caught);

        caught = false;
        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().filter(e -> true).onClose(close1).onClose(close2).onClose(close3)) {
            ints.forEach(i -> {});
        }
        catch (RuntimeException e) {
            assertCascaded(e, 3);
            assertTrue(holder[0] && holder[1] && holder[2]);
            caught = true;
        }
        assertTrue(caught);

        caught = false;
        Arrays.fill(holder, false);
        try (Stream<Integer> ints = countTo(100).stream().filter(e -> true).onClose(close1).onClose(close2).filter(e -> true).onClose(close3)) {
            ints.forEach(i -> {});
        }
        catch (RuntimeException e) {
            assertCascaded(e, 3);
            assertTrue(holder[0] && holder[1] && holder[2]);
            caught = true;
        }
        assertTrue(caught);
    }

    private void assertCascaded(RuntimeException e, int n) {
        assertTrue(e.getMessage().equals("1"));
        assertTrue(e.getSuppressed().length == n - 1);
        for (int i=0; i<n-1; i++)
        assertTrue(e.getSuppressed()[i].getMessage().equals(String.valueOf(i + 2)));
    }

    public void testConsumed() {
        try(Stream<Integer> s = countTo(100).stream()) {
            s.forEach(i -> {});
            // Adding onClose handler when stream is consumed is illegal
            // handler must not be registered
            checkISE(() -> s.onClose(() -> fail("1")));
        }

        // close() must be idempotent:
        // second close() invoked at the end of try-with-resources must have no effect
        try(Stream<Integer> s = countTo(100).stream()) {
            s.close();
            // Adding onClose handler when stream is closed is also illegal
            checkISE(() -> s.onClose(() -> fail("3")));
        }
    }
}
