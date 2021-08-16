/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import jdk.test.lib.RandomFactory;

/**
 * @test
 * @bug 8140450
 * @summary Stack Walk Test (use -Dseed=X to set PRNG seed)
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @compile StackRecorderUtil.java
 * @run main/othervm StackWalkTest
 * @run main/othervm/java.security.policy=stackwalktest.policy StackWalkTest
 * @run main/othervm StackWalkTest -random:50
 * @run main/othervm/java.security.policy=stackwalktest.policy StackWalkTest -random:50
 * @author danielfuchs, bchristi
 * @key randomness
 */
public class StackWalkTest {
    private static boolean random = false;
    private static boolean verbose = false;
    private static int randomRuns = 50;

    private static final int MAX_RANDOM_DEPTH = 1000;

    static final Set<String> infrastructureClasses = new TreeSet<>(Arrays.asList(
            "jdk.internal.reflect.NativeMethodAccessorImpl",
            "jdk.internal.reflect.DelegatingMethodAccessorImpl",
            "java.lang.reflect.Method",
            "com.sun.javatest.regtest.MainWrapper$MainThread",
            "com.sun.javatest.regtest.agent.MainWrapper$MainThread",
            "java.lang.Thread"
    ));
    static final List<Class<?>> streamPipelines = Arrays.asList(
        classForName("java.util.stream.AbstractPipeline"),
        classForName("java.util.stream.TerminalOp")
    );
    static Class<?> classForName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e){
            throw new RuntimeException(e);
        }
    }

    private static boolean isStreamPipeline(Class<?> clazz) {
        for (Class<?> c : streamPipelines) {
            if (c.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    StackRecorderUtil recorder;
    int count = 0;
    boolean didWalk = false;

    final int estDepth;
    final Set<StackWalker.Option> swOptions;

    public StackWalkTest() {
        this(EnumSet.noneOf(StackWalker.Option.class), -1);
    }

    public StackWalkTest(Set<StackWalker.Option> swOptions) {
        this(swOptions, -1);
    }

    public StackWalkTest(int estimatedDepth) {
        this(EnumSet.noneOf(StackWalker.Option.class), -1);
    }

    public StackWalkTest(Set<StackWalker.Option> swOptions, int estimatedDepth) {
        this.swOptions = swOptions;
        this.estDepth = estimatedDepth;
    }

    private StackWalker createStackWalker() {
        // test all StackWalker factory methods
        if (this.estDepth < 0) {
            if (swOptions.isEmpty()) {
                return StackWalker.getInstance();
            } else {
                return StackWalker.getInstance(swOptions);
            }
        }
        return StackWalker.getInstance(swOptions, estDepth);
    }
    public void consume(StackFrame sf) {
        if (count == 0 && swOptions.contains(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                && isStreamPipeline(sf.getDeclaringClass())) {
            return;
        }
        if (verbose) {
            System.out.println("\t" + sf.getClassName() + "." + sf.getMethodName());
        }
        if (count >= recorder.frameCount()) {
            // We've gone past main()...
            if (infrastructureClasses.contains(sf.getClassName())) {
                // safe to ignore
                return;
            }
        }
        try {
            recorder.compareFrame(count, sf);
        } catch (IndexOutOfBoundsException e) {
            // Extra non-infra frame in stream
            throw new RuntimeException("extra non-infra stack frame at count "
                    + count + ": <" + sf + ">", e);
        }
        count++;
    }

    public class Call {
        public void walk(int total, int markAt) {
            recorder.add(Call.class, "walk", "StackWalkTest.java");
            long swFrameCount = createStackWalker().walk(s -> s.count());

            if (verbose) {
                System.out.println("Call.walk() total=" + total + ", markAt=" + markAt);
                System.out.println("recorder frames:");
                for (StackRecorderUtil.TestFrame f : recorder) {
                    System.out.println("\t" + f.declaringClass + "." + f.methodName);
                }
                System.out.println("\nStackWalker recorded " + swFrameCount + " frames");
                System.out.flush();
            }
            long recFrameCount = (long)recorder.frameCount();
            if (swFrameCount < recFrameCount) {
                throw new RuntimeException("StackWalker recorded fewer frames ("+
                        swFrameCount + ") than recorded ("+ recorder.frameCount() +
                        ") - " + "estimatedDepth set to " + estDepth);
            }
            if (verbose) {
                System.out.println("StackWalker frames:");
            }
            createStackWalker().forEach(StackWalkTest.this::consume);
            didWalk = true;
        }
        public void call(int total, int current, int markAt) {
            recorder.add(Call.class, "call", "StackWalkTest.java");
            if (current < total) {
                testCall.call(total, current+1, markAt);
            } else {
                walk(total, markAt);
            }
        }
    }

    public class Marker extends Call {
        @Override
        public void call(int total, int current, int markAt) {
            recorder.add(Marker.class, "call", "StackWalkTest.java");
            if (current < total) {
                testCall.call(total, current+1, markAt);
            } else {
                walk(total, markAt);
            }
        }
    }
    private Call markerCall = new Marker();

    public class Test extends Call {
        @Override
        public void call(int total, int current, int markAt) {
            recorder.add(Test.class, "call", "StackWalkTest.java");
            if (current < total) {
                int nexti = current + 1;
                if (nexti==markAt) {
                    markerCall.call(total, nexti, markAt);
                } else {
                    testCall.call2(total, nexti, markAt);
                }
            } else {
                walk(total, markAt);
            }
        }
        public void call2(int total, int current, int markAt) {
            recorder.add(Test.class, "call2", "StackWalkTest.java");
            if (current < total) {
                int nexti = current + 1;
                if (nexti==markAt) {
                    markerCall.call(total, nexti, markAt);
                } else {
                    test2Call.call(total, nexti, markAt);
                }
            } else {
                walk(total, markAt);
            }
        }
    }
    private Test testCall = new Test();

    /** Inherits call() from Call */
    public class Test2 extends Call {}
    private Test2 test2Call = new Test2();

    public void runTest(Class callerClass, String callerMethod, int stackDepth,
                        int markAt) {
        if (didWalk) {
            throw new IllegalStateException("StackWalkTest already used");
        }
        // Test may run into StackOverflow when running in -Xcomp mode on deep stack
        assert stackDepth <= 1000;
        assert markAt <= stackDepth : "markAt(" + markAt + ") > stackDepth("
                + stackDepth + ")";
        System.out.print("runTest(" + swOptions
                + "), estimatedDepth=" + estDepth);

        recorder = new StackRecorderUtil(swOptions);
        recorder.add(callerClass, callerMethod, "StackWalkTest.java");
        recorder.add(StackWalkTest.class, "runTest", "StackWalkTest.java");

        Test test1 = new Test();
        test1.call(stackDepth, 0, markAt);

        System.out.println(" finished");
        if (!didWalk) {
            throw new IllegalStateException("Test wasn't actually performed");
        }
    }

    public static void main(String[] args) {
        String rand = "-random";
        String randItems = "-random:";
        for(String arg : args) {
            if (arg.startsWith(rand)) {
                random = true;
                try {
                    if(arg.startsWith(randItems)) {
                        randomRuns = Integer.valueOf(arg.substring(randItems.length()));
                    }
                } catch(NumberFormatException e) {}
            } else if("-verbose".equals(arg)) {
                verbose = true;
            }
        }
        if (random) {
            Random rng = RandomFactory.getRandom();
            for (int iters = 0; iters < randomRuns; iters++) {
                Set<StackWalker.Option> opts = new HashSet<>();
                if (rng.nextBoolean()) {
                    opts.add(RETAIN_CLASS_REFERENCE);
                }

                int depth = 1 + rng.nextInt(MAX_RANDOM_DEPTH);

                StackWalkTest swt;
                if (rng.nextBoolean() && depth > 1) {
                    // Test that specifying an estimatedDepth doesn't prevent
                    // full stack traversal
                    swt = new StackWalkTest(opts, 1+rng.nextInt(depth-1));
                } else {
                    swt = new StackWalkTest(opts);
                }

                int markAt = rng.nextInt(depth+1);
                System.out.print(depth + "@" + markAt + " ");
                System.out.flush();
                swt.runTest(StackWalkTest.class, "main", depth, markAt);
            }
        } else {
            // Long stack, default maxDepth
            StackWalkTest swt;
            swt = new StackWalkTest();
            swt.runTest(StackWalkTest.class, "main", 1000, 10);

            // Long stack, matching maxDepth
            swt = new StackWalkTest(2000);
            swt.runTest(StackWalkTest.class, "main", 1000, 10);

            // Long stack, maximum maxDepth
            swt = new StackWalkTest(Integer.MAX_VALUE);
            swt.runTest(StackWalkTest.class, "main", 1000, 10);

            //
            // Single batch
            //
            swt = new StackWalkTest(); // default maxDepth
            swt.runTest(StackWalkTest.class, "main", 6, 3);

            swt = new StackWalkTest(4); // maxDepth < stack
            swt.runTest(StackWalkTest.class, "main", 6, 3);

            swt = new StackWalkTest(2); // maxDepth < marker
            swt.runTest(StackWalkTest.class, "main", 6, 4);

            //
            // 2 batches
            //
            swt = new StackWalkTest(); // default maxDepth
            swt.runTest(StackWalkTest.class, "main", 24, 10);
            swt = new StackWalkTest(18); // maxDepth < stack
            swt.runTest(StackWalkTest.class, "main", 24, 10);
            swt = new StackWalkTest(8); // maxDepth < marker
            swt.runTest(StackWalkTest.class, "main", 24, 10);

            //
            // 3 batch
            //
            swt = new StackWalkTest(); // default maxDepth
            swt.runTest(StackWalkTest.class, "main", 60, 20);
            swt = new StackWalkTest(35); // maxDepth < stack
            swt.runTest(StackWalkTest.class, "main", 60, 20);
            swt = new StackWalkTest(8); // maxDepth < marker
            swt.runTest(StackWalkTest.class, "main", 60, 20);

            //
            // StackWalker.Options
            //
            swt = new StackWalkTest();
            swt.runTest(StackWalkTest.class, "main", 50, 10);

            swt = new StackWalkTest(EnumSet.of(RETAIN_CLASS_REFERENCE));
            swt.runTest(StackWalkTest.class, "main", 80, 40);

            swt = new StackWalkTest(EnumSet.of(RETAIN_CLASS_REFERENCE), 50);
            swt.runTest(StackWalkTest.class, "main", 1000, 524);
        }
    }
}
