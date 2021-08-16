/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6631352
 * @summary Check that appends are atomic
 */

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class AtomicAppend {
    // Before the fix for
    // 6631352: Implement atomic append mode using FILE_APPEND_DATA (win)
    // this would fail intermittently on windows
    void test(String[] args) throws Throwable {
        final int nThreads = 10;
        final int writes = 1000;
        final File file = new File("foo");
        file.delete();
        try {
            final ExecutorService es = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < nThreads; i++)
                es.execute(new Runnable() { public void run() {
                    try {
                        try (FileOutputStream s = new FileOutputStream(file, true)) {
                            for (int j = 0; j < 1000; j++) {
                                s.write((int) 'x');
                                s.flush();
                            }
                        }
                    } catch (Throwable t) { unexpected(t); }}});
            es.shutdown();
            es.awaitTermination(10L, TimeUnit.MINUTES);
            equal(file.length(), (long) (nThreads * writes));
        } finally {
            file.delete();
        }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new AtomicAppend().instanceMain(args);
    }
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
