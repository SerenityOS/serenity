/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.StackWalker.Option.*;
import java.lang.StackWalker.StackFrame;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @test
 * @bug 8140450
 * @summary Stack Stream Test
 * @modules java.logging
 * @run main/othervm StackStreamTest
 */
public class StackStreamTest {
    public static void main(String[] argv) throws Exception {
        new StackStreamTest().test();
    }

    private static Logger logger = Logger.getLogger("stackstream");
    public StackStreamTest() {
    }

    public void test() {
        A.a();
    }
    static class A {
        public static void a() {
            B.b();
        }
    }
    static class B {
        public static void b() {
            C.c();
        }
    }
    static class C {
        public static void c() {
            D.d();
        }
    }
    static class D {
        public static void d() {
            E.e();
        }
    }
    static class E {
        public static void e() {
            F.f();
        }
    }
    static class F {
        public static void f() {
            logger.severe("log message");
            G.g();
            new K().k();
        }
    }

    private static boolean isTestClass(StackFrame f) {
        // Filter jtreg frames from the end of the stack
        return f.getClassName().startsWith("StackStreamTest");
    }

    static class G {
        static StackWalker STE_WALKER = StackWalker.getInstance();
        static StackWalker DEFAULT_WALKER = StackWalker.getInstance();

        private static final List<String> GOLDEN_CLASS_NAMES =
                Arrays.asList("StackStreamTest$G",
                              "StackStreamTest$F",
                              "StackStreamTest$E",
                              "StackStreamTest$D",
                              "StackStreamTest$C",
                              "StackStreamTest$B",
                              "StackStreamTest$A",
                              "StackStreamTest",
                              "StackStreamTest");
        private static final List<String> GOLDEN_METHOD_NAMES =
            Arrays.asList("g", "f", "e", "d", "c", "b", "a", "test", "main");


        public static void g() {

            System.out.println("Thread dump");
            Thread.dumpStack();

            caller();
            firstFrame();

            // Check class names
            System.out.println("check class names");
            List<String> sfs = DEFAULT_WALKER.walk(s -> {
                return s.filter(StackStreamTest::isTestClass)
                        .map(StackFrame::getClassName)
                        .collect(Collectors.toList());
            });
            equalsOrThrow("class names", sfs, GOLDEN_CLASS_NAMES);

            // Check method names
            System.out.println("methodNames()");
            sfs = DEFAULT_WALKER.walk(s -> {
                return s.filter(StackStreamTest::isTestClass)
                        .map(StackFrame::getMethodName)
                        .collect(Collectors.toList());}
            );
            equalsOrThrow("method names", sfs, GOLDEN_METHOD_NAMES);

            Exception exc = new Exception("G.g stack");
            exc.printStackTrace();

            System.out.println("Stream of StackTraceElement");
            StackWalker.getInstance()
                .walk(s ->
                {
                    s.map(StackFrame::toStackTraceElement)
                            .forEach(ste -> System.out.println("STE: " + ste));
                    return null;
                });

            // Do we need this?
            System.out.println("Collect StackTraceElement");
            List<StackTraceElement> stacktrace = STE_WALKER.walk(s ->
            {
                // Filter out jtreg frames
                return s.filter(StackStreamTest::isTestClass)
                        .collect(Collectors.mapping(StackFrame::toStackTraceElement, Collectors.toList()));
            });
            int i=0;
            for (StackTraceElement s : stacktrace) {
                System.out.format("  %d: %s%n", i++, s);
            }

            // Check STEs for correctness
            checkStackTraceElements(GOLDEN_CLASS_NAMES, GOLDEN_METHOD_NAMES, stacktrace);
        }

        static void checkStackTraceElements(List<String> classNames,
                                            List<String> methodNames,
                                            List<StackTraceElement> stes) {
            if (classNames.size() != methodNames.size() ) {
                throw new RuntimeException("Test error: classNames and methodNames should be same size");
            }
            if (classNames.size() != stes.size()) {
                dumpSTEInfo(classNames, methodNames, stes);
                throw new RuntimeException("wrong number of elements in stes");
            }
            for (int i = 0; i < classNames.size() ; i++) {
                if (!classNames.get(i).equals(stes.get(i).getClassName()) ||
                    !methodNames.get(i).equals(stes.get(i).getMethodName())) {
                    dumpSTEInfo(classNames, methodNames, stes);
                    throw new RuntimeException("class & method names don't match");
                }
            }
        }

        static void dumpSTEInfo(List<String> classNames, List<String> methodNames,
                                List<StackTraceElement> stes) {
            System.out.println("Observed class, method names:");
            for (StackTraceElement ste : stes) {
                System.out.println("  " + ste.getClassName() + ", " + ste.getMethodName());
            }
            System.out.println("Expected class, method names:");
            for (int i = 0; i < classNames.size(); i++) {
                System.out.println("  " + classNames.get(i) + ", " + methodNames.get(i));
            }
        }

        static void firstFrame() {
            System.out.println("first frame()");
            StackWalker sw = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
            sw.forEach(e -> {
                System.out.println(e.getClassName() + "," + e.getMethodName());
            });
            System.out.println("\n");
            Optional<StackFrame> frame = sw.walk(s ->
            {
                 return s.filter(e -> {
                            System.err.println(e.getClassName() + " == " +
                                               e.getClassName().equals("StackStreamTest"));
                            return e.getClassName().equals("StackStreamTest");
                        }).findFirst();
            });
            Class<?> c = frame.get().getDeclaringClass();
            System.out.println("\nfirst frame: " + c);
            if (c != StackStreamTest.class) {
                throw new RuntimeException("Unexpected first caller class " + c);
            }
        }
    }

    private static <T> void equalsOrThrow(String label, List<T> list, List<T> expected) {
        System.out.println("List:    " + list);
        System.out.println("Expectd: " + list);
        if (!list.equals(expected)) {
            System.err.println("Observed " + label);
            for (T s1 : list) {
                System.out.println("  " + s1);
            }
            System.err.println("Expected " + label);
            for (T s2 : expected) {
                System.out.println("  " + s2);
            }
            throw new RuntimeException("Error with " + label);
        }
    }


    static class K {
        void k() {
            k1();
        }
        void k1() {
            k2();
        }
        void k2() {
            k3();
        }
        void k3() {
            k4();
        }
        void k4() {
            k5();
        }
        void k5() {
            k6();
        }
        void k6() {
            k7();
        }
        void k7() {
            k8();
        }
        void k8() {
            k9();
        }
        void k9() {
            k10();
        }
        void k10() {
            k20();
        }
        void k20() {
            new Caller().test();
        }

        class Caller {
            void test() {
                Class<?> c = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
                System.out.println("\nTesting K class : " + c);
                Thread.dumpStack();
                if (c != K.class) {
                    throw new RuntimeException("Unexpected caller class "+ c);
                }
            }
        }
    }

    static void caller() {
        Class<?> c = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        System.out.println("\ncaller class : " + c);
        if (c != G.class) {
            throw new RuntimeException("Unexpected caller class "+ c);
        }
    }

}
