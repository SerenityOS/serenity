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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.StackWalker.StackFrame;
import static java.lang.StackWalker.Option.*;


/**
 * @test
 * @bug 8140450
 * @summary This test will walk the stack using different methods, called
 *          from several threads running concurrently.
 *          Except in the case of MTSTACKSTREAM - which takes a snapshot
 *          of the stack before walking, all the methods only allow to
 *          walk the current thread stack.
 * @run main/othervm MultiThreadStackWalk
 * @author danielfuchs
 */
public class MultiThreadStackWalk {

    static Set<String> infrastructureClasses = new TreeSet<>(Arrays.asList(
            "jdk.internal.reflect.NativeMethodAccessorImpl",
            "jdk.internal.reflect.DelegatingMethodAccessorImpl",
            "java.lang.reflect.Method",
            "com.sun.javatest.regtest.MainWrapper$MainThread",
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

    /**
     * An object that contains variables pertaining to the execution
     * of the test within one thread.
     * A small amount of those variable are shared with sub threads when
     * the stack walk is executed in parallel - that is when spliterators
     * obtained from trySplit are handed over to an instance of SplitThread
     * in order to parallelize thread walking.
     * @see WalkThread#handOff(MultiThreadStackWalk.Env, java.util.Spliterator, boolean, boolean)
     * @see Env#split(MultiThreadStackWalk.Env)
     */
    public static class Env {
        final AtomicLong frameCounter;  // private: the counter for the current thread.
        final long checkMarkAt;         // constant: the point at which we expect to
                                        // find the marker in consume()
        final long max;                 // constant: the maximum number of recursive
                                        // calls to Call.
        final AtomicBoolean debug ;     // shared: whether debug is active for the
                                        // instance of Test from which this instance
                                        // of Env was spawned
        final AtomicLong markerCalled;  // shared: whether the marker was reached
        final AtomicLong maxReached;    // shared: whether max was reached
        final Set<String> unexpected;   // shared: list of unexpected infrastructure
                                        // classes encountered after max is reached

        public Env(long total, long markAt, AtomicBoolean debug) {
            this.debug = debug;
            frameCounter = new AtomicLong();
            maxReached = new AtomicLong();
            unexpected = Collections.synchronizedSet(new TreeSet<>());
            this.max = total+2;
            this.checkMarkAt = total - markAt + 1;
            this.markerCalled = new AtomicLong();
        }

        // Used when delegating part of the stack walking to a sub thread
        // see WalkThread.handOff.
        private Env(Env orig, long start) {
            debug = orig.debug;
            frameCounter = new AtomicLong(start);
            maxReached = orig.maxReached;
            unexpected = orig.unexpected;
            max = orig.max;
            checkMarkAt = orig.checkMarkAt;
            markerCalled = orig.markerCalled;
        }

        // The stack walk consumer method, where all the checks are
        // performed.
        public void consume(StackFrame sfi) {
            if (frameCounter.get() == 0 && isStreamPipeline(sfi.getDeclaringClass())) {
                return;
            }

            final long count = frameCounter.getAndIncrement();
            final StringBuilder builder = new StringBuilder();
            builder.append("Declaring class[")
                   .append(count)
                   .append("]: ")
                   .append(sfi.getDeclaringClass());
            builder.append('\n');
            builder.append("\t")
                   .append(sfi.getClassName())
                   .append(".")
                   .append(sfi.toStackTraceElement().getMethodName())
                   .append(sfi.toStackTraceElement().isNativeMethod()
                           ? "(native)"
                           : "(" + sfi.toStackTraceElement().getFileName()
                             +":"+sfi.toStackTraceElement().getLineNumber()+")");
            builder.append('\n');
            if (debug.get()) {
                System.out.print("[debug] " + builder.toString());
                builder.setLength(0);
            }
            if (count == max) {
                maxReached.incrementAndGet();
            }
            if (count  == checkMarkAt) {
                if (sfi.getDeclaringClass() != MultiThreadStackWalk.Marker.class) {
                    throw new RuntimeException("Expected Marker at " + count
                            + ", found " + sfi.getDeclaringClass());
                }
            } else {
                if (count <= 0 && sfi.getDeclaringClass() != MultiThreadStackWalk.Call.class) {
                    throw new RuntimeException("Expected Call at " + count
                            + ", found " + sfi.getDeclaringClass());
                } else if (count > 0 && count < max && sfi.getDeclaringClass() != MultiThreadStackWalk.Test.class) {
                    throw new RuntimeException("Expected Test at " + count
                            + ", found " + sfi.getDeclaringClass());
                } else if (count == max && sfi.getDeclaringClass() != MultiThreadStackWalk.class) {
                    throw new RuntimeException("Expected MultiThreadStackWalk at "
                            + count + ", found " + sfi.getDeclaringClass());
                } else if (count == max &&  !sfi.toStackTraceElement().getMethodName().equals("runTest")) {
                    throw new RuntimeException("Expected runTest method at "
                            + count + ", found " + sfi.toStackTraceElement().getMethodName());
                } else if (count == max+1) {
                    if (sfi.getDeclaringClass() != MultiThreadStackWalk.WalkThread.class) {
                        throw new RuntimeException("Expected MultiThreadStackWalk at "
                            + count + ", found " + sfi.getDeclaringClass());
                    }
                    if (count == max && !sfi.toStackTraceElement().getMethodName().equals("run")) {
                        throw new RuntimeException("Expected main method at "
                            + count + ", found " + sfi.toStackTraceElement().getMethodName());
                    }
                } else if (count > max+1) {
                    // expect JTreg infrastructure...
                    if (!infrastructureClasses.contains(sfi.getDeclaringClass().getName())) {
                        System.err.println("**** WARNING: encountered unexpected infrastructure class at "
                                + count +": " + sfi.getDeclaringClass().getName());
                        unexpected.add(sfi.getDeclaringClass().getName());
                    }
                }
            }
            if (count == 100) {
                // Maybe we should had some kind of checking inside that lambda
                // too. For the moment we should be satisfied if it doesn't throw
                // any exception and doesn't make the outer walk fail...
                StackWalker.getInstance(RETAIN_CLASS_REFERENCE).forEach(x -> {
                    StackTraceElement st = x.toStackTraceElement();
                    StringBuilder b = new StringBuilder();
                    b.append("*** inner walk: ")
                            .append(x.getClassName())
                            .append(st == null ? "- no stack trace element -" :
                                    ("." + st.getMethodName()
                                            + (st.isNativeMethod() ? "(native)" :
                                            "(" + st.getFileName()
                                                    + ":" + st.getLineNumber() + ")")))
                            .append('\n');
                    if (debug.get()) {
                        System.out.print(b.toString());
                        b.setLength(0);
                    }
                });
            }
        }
    }

    public interface Call {
        enum WalkType {
            WALKSTACK,         // use Thread.walkStack
        }
        default WalkType getWalkType() { return WalkType.WALKSTACK;}
        default void walk(Env env) {
            WalkType walktype = getWalkType();
            System.out.println("Thread "+ Thread.currentThread().getName()
                    +" starting walk with " + walktype);
            switch(walktype) {
                case WALKSTACK:
                    StackWalker.getInstance(RETAIN_CLASS_REFERENCE)
                               .forEach(env::consume);
                    break;
                default:
                    throw new InternalError("Unknown walk type: " + walktype);
            }
        }
        default void call(Env env, Call next, int total, int current, int markAt) {
            if (current < total) {
                next.call(env, next, total, current+1, markAt);
            }
        }
    }

    public static class Marker implements Call {
        final WalkType walkType;
        Marker(WalkType walkType) {
            this.walkType = walkType;
        }
        @Override
        public WalkType getWalkType() {
            return walkType;
        }

        @Override
        public void call(Env env, Call next, int total, int current, int markAt) {
            env.markerCalled.incrementAndGet();
            if (current < total) {
                next.call(env, next, total, current+1, markAt);
            } else {
                next.walk(env);
            }
        }
    }

    public static class Test implements Call {
        final Marker marker;
        final WalkType walkType;
        final AtomicBoolean debug;
        Test(WalkType walkType) {
            this.walkType = walkType;
            this.marker = new Marker(walkType);
            this.debug = new AtomicBoolean();
        }
        @Override
        public WalkType getWalkType() {
            return walkType;
        }
        @Override
        public void call(Env env, Call next, int total, int current, int markAt) {
            if (current < total) {
                int nexti = current + 1;
                Call nextObj = nexti==markAt ? marker : next;
                nextObj.call(env, next, total, nexti, markAt);
            } else {
                walk(env);
            }
        }
    }

    public static Env runTest(Test test, int total, int markAt) {
        Env env = new Env(total, markAt, test.debug);
        test.call(env, test, total, 0, markAt);
        return env;
    }

    public static void checkTest(Env env, Test test) {
        String threadName = Thread.currentThread().getName();
        System.out.println(threadName + ": Marker called: " + env.markerCalled.get());
        System.out.println(threadName + ": Max reached: " + env.maxReached.get());
        System.out.println(threadName + ": Frames consumed: " + env.frameCounter.get());
        if (env.markerCalled.get() == 0) {
            throw new RuntimeException(Thread.currentThread().getName() + ": Marker was not called.");
        }
        if (env.markerCalled.get() > 1) {
            throw new RuntimeException(Thread.currentThread().getName()
                    + ": Marker was called more than once: " + env.maxReached.get());
        }
        if (!env.unexpected.isEmpty()) {
            System.out.flush();
            System.err.println("Encountered some unexpected infrastructure classes below 'main': "
                    + env.unexpected);
        }
        if (env.maxReached.get() == 0) {
            throw new RuntimeException(Thread.currentThread().getName()
                    + ": max not reached");
        }
        if (env.maxReached.get() > 1) {
            throw new RuntimeException(Thread.currentThread().getName()
                    + ": max was reached more than once: " + env.maxReached.get());
        }
    }

    static class WalkThread extends Thread {
        final static AtomicLong walkersCount = new AtomicLong();
        Throwable failed = null;
        final Test test;
        public WalkThread(Test test) {
            super("WalkThread[" + walkersCount.incrementAndGet() + ", type="
                    + test.getWalkType() + "]");
            this.test = test;
        }

        public void run() {
            try {
                Env env = runTest(test, 1000, 10);
                //waitWalkers(env);
                checkTest(env, test);
            } catch(Throwable t) {
                failed = t;
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        WalkThread[] threads = new WalkThread[Call.WalkType.values().length*3];
        Throwable failed = null;
        for (int i=0; i<threads.length; i++) {
            Test test = new Test(Call.WalkType.values()[i%Call.WalkType.values().length]);
            threads[i] = new WalkThread(test);
        }
        for (int i=0; i<threads.length; i++) {
            threads[i].start();
        }
        for (int i=0; i<threads.length; i++) {
            threads[i].join();
            if (failed == null) failed = threads[i].failed;
            else if (threads[i].failed == null) {
                failed.addSuppressed(threads[i].failed);
            }
        }
        if (failed != null) {
            throw failed;
        }
    }

}
