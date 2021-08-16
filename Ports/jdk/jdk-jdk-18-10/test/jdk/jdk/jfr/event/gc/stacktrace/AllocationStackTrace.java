/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.gc.stacktrace;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import com.sun.management.GarbageCollectorMXBean;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

import java.util.List;
import java.util.ArrayList;

import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

abstract class MemoryAllocator {

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;

    public static Object garbage = null;

    abstract public void allocate();
    public void clear() {
        garbage = null;
    }
}

class EdenMemoryAllocator extends MemoryAllocator {

    @Override
    public void allocate() {
        garbage = new byte[10 * KB];
    }
}

class HumongousMemoryAllocator extends MemoryAllocator {

    @Override
    public void allocate() {
        garbage = new byte[5 * MB];
    }
}

/**
 * Attempts to fill up young gen and allocate in old gen
 */
class OldGenMemoryAllocator extends MemoryAllocator {

    private List<byte[]> list = new ArrayList<byte[]>();
    private int counter = 6000;

    @Override
    public void allocate() {
        if (counter-- > 0) {
            list.add(new byte[10 * KB]);
        } else {
            list = new ArrayList<byte[]>();
            counter = 6000;
        }

        garbage = list;
    }

    @Override
    public void clear(){
        list = null;
        super.clear();
    }
}

class MetaspaceMemoryAllocator extends MemoryAllocator {

    private static int counter = 0;

    /**
     * Imitates class loading. Each invocation of this method causes a new class
     * loader object is created and a new class is loaded by this class loader.
     * Method throws OOM when run out of memory.
     */
    static protected void loadNewClass() {
        try {
            String jarUrl = "file:" + (counter++) + ".jar";
            URL[] urls = new URL[]{new URL(jarUrl)};
            URLClassLoader cl = new URLClassLoader(urls);
            Proxy.newProxyInstance(
                    cl,
                    new Class[]{Foo.class},
                    new FooInvocationHandler(new FooBar()));
        } catch (java.net.MalformedURLException badThing) {
            // should never occur
            System.err.println("Unexpected error: " + badThing);
            throw new RuntimeException(badThing);
        }
    }

    @Override
    public void allocate() {
        try {
            loadNewClass();
        } catch (OutOfMemoryError e) {
            /* empty */
        }
    }

    public static interface Foo {
    }

    public static class FooBar implements Foo {
    }

    static class FooInvocationHandler implements InvocationHandler {

        private final Foo foo;

        FooInvocationHandler(Foo foo) {
            this.foo = foo;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(foo, args);
        }
    }
}

/**
 * Utility class to peform JFR recording, GC provocation/detection and
 * stacktrace verification for related JFR events
 */
public class AllocationStackTrace {

    /**
     * Tests event stacktrace for young GC if -XX:+UseSerialGC is used
     */
    public static void testDefNewAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("Copy");
        MemoryAllocator memory = new EdenMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testDefNewAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace for old GC if -XX:+UseSerialGC is used
     */
    public static void testMarkSweepCompactAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("MarkSweepCompact");
        MemoryAllocator memory = new OldGenMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testMarkSweepCompactAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace during metaspace GC threshold if -XX:+UseSerialGC
     * is used
     */
    public static void testMetaspaceSerialGCAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("MarkSweepCompact");
        MemoryAllocator memory = new MetaspaceMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testMetaspaceSerialGCAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace for young GC if -XX:+UseParallelGC is used
     */
    public static void testParallelScavengeAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("PS Scavenge");
        MemoryAllocator memory = new EdenMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testParallelScavengeAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace for old GC if -XX:+UseParallelGC is used
     */
    public static void testParallelMarkSweepAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("PS MarkSweep");
        MemoryAllocator memory = new OldGenMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testParallelMarkSweepAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace during metaspace GC threshold if
     * -XX:+UseParallelGC is used
     */
    public static void testMetaspaceParallelGCAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("PS MarkSweep");
        MemoryAllocator memory = new MetaspaceMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testMetaspaceParallelGCAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace for young GC if -XX:+UseG1GC is used
     */
    public static void testG1YoungAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("G1 Young Generation");
        MemoryAllocator memory = new EdenMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testG1YoungAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace for old GC if -XX:+UseG1GC is used
     */
    public static void testG1OldAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("G1 Old Generation");
        MemoryAllocator memory = new OldGenMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testG1OldAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace during metaspace GC threshold if -XX:+UseG1GC is
     * used
     */
    public static void testMetaspaceG1GCAllocEvent() throws Exception {
        GarbageCollectorMXBean bean = garbageCollectorMXBean("G1 Young Generation");
        MemoryAllocator memory = new MetaspaceMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testMetaspaceG1GCAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    /**
     * Tests event stacktrace for GC caused by humongous allocations if
     * -XX:+UseG1GC is used
     */
    public static void testG1HumonAllocEvent() throws Exception {
        // G1 tries to reclaim humongous objects at every young collection
        // after doing a conservative estimate of its liveness
        GarbageCollectorMXBean bean = garbageCollectorMXBean("G1 Young Generation");
        MemoryAllocator memory = new HumongousMemoryAllocator();

        String[] expectedStack = new String[]{
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testAllocEvent",
            "jdk.jfr.event.gc.stacktrace.AllocationStackTrace.testG1HumonAllocEvent"
        };

        testAllocEvent(bean, memory, expectedStack);
    }

    private static GarbageCollectorMXBean garbageCollectorMXBean(String name) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        GarbageCollectorMXBean bean = ManagementFactory.newPlatformMXBeanProxy(
                server, "java.lang:type=GarbageCollector,name=" + name, GarbageCollectorMXBean.class);
        return bean;
    }

