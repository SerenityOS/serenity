/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @test
 * @bug 8152389
 * @summary Verify the correct behavior of LogRecord.inferCaller() in particular
 *          when a message is directly logged through the root logger.
 * @run main/othervm TestInferCaller
 * @author danielfuchs
 */
public class TestInferCaller {

    static final class LogEvent {
        public final String className;
        public final String methodName;
        public final LogRecord record;

        public LogEvent(String className, String methodName, LogRecord record) {
            this.className = className;
            this.methodName = methodName;
            this.record = record;
        }

    }

    static final class TestHandler extends Handler {

        public static final Queue<LogEvent> PUBLISHED = new LinkedList<LogEvent>();

        public TestHandler() {
            initLevel(Level.ALL);
        }

        @Override
        public void close() throws SecurityException { }
        @Override
        public void publish(LogRecord record) {
            LogEvent event = new LogEvent(record.getSourceClassName(),
                                          record.getSourceMethodName(),
                                          record);
            PUBLISHED.add(event);
        }
        @Override
        public void flush() {}

        private void initLevel(Level newLevel) {
            super.setLevel(newLevel);
        }

    }

    public void test1(Logger logger) {
        System.out.println("test1: " + loggerName(logger));

        AtomicInteger count = new AtomicInteger();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        logger.setLevel(Level.ALL);
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");

        logger.severe("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        LogEvent event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());

        logger.warning("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());

        logger.info("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());

        logger.config("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());

        logger.fine("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());

        logger.finer("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());

        logger.finest("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test1", "message " + count.get());
    }

    void test2(Logger logger) {
        AtomicInteger count = new AtomicInteger();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        logger.setLevel(Level.ALL);
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");

        for (Level l : Arrays.asList(Level.SEVERE, Level.WARNING, Level.INFO,
                Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST)) {
            System.out.println("test2: " + loggerName(logger) + " " + l);
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");

            logger.log(l, "message " + count.incrementAndGet());
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            LogEvent event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            logger.log(l, "message " + count.incrementAndGet(), "param");
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            logger.log(l, "message " + count.incrementAndGet(), new Object[] {"foo", "bar"});
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            logger.log(l, "message " + count.incrementAndGet(), new RuntimeException());
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            // JDK 8 & 9 only (uses lambda)
            logger.log(l, () -> "message " + count.incrementAndGet());
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            // JDK 8 & 9 only (uses lambda)
            logger.log(l, new RuntimeException(), () -> "message " + count.incrementAndGet());
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            // JDK 9 only: new API
            logger.logrb(l, (ResourceBundle)null, "message " + count.incrementAndGet(), (Object[]) null);
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

            // JDK 9 only: new API
            logger.logrb(l, (ResourceBundle)null, "message " + count.incrementAndGet(), new RuntimeException());
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), "test2", "message " + count.get());

        }
    }

    void test3(Logger logger) {
        System.out.println("test3: " + loggerName(logger));
        AtomicInteger count = new AtomicInteger();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        logger.setLevel(Level.ALL);
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");

        testReflection(logger, count, "severe", "testReflection");
        testReflection(logger, count, "warning", "testReflection");
        testReflection(logger, count, "info", "testReflection");
        testReflection(logger, count, "config", "testReflection");
        testReflection(logger, count, "fine", "testReflection");
        testReflection(logger, count, "finer", "testReflection");
        testReflection(logger, count, "finest", "testReflection");
    }

    void testReflection(Logger logger, AtomicInteger count, String logm, String method) {
        try {
            Method m = Logger.class.getMethod(logm, String.class);
            m.invoke(logger, "message " + count.incrementAndGet());
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            LogEvent event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), method, "message " + count.get());

            Method m2 = Method.class.getMethod("invoke", Object.class, new Object[0].getClass());
            m2.invoke(m, logger, new Object[] { "message " + count.incrementAndGet() });
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), method, "message " + count.get());

            m2.invoke(m2, m, new Object[] {logger, new Object[] { "message " + count.incrementAndGet() }});
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), method, "message " + count.get());

