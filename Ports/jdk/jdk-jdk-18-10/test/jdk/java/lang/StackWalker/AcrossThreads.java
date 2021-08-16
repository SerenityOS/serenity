/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143214
 * @summary Verify that StackWalker works well when one instance of StackWalker
 *          is used by several threads sequentially or concurrently.
 * @run testng AcrossThreads
 */

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.StackWalker.StackFrame;
import static java.lang.StackWalker.Option.*;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class AcrossThreads {
    static final StackWalker WALKERS[] = new StackWalker[] {
            StackWalker.getInstance(RETAIN_CLASS_REFERENCE),
            StackWalker.getInstance(EnumSet.of(SHOW_REFLECT_FRAMES, RETAIN_CLASS_REFERENCE)),
            StackWalker.getInstance(EnumSet.of(SHOW_HIDDEN_FRAMES, RETAIN_CLASS_REFERENCE))
    };

    @DataProvider
    public StackWalker[][] walkerProvider() {
        return new StackWalker[][] {
                new StackWalker[] { WALKERS[0] },
                new StackWalker[] { WALKERS[1] },
                new StackWalker[] { WALKERS[2] }
        };
    }

    @Test(dataProvider = "walkerProvider")
    public void test(StackWalker walker) {
        Thread t1 = new T1(walker);
        // call methods of one instance of StackWalker sequentially in T1, T2, T3.
        t1.start();
        try {
            t1.join();
        } catch (InterruptedException e) { }

        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 100; i++) {
            threads.add(new T1(walker));
            threads.add(new T2(walker));
            threads.add(new T3(walker));
        }
        // call methods of one instance of StackWalker concurrently in several threads.
        threads.parallelStream().forEach(t -> {
            t.setDaemon(true);
            t.start();
        });
        threads.parallelStream().forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) { }
        });
    }

    interface Consumer {
        final int LOOPS = 5;

        public void consume();

        default public void assertWalker(StackWalker walker, int n) {
            if (--n == 0) {
                Map<String, Integer> methods = new HashMap<String, Integer>();
                walker.forEach(f -> {
                    Integer i = methods.putIfAbsent(f.getMethodName(), 1);
                    if (i != null) {
                        methods.put(f.getMethodName(), i + 1);
                    }
                });

                // verify that walker.forEach(...) reaches the specified methods.
                assertTrue(methods.get("consume") == 1);
                assertTrue(methods.get("run") == 1);
                assertTrue(methods.get("assertWalker") == LOOPS);

                // verify that walker.walk(...) reaches the specified methods.
                assertTrue(walker.walk(s -> s.map(StackFrame::getMethodName)
                                             .filter(mn -> mn.equals("consume"))
                                             .count()) == 1);
                assertTrue(walker.walk(s -> s.map(StackFrame::getMethodName)
                                             .filter(mn -> mn.equals("run"))
                                             .count()) == 1);
                assertTrue(walker.walk(s -> s.map(StackFrame::getMethodName)
                                             .filter(mn -> mn.equals("assertWalker"))
                                             .count()) == LOOPS);
            } else {
                assertWalker(walker, n);
            }
        }
    }

    class T1 extends Thread implements Consumer {
        final StackWalker walker;

        public T1(StackWalker walker) {
            this.walker = walker;
        }

        public void run() {
            consume();

            Thread t2 = new T2(walker);
            t2.start();
            try {
                t2.join();
            } catch (InterruptedException e) { }

            consume();
        }

        public void consume() {
            assertWalker(walker, LOOPS);

            // verify walker.walk() reaches T1 class through methods run() and consume().
            assertTrue(walker.walk(s -> s.filter(f -> T1.class == f.getDeclaringClass())
                                         .count()) == 2);

            assertCallerClass(walker);
            assertEquals(T1.class, walker.getCallerClass());
        }
    }

    class T2 extends Thread implements Consumer {
        final StackWalker walker;

        public T2(StackWalker walker) {
            this.walker = walker;
        }

        public void run() {
            consume();

            Thread t3 = new T3(walker);
            t3.start();
            try {
                t3.join();
            } catch (InterruptedException e) { }

            consume();
        }

        public void consume() {
            assertWalker(walker, LOOPS);

            // verify walker.walk() reaches T2 class through methods run() and consume().
            assertTrue(walker.walk(s -> s.filter(f -> T2.class == f.getDeclaringClass())
                                         .count()) == 2);
            // verify T1 is not reached, even if call is invoked
            // from test()->T1.start()->T1.run()->T2
            assertTrue(walker.walk(s -> s.filter(f -> T1.class == f.getDeclaringClass())
                                         .count()) == 0);

            assertCallerClass(walker);
            assertEquals(T2.class, walker.getCallerClass());
        }
    }

    class T3 extends Thread implements Consumer {
        final StackWalker walker;

        public T3(StackWalker walker) {
            this.walker = walker;
        }

        public void run() {
            consume();
        }

        public void consume() {
            assertWalker(walker, LOOPS);

            // verify walker.walk() reaches T1 class through methods run() and consume().
            assertTrue(walker.walk(s -> s.filter(f -> T3.class == f.getDeclaringClass())
                                         .count()) == 2);
            // verify T1, T2 is not reached, even if call is invoked
            // from test() -> T1.start() -> T1.run() -> T2.start() -> T2.run() -> T3
            assertTrue(walker.walk(s -> s.filter(f -> T2.class == f.getDeclaringClass())
                                         .count()) == 0);
            assertTrue(walker.walk(s -> s.filter(f -> T1.class == f.getDeclaringClass())
                                         .count()) == 0);

            assertCallerClass(walker);
            assertEquals(T3.class, walker.getCallerClass());
        }
    }

    static void assertCallerClass(StackWalker walker) {
        // verify walker.getCallerClass() get the expected class.
        call(walker);
    }

    static void call(StackWalker walker) {
        Class<?> c = walker.getCallerClass();
        assertEquals(c, AcrossThreads.class);
    }
}