    /**
     * Performs JFR recording, GC provocation/detection and stacktrace
     * verification for JFR event. In case of verification failure
     * repeats several times.
     *
     * @param bean MX bean for desired GC
     * @param memory allocator for desired type of allocations
     * @param expectedStack array of expected frames
     */
    private static void testAllocEvent(GarbageCollectorMXBean bean, MemoryAllocator memory, String[] expectedStack) throws Exception {
        // The test checks the stacktrace of events and expects all the events are fired
        // in the current thread. But compilation may also trigger GC.
        // So to filter out such cases the test performs several iterations and expects
        // that the memory allocations made by the test will produce the desired JFR event.
        final int iterations = 5;
        for (int i = 0; i < iterations; i++) {
            if (allocAndCheck(bean, memory, expectedStack)) {
                return;
            } else {
                System.out.println("Attempt: " + i + " out of " + iterations+": no matching stack trace found.");
            }
            memory.clear();
        }
        throw new AssertionError("No matching stack trace found");
    }

    /**
     * Performs JFR recording, GC provocation/detection and stacktrace
     * verification for JFR event.
     *
     * @param bean MX bean for desired GC
     * @param memory allocator for desired type of allocations
     * @param expectedStack array of expected frames
     * @throws Exception
     */
    private static boolean allocAndCheck(GarbageCollectorMXBean bean, MemoryAllocator memory,
            String[] expectedStack) throws Exception {
        String threadName = Thread.currentThread().getName();
        String event = EventNames.AllocationRequiringGC;

        Recording r = new Recording();
        r.enable(event).withStackTrace();
        r.start();

        long prevCollectionCount = bean.getCollectionCount();
        long collectionCount = -1;

        long iterationCount = 0;

        do {
            memory.allocate();
            collectionCount = bean.getCollectionCount();
            iterationCount++;
        } while (collectionCount == prevCollectionCount);

        System.out.println("Allocation num: " + iterationCount);
        System.out.println("GC detected: " + collectionCount);

        r.stop();
        List<RecordedEvent> events = Events.fromRecording(r);

        System.out.println("JFR GC events found: " + events.size());

        // Find any event that matched the expected stack trace
        for (RecordedEvent e : events) {
            System.out.println("Event: " + e);
            RecordedThread thread = e.getThread();
            String eventThreadName = thread.getJavaName();
            if (!threadName.equals(eventThreadName)) {
                continue;
            }
            if (matchingStackTrace(e.getStackTrace(), expectedStack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchingStackTrace(RecordedStackTrace stack, String[] expectedStack) {
        if (stack == null) {
            return false;
        }

        List<RecordedFrame> frames = stack.getFrames();
        int pos = findFramePos(frames, expectedStack[0]);

        if (pos == -1) {
            return false;
        }

        for (String expectedFrame : expectedStack) {
            RecordedFrame f = frames.get(pos++);
            String frame = frameToString(f);

            if (!frame.equals(expectedFrame)) {
                return false;
            }
        }

        return true;
    }

    private static int findFramePos(List<RecordedFrame> frames, String frame) {
        int pos = 0;

        for (RecordedFrame f : frames) {
            if (frame.equals(frameToString(f))) {
                return pos;
            }
            pos++;
        }

        return -1;
    }

    private static String frameToString(RecordedFrame f) {
        RecordedMethod m = f.getMethod();
        String methodName = m.getName();
        String className = m.getType().getName();
        return className + "." + methodName;
    }

}
