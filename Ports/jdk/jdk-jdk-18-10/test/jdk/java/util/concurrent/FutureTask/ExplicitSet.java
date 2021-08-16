/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7132378
 * @summary Race in FutureTask if used with explicit set ( not Runnable )
 * @author Chris Hegarty
 */

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ExplicitSet {

    static void realMain(String[] args) throws Throwable {
        for (int i = 1; i <= 10000; i++) {
            //System.out.print(".");
            test();
        }
    }

    static void test() throws Throwable {
        final SettableTask task = new SettableTask();

        Thread thread = new Thread() { public void run() {
            try {
                check(task.get() != null);
            } catch (Exception e) { unexpected(e); }
        }};
        thread.start();

        task.set(Boolean.TRUE);
        thread.join(5000);
    }

    static class SettableTask extends FutureTask<Boolean> {
        SettableTask() {
            super(new Callable<Boolean>() {
                    public Boolean call() {
                        fail("The task should never be run!");
                        return null;
                    }
                });
        }

        @Override
        public void set(Boolean b) {
            super.set(b);
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
