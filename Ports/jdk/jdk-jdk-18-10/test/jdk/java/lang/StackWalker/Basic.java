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

/*
 * @test
 * @bug 8140450 8173898
 * @summary Basic test for the StackWalker::walk method
 * @run testng Basic
 */

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.lang.StackWalker.Option.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Basic {
    private static boolean verbose = false;

    @DataProvider(name = "stackDepths")
    public static Object[][] stackDepths() {
        return new Object[][] {
                { new int[] { 12 },  new int[] { 4, 8, 12}      },
                { new int[] { 18 },  new int[] { 8, 16, 20}     },
                { new int[] { 32 },  new int[] { 16, 32, 64}    },
        };
    }

    /**
     * For a stack of a given depth, it creates a StackWalker with an estimate.
     * Test walking different number of frames
     */
    @Test(dataProvider = "stackDepths")
    public static void test(int[] depth, int[] estimates) {
        Basic test = new Basic(depth[0]);
        for (int estimate : estimates) {
            test.walk(estimate);
        }
    }

    @Test
    public static void testWalkFromConstructor() throws Exception {
        System.out.println("testWalkFromConstructor:");
        List<String> found = ((ConstructorNewInstance)ConstructorNewInstance.class.getMethod("create")
                             .invoke(null)).collectedFrames();
        assertEquals(List.of(ConstructorNewInstance.class.getName()+"::<init>",
                             ConstructorNewInstance.class.getName()+"::create",
                             Basic.class.getName()+"::testWalkFromConstructor"),
                     found);
    }

    @Test
    public static void testMethodSignature() throws Exception {
        List<StackFrame> frames = new StackBuilder(16, 16).build();
        Map<String, MethodType> methodTypes = StackBuilder.methodTypes();
        for (StackFrame f : frames) {
            MethodType type = methodTypes.get(f.getMethodName());
            if (type != null) {
                System.out.format("%s.%s %s%n", f.getClassName(), f.getMethodName(),
                                  f.getDescriptor());

                String descriptor = f.getDescriptor();
                if (!descriptor.equals(type.toMethodDescriptorString())) {
                    throw new RuntimeException("Expected: " + type.toMethodDescriptorString()
                        + " got: " + f.getDescriptor());
                }

                if (!f.getMethodType().equals(type)) {
                    throw new RuntimeException("Expected: " + type
                        + " got: " + f.getMethodType());
                }

                // verify descriptor returned by getDescriptor() before and after
                // getMethodType() is called
                if (!descriptor.equals(f.getDescriptor())) {
                    throw new RuntimeException("Mismatched: " + descriptor
                        + " got: " + f.getDescriptor());
                }
            }
        }
    }

    private final int depth;
    Basic(int depth) {
        this.depth = depth;
    }

    /** For TestNG */
    public Basic() {
        depth = 0;
    }

    /*
     * Setup a stack builder with the expected stack depth
     * Walk the stack and count the frames.
     */
    void walk(int estimate) {
        int limit = Math.min(depth, 16);
        List<StackFrame> frames = new StackBuilder(depth, limit).build();
        System.out.format("depth=%d estimate=%d expected=%d walked=%d%n",
                          depth, estimate, limit, frames.size());
        assertEquals(limit, frames.size());
    }

    static class ConstructorNewInstance {
        static final StackWalker walker =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        List<String> testFramesOrReflectionFrames;
        public ConstructorNewInstance() {
            testFramesOrReflectionFrames = walker.walk(this::parse);
        }
        public List<String> collectedFrames() {
            return testFramesOrReflectionFrames;
        }
        public boolean accept(StackFrame f) {
            // Frames whose class names don't contain "."
            // are our own test frames. These are the ones
            // we expect.
            // Frames whose class names contain ".reflect."
            // are reflection frames. None should be present,
            // since they are supposed to be filtered by
            // by StackWalker. If we find any, we want to fail.
            if (!f.getClassName().contains(".")
                || f.getClassName().contains(".reflect.")) {
                System.out.println("    " + f);
                return true;
            }
            // Filter out all other frames (in particular
            // those from the test framework) in order to
            // have predictable results.
            return false;
        }
        public String frame(StackFrame f) {
            return f.getClassName() + "::" + f.getMethodName();
        }
        List<String> parse(Stream<StackFrame> s) {
            return s.filter(this::accept)
                    .map(this::frame)
                    .collect(Collectors.toList());
        }
        public static ConstructorNewInstance create() throws Exception {
            return ConstructorNewInstance.class.getConstructor().newInstance();
        }
    }

    static class StackBuilder {
        private final int stackDepth;
        private final int limit;
        private int depth = 0;
        private List<StackFrame> result;
        StackBuilder(int stackDepth, int limit) {
            this.stackDepth = stackDepth; // build method;
            this.limit = limit;
        }
        List<StackFrame> build() {
            trace("build");
            m1();
            return result;
        }
        void m1() {
            trace("m1");
            m2();
        }
        List m2() {
            trace("m2");
            m3();
            return null;
        }
        int m3() {
            trace("m3");
            m4(null);
            return 0;
        }
        void m4(Object o) {
            trace("m4");
            int remaining = stackDepth-depth-1;
            if (remaining >= 4) {
                m1();
            } else {
                filler(remaining);
            }
        }
        void filler(int i) {
            trace("filler");
            if (i == 0)
                walk();
            else
                filler(--i);
        }

        void walk() {
            StackWalker walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
            result = walker.walk(s -> s.limit(limit).collect(Collectors.toList()));
        }
        void trace(String methodname) {
            ++depth;
            if (verbose)
                System.out.format("%2d: %s%n", depth, methodname);
        }

        static Map<String, MethodType> methodTypes() throws Exception {
            return Map.of("m1", MethodType.methodType(void.class),
                          "m2", MethodType.methodType(List.class),
                          "m3", MethodType.methodType(int.class),
                          "m4", MethodType.methodType(void.class, Object.class));
        }
    }

}
