/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Martin Buchholz and Doug Lea with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @summary Should be able to shutdown a pool when worker creation failed.
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.Utils;

public class FlakyThreadFactory {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    void test(String[] args) throws Throwable {
        test(NullPointerException.class,
             new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    throw new NullPointerException();
                }});
        test(OutOfMemoryError.class,
             new ThreadFactory() {
                 @SuppressWarnings("DeadThread")
                 public Thread newThread(Runnable r) {
                     // We expect this to throw OOME, but ...
                     new Thread(null, r, "a natural OOME", 1L << 60);
                     // """On some platforms, the value of the stackSize
                     // parameter may have no effect whatsoever."""
                     throw new OutOfMemoryError("artificial OOME");
                 }});
        test(null,
             new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return null;
                }});
    }

    void test(final Class<?> exceptionClass,
              final ThreadFactory failingThreadFactory)
            throws Throwable {
        ThreadFactory flakyThreadFactory = new ThreadFactory() {
            int seq = 0;
            public Thread newThread(Runnable r) {
                if (seq++ < 4)
                    return new Thread(r);
                else
                    return failingThreadFactory.newThread(r);
            }};
        ThreadPoolExecutor pool =
            new ThreadPoolExecutor(10, 10,
                                   0L, TimeUnit.SECONDS,
                                   new LinkedBlockingQueue(),
                                   flakyThreadFactory);
        try {
            for (int i = 0; i < 8; i++)
                pool.submit(new Runnable() { public void run() {} });
            check(exceptionClass == null);
        } catch (Throwable t) {
            /* t.printStackTrace(); */
            check(exceptionClass.isInstance(t));
        }
        pool.shutdown();
        check(pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
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
        new FlakyThreadFactory().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
