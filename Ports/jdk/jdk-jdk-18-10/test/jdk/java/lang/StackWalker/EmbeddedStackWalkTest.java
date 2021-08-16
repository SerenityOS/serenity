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


/**
 * @test
 * @bug 8143214
 * @summary Verify StackWalker works well when embedded in another
 *          StackWalker's functions.
 * @run testng/othervm EmbeddedStackWalkTest
 */

import java.lang.StackWalker.StackFrame;
import static java.lang.StackWalker.Option.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.EnumSet;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class EmbeddedStackWalkTest {
    static final StackWalker WALKERS[] = new StackWalker[] {
            StackWalker.getInstance(RETAIN_CLASS_REFERENCE),
            StackWalker.getInstance(EnumSet.of(SHOW_REFLECT_FRAMES, RETAIN_CLASS_REFERENCE)),
            StackWalker.getInstance(EnumSet.of(SHOW_HIDDEN_FRAMES, RETAIN_CLASS_REFERENCE))
    };

    static final int BIG_LOOP   = 30;
    static final int SMALL_LOOP = 5;

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
        C1.call(walker, BIG_LOOP);
    }

    // line numbers are hardcoded for now.
    // Should annotate the line numbers and auto-generated these constants
    // for test verification instead
    static final int BEGIN_LINE = 71;   // the begin line number of approximate range.
    static final int END_LINE   = 136;  // the end line number of approximate range.
    static class C1 { // here is the begin line number of approximate range, L71.
        public static void call(StackWalker walker, int loops) {
            if (loops == 0) {
                String caller = walker.walk(s ->
                    s.map(StackFrame::getClassName)
                     .filter(cn -> !cn.startsWith("jdk.internal.reflect.") && !cn.startsWith("java.lang.invoke"))
                     .skip(2).findFirst()
                ).get();
                assertEquals(caller, C1.class.getName());

                walker.forEach(f -> C2.testEmbeddedWalker());
            } else {
                call(walker, --loops);
            }
        }
    }

    static class C2 {
        static final StackWalker embeddedWalkers[] = new StackWalker[] {
            StackWalker.getInstance(),
            StackWalker.getInstance(SHOW_REFLECT_FRAMES),
            StackWalker.getInstance(SHOW_HIDDEN_FRAMES)
        };

        public static void testEmbeddedWalker() {
            walk(SMALL_LOOP);
        }

        static void walk(int loops) {
            if (loops == 0) {
                Arrays.stream(embeddedWalkers)
                      .forEach(walker -> run(walker));
            } else {
                walk(--loops);
            }
        }

        static void run(StackWalker walker) {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = null;
            try {
                handle = lookup.findStatic(C2.class, "call",
                        MethodType.methodType(void.class, StackWalker.class));
                handle.invoke(walker);
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }

        static void call(StackWalker walker) {
            String caller = walker.walk(s ->
                s.map(StackFrame::getClassName)
                 .filter(cn -> !cn.startsWith("jdk.internal.reflect.") && !cn.startsWith("java.lang.invoke"))
                 .skip(2).findFirst()
            ).get();
            assertEquals(caller, C2.class.getName());

            verify(walker, C1.class, "call");
            verify(walker, C2.class, "call");
            verify(walker, C2.class, "run");
            verify(walker, C2.class, "walk");
            verify(walker, C2.class, "testEmbeddedWalker");
        } // here is the end line number of approximate range, L136.

        static void verify(StackWalker walker, Class<?> c, String mn) {
            final String fileName = "EmbeddedStackWalkTest.java";
            walker.walk(s -> {
                s.limit(BIG_LOOP)
                 .filter(f -> c.getName().equals(f.getClassName()) && mn.equals(f.getMethodName()))
                 .forEach(f -> {
                    assertEquals(f.getFileName(), fileName);
                    int line = f.getLineNumber();
                    assertTrue(line >= BEGIN_LINE && line <= END_LINE);

                    StackTraceElement st = f.toStackTraceElement();
                    assertEquals(c.getName(), st.getClassName());
                    assertEquals(mn, st.getMethodName());
                    assertEquals(st.getFileName(), fileName);
                    line = st.getLineNumber();
                    assertTrue(line >= BEGIN_LINE && line <= END_LINE);
                });
                return null;
            });
        }
    }
}
