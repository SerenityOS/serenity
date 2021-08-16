/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6207984 6268068
 * @summary Test contains/remove equator compatibility
 * @author  Martin Buchholz
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class RemoveContains {
    static volatile int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        new AssertionError(msg).printStackTrace();
    }

    static void pass() {
        passed++;
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    static void check(boolean condition, String msg) {
        if (condition)
            passed++;
        else
            fail(msg);
    }

    static void check(boolean condition) {
        check(condition, "Assertion failure");
    }

    public static void main(String[] args) {
        final Comparator<String> firstChar = new Comparator<>() {
            public int compare(String x, String y) {
                return x.charAt(0) - y.charAt(0); }};

        test(new PriorityQueue<String>(firstChar));
        test(new PriorityQueue<String>(10, firstChar));
        test(new PriorityBlockingQueue<String>(10, firstChar));
        test(new ArrayBlockingQueue<String>(10));
        test(new LinkedBlockingQueue<String>(10));
        test(new LinkedBlockingDeque<String>(10));
        test(new LinkedTransferQueue<String>());
        test(new ArrayDeque<String>(10));

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Error("Some tests failed");
    }

    private static void test(Queue<String> q) {
        try {
            List<String> words =
                Arrays.asList("foo", "fee", "fi", "fo", "fum",
                              "Englishman");
            q.addAll(words);
            for (String word : words)
                check(q.contains(word));
            check(! q.contains("flurble"));

            check(q.remove("fi"));
            for (String word : words)
                check(q.contains(word) ^ word.equals("fi"));

            check(! q.remove("fi"));
            check(! q.remove("flurble"));

        } catch (Throwable t) { unexpected(t); }
    }
}