            m2.invoke(m2, m2, new Object[] { m, new Object[] {logger, new Object[] { "message " + count.incrementAndGet() }}});
            assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
            event = TestHandler.PUBLISHED.remove();
            assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
            checkEvent(event, this.getClass().getName(), method, "message " + count.get());

        } catch (Error | RuntimeException x ) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    // JDK 8 & 9 only (uses lambda)
    public void test4(Logger logger) {
        System.out.println("test4: " + loggerName(logger));
        AtomicInteger count = new AtomicInteger();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        logger.setLevel(Level.ALL);
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");

        logger.severe(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        LogEvent event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());

        logger.warning(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());

        logger.info(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());

        logger.config(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());

        logger.fine(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());

        logger.finer(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());

        logger.finest(() -> "message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), "test4", "message " + count.get());
    }

    // JDK 8 & 9 only  (uses lambda)
    void test5(Logger logger) {
        System.out.println("test5: " + loggerName(logger));
        AtomicInteger count = new AtomicInteger();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        logger.setLevel(Level.ALL);
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");

        testLambda(count, logger::severe, "testLambda");
        testLambda(count, logger::warning, "testLambda");
        testLambda(count, logger::info, "testLambda");
        testLambda(count, logger::config, "testLambda");
        testLambda(count, logger::fine, "testLambda");
        testLambda(count, logger::finer, "testLambda");
        testLambda(count, logger::finest, "testLambda");
    }

    // JDK 8 & 9 only (uses lambda)
    void testLambda(AtomicInteger count, Consumer<String> logm, String method) {
        logm.accept("message " + count.incrementAndGet());
        assertEquals(1, TestHandler.PUBLISHED.size(), "No event in queue: ");
        LogEvent event = TestHandler.PUBLISHED.remove();
        assertEquals(0, TestHandler.PUBLISHED.size(), "Queue should be empty: ");
        checkEvent(event, this.getClass().getName(), method, "message " + count.get());
    }

    private static String loggerName(Logger logger) {
        String name = logger.getName();
        if (name == null) return "<anonymous>";
        if (name.isEmpty()) return "<RootLogger>";
        return "\"" + name + "\"";
    }

    public void test(Logger logger) {
        test1(logger);
        test2(logger);
        test3(logger);

        // JDK 8 & 9 only (uses lambda)
        test4(logger);
        test5(logger);
    }

    public static void main(String[] args) {
        TestInferCaller test = new TestInferCaller();
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            h.setLevel(Level.OFF);
        }
        root.addHandler(new TestHandler());

        for (Logger logger : Arrays.asList(root, Logger.getGlobal(),
                Logger.getAnonymousLogger(), Logger.getLogger("foo.bar"))) {
            System.out.println("Testing with: " + loggerName(logger) + " " + logger.getClass());
            test.test(logger);
        }
    }

    private static void assertEquals(int expected, int actual, String what) {
        if (expected != actual) {
            throw new RuntimeException(what
                    + "\n\texpected: " + expected
                    + "\n\tactual:   " + actual);
        }
    }

    private static void assertEquals(String expected, String actual, String what) {
        if (!Objects.equals(expected, actual)) {
            throw new RuntimeException(what
                    + "\n\texpected: " + expected
                    + "\n\tactual:   " + actual);
        }
    }

    private void checkEvent(LogEvent event, String className, String methodName, String message) {
        assertEquals(className, event.className, "Bad class name: ");
        assertEquals(className, event.record.getSourceClassName(), "Bad source class name: ");
        assertEquals(methodName, event.methodName, "Bad method name: ");
        assertEquals(methodName, event.record.getSourceMethodName(), "Bad source method name: ");
        assertEquals(message, event.record.getMessage(), "Bad message: ");
    }


}
