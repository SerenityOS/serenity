/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8037857
 * @summary tests for stream and spliterator factory methods
 * @run testng StreamAndSpliterator
 */

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Spliterators;

import org.testng.Assert.ThrowingRunnable;

import static org.testng.Assert.assertThrows;

public class StreamAndSpliterator {
    @Test
    public void testStreamNPEs() {
        assertThrowsNPE(() -> Arrays.stream((int[]) null, 0, 0));
        assertThrowsNPE(() -> Arrays.stream((long[]) null, 0, 0));
        assertThrowsNPE(() -> Arrays.stream((double[]) null, 0, 0));
        assertThrowsNPE(() -> Arrays.stream((String[]) null, 0, 0));
    }

    @Test
    public void testStreamAIOBEs() {
        // origin > fence
        assertThrowsAIOOB(() -> Arrays.stream(new int[]{}, 1, 0));
        assertThrowsAIOOB(() -> Arrays.stream(new long[]{}, 1, 0));
        assertThrowsAIOOB(() -> Arrays.stream(new double[]{}, 1, 0));
        assertThrowsAIOOB(() -> Arrays.stream(new String[]{}, 1, 0));

        // bad origin
        assertThrowsAIOOB(() -> Arrays.stream(new int[]{}, -1, 0));
        assertThrowsAIOOB(() -> Arrays.stream(new long[]{}, -1, 0));
        assertThrowsAIOOB(() -> Arrays.stream(new double[]{}, -1, 0));
        assertThrowsAIOOB(() -> Arrays.stream(new String[]{}, -1, 0));

        // bad fence
        assertThrowsAIOOB(() -> Arrays.stream(new int[]{}, 0, 1));
        assertThrowsAIOOB(() -> Arrays.stream(new long[]{}, 0, 1));
        assertThrowsAIOOB(() -> Arrays.stream(new double[]{}, 0, 1));
        assertThrowsAIOOB(() -> Arrays.stream(new String[]{}, 0, 1));
    }


    @Test
    public void testSpliteratorNPEs() {
        assertThrowsNPE(() -> Arrays.spliterator((int[]) null, 0, 0));
        assertThrowsNPE(() -> Arrays.spliterator((long[]) null, 0, 0));
        assertThrowsNPE(() -> Arrays.spliterator((double[]) null, 0, 0));
        assertThrowsNPE(() -> Arrays.spliterator((String[]) null, 0, 0));
    }

    @Test
    public void testSpliteratorAIOBEs() {
        // origin > fence
        assertThrowsAIOOB(() -> Arrays.spliterator(new int[]{}, 1, 0));
        assertThrowsAIOOB(() -> Arrays.spliterator(new long[]{}, 1, 0));
        assertThrowsAIOOB(() -> Arrays.spliterator(new double[]{}, 1, 0));
        assertThrowsAIOOB(() -> Arrays.spliterator(new String[]{}, 1, 0));

        // bad origin
        assertThrowsAIOOB(() -> Arrays.spliterator(new int[]{}, -1, 0));
        assertThrowsAIOOB(() -> Arrays.spliterator(new long[]{}, -1, 0));
        assertThrowsAIOOB(() -> Arrays.spliterator(new double[]{}, -1, 0));
        assertThrowsAIOOB(() -> Arrays.spliterator(new String[]{}, -1, 0));

        // bad fence
        assertThrowsAIOOB(() -> Arrays.spliterator(new int[]{}, 0, 1));
        assertThrowsAIOOB(() -> Arrays.spliterator(new long[]{}, 0, 1));
        assertThrowsAIOOB(() -> Arrays.spliterator(new double[]{}, 0, 1));
        assertThrowsAIOOB(() -> Arrays.spliterator(new String[]{}, 0, 1));
    }


    @Test
    public void testSpliteratorNPEsFromSpliterators() {
        assertThrowsNPE(() -> Spliterators.spliterator((int[]) null, 0, 0, 0));
        assertThrowsNPE(() -> Spliterators.spliterator((long[]) null, 0, 0, 0));
        assertThrowsNPE(() -> Spliterators.spliterator((double[]) null, 0, 0, 0));
        assertThrowsNPE(() -> Spliterators.spliterator((String[]) null, 0, 0, 0));
    }

    @Test
    public void testSpliteratorAIOBEsFromSpliterators() {
        // origin > fence
        assertThrowsAIOOB(() -> Spliterators.spliterator(new int[]{}, 1, 0, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new long[]{}, 1, 0, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new double[]{}, 1, 0, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new String[]{}, 1, 0, 0));

        // bad origin
        assertThrowsAIOOB(() -> Spliterators.spliterator(new int[]{}, -1, 0, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new long[]{}, -1, 0, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new double[]{}, -1, 0, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new String[]{}, -1, 0, 0));

        // bad fence
        assertThrowsAIOOB(() -> Spliterators.spliterator(new int[]{}, 0, 1, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new long[]{}, 0, 1, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new double[]{}, 0, 1, 0));
        assertThrowsAIOOB(() -> Spliterators.spliterator(new String[]{}, 0, 1, 0));
    }

    void assertThrowsNPE(ThrowingRunnable r) {
        assertThrows(NullPointerException.class, r);
    }

    void assertThrowsAIOOB(ThrowingRunnable r) {
        assertThrows(ArrayIndexOutOfBoundsException.class, r);
    }
}
