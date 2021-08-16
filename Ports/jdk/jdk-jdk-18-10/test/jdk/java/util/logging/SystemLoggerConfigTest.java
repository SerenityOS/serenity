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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import sun.util.logging.PlatformLogger;


/**
 * @test
 * @bug     8159245
 * @summary Tests configuration of loggers.
 * @modules java.logging/sun.util.logging.internal java.base/sun.util.logging
 * @run  main/othervm SystemLoggerConfigTest NOSECURITY
 * @run  main/othervm -Djava.security.manager=allow SystemLoggerConfigTest WITHSECURITY
 *
 * @author danielfuchs
 */
public class SystemLoggerConfigTest {

    static Logger createSystemLogger(String name) {
        return sun.util.logging.internal.LoggingProviderImpl.getLogManagerAccess()
                .demandLoggerFor(LogManager.getLogManager(), name,
                                 Thread.class.getModule());
    }

    static PlatformLogger createPlatformLogger(String name) {
        return PlatformLogger.getLogger(name);
    }

    private static void assertFalse(boolean value, String msg) {
        assertEquals(false, value, msg);
    }
    private static void assertEquals(boolean expected, boolean actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg+": expected: " + expected + " actual: " + actual);
        }
    }
    private static void assertEquals(int expected, int actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg+": expected: " + expected + " actual: " + actual);
        }
    }
    private static void assertEquals(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new AssertionError(msg+": expected: " + expected + " actual: " + actual);
        }
    }
    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(msg+": expected: " + expected + " actual: " + actual);
        }
    }

    static class TestHandler extends Handler {
        private final List<LogRecord> records = new CopyOnWriteArrayList<>();
        public TestHandler() {
            super();
            setLevel(Level.ALL);
        }

        @Override
        public void publish(LogRecord lr) {
            records.add(lr);
        }

        public List<LogRecord> drain() {
            List<LogRecord> list = new ArrayList<>(records);
            records.clear();
            return list;
        }

        public void close() {
            records.clear();
        }

        public void flush() {
        }

    }

    public static class TestHandler1 extends TestHandler {
        final static AtomicLong COUNT = new AtomicLong();
        public TestHandler1() {
            COUNT.incrementAndGet();
        }
    }

    public static class TestHandler2 extends TestHandler {
        final static AtomicLong COUNT = new AtomicLong();
        public TestHandler2() {
            COUNT.incrementAndGet();
        }
    }

    static enum TestCase { WITHSECURITY, NOSECURITY }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = Stream.of(TestCase.values())
                    .map(String::valueOf)
                    .collect(Collectors.toList())
                    .toArray(new String[0]);
        }
        Stream.of(args)
              .map(TestCase::valueOf)
              .forEach(SystemLoggerConfigTest::launch);
    }

    public static void launch(TestCase test) {
        switch(test) {
            case WITHSECURITY:
                Policy.setPolicy(new Policy() {
                    @Override
                    public boolean implies(ProtectionDomain domain, Permission permission) {
                        return true;
                    }
                });
                System.setSecurityManager(new SecurityManager());
                break;
            case NOSECURITY:
                break;
            default:
                throw new InternalError("Unexpected enum: " + test);
        }
        try {
            test(test.name(), ".1", ".child");
            test(test.name(), ".2", "");
            testUpdateConfiguration(test.name(), ".3");
            testSetPlatformLevel(test.name(), ".4");
        } catch (IOException io) {
            throw new UncheckedIOException(io);
        }
    }

    public static void test(String name, String step, String ext)
            throws IOException {

        System.out.println("\n*** Testing " + name + step + ext);

        final String systemName1a = "system.logger.one.a." + name + step + ext;
        final String systemName1b = "system.logger.one.b." + name + step + ext;
        final String appName1a = "system.logger.one.a." + name + step;
        final String appName1b = "system.logger.one.b." + name + step;
        final String msg1a = "logger name: " + systemName1a;
        final String msg1b = "logger name: " + systemName1b;
        final String systemName2 = "system.logger.two." + name + step + ext;
        final String appName2 = "system.logger.two." + name + step;
        final String msg2 = "logger name: " + systemName2;
        final String systemName3 = "system.logger.three." + name + step + ext;
        final String appName3 = "system.logger.three." + name + step;
        final String msg3 = "logger name: " + systemName3;
        List<LogRecord> records;

        System.out.println("\n[Case #1] Creating platform logger: " + systemName1a);
        PlatformLogger system1a = createPlatformLogger(systemName1a);
        System.out.println("    Creating platform logger: " + systemName1b);
        PlatformLogger system1b = createPlatformLogger(systemName1b);
        System.out.println("    Adding handler on root logger...");
        TestHandler test1 = new TestHandler();
        Logger.getLogger("").addHandler(test1);

        System.out.println("    Creating and configuring app logger: " + appName1a
                + ", " + appName1b);
        Logger app1a = Logger.getLogger(appName1a);
        app1a.setLevel(Level.INFO);
        Logger app1b = Logger.getLogger(appName1b);
        app1b.setLevel(Level.INFO);
        assertFalse(system1a.isLoggable(PlatformLogger.Level.FINEST),
                "Unexpected level for " + system1a);
        System.out.println("    Configuring root logger...");
        Logger.getLogger("").setLevel(Level.FINEST);
        System.out.println("    Logging through system logger: " + systemName1a);
        system1a.finest(msg1a);
        Reference.reachabilityFence(app1a);
        records = test1.drain();
        assertEquals(0, records.size(), "Unexpected size for " + records.toString());
        System.out.println("    Logging through system logger: " + systemName1b);
        system1b.finest(msg1b);
        Reference.reachabilityFence(app1b);
        records = test1.drain();
        assertEquals(0, records.size(), "Unexpected size for " + records.toString());
        Logger.getLogger("system.logger.one.a").finest("system.logger.one.a");
        records = test1.drain();
        assertEquals("system.logger.one.a", records.get(0).getMessage(), "Unexpected message: ");
        Logger.getLogger("").setLevel(Level.INFO);
        Logger.getLogger("system.logger.one.a").finest("system.logger.one.a");
        records = test1.drain();
        assertEquals(0, records.size(), "Unexpected size for " + records.toString());

        Reference.reachabilityFence(system1a);
        Reference.reachabilityFence(system1b);

        System.out.println("\n[Case #2] Creating system logger: " + systemName2);
        Logger system2 = createSystemLogger(systemName2);
        System.out.println("    Creating app logger: " + appName2);
        Logger app2 = Logger.getLogger(appName2);
        System.out.println("    Configuring app logger...");
        TestHandler test2 = new TestHandler();
        app2.setLevel(Level.ALL);
        app2.setUseParentHandlers(false);
        app2.addHandler(test2);
        System.out.println("    Logging through system logger: " + systemName2);
        system2.finest(msg2);
        records = test2.drain();
        assertEquals(1, records.size(), "Unexpected size for " + records.toString());
        assertEquals(msg2, records.get(0).getMessage(), "Unexpected message: ");
        records = test1.drain();
        assertEquals(0, records.size(), "Unexpected size for " + records.toString());

        Reference.reachabilityFence(app2);
        Reference.reachabilityFence(system2);

        System.out.println("\n[Case #3] Creating app logger: " + appName3);
        Logger app3 = Logger.getLogger(appName3);
        System.out.println("    Configuring app logger...");
        TestHandler test3 = new TestHandler();
        app3.setLevel(Level.ALL);
        app3.setUseParentHandlers(false);
        app3.addHandler(test3);
        System.out.println("    Creating system logger: " + systemName3);
        Logger system3 = createSystemLogger(systemName3);
        System.out.println("    Logging through system logger: " + systemName3);
        system3.finest(msg3);
        records = test3.drain();
        assertEquals(1, records.size(), "Unexpected size for " + records.toString());
        assertEquals(msg3, records.get(0).getMessage(), "Unexpected message: ");
        records = test1.drain();
        assertEquals(0, records.size(), "Unexpected size for " + records.toString());

        Reference.reachabilityFence(app3);
        Reference.reachabilityFence(system3);
        System.gc();

    }

    @SuppressWarnings("deprecated")
    static void setPlatformLevel(PlatformLogger logger, PlatformLogger.Level level) {
        logger.setLevel(level);
    }

    public static void testSetPlatformLevel(String name, String step) {
        System.out.println("\n*** Testing PlatformLogger.setLevel " + name + step);

        System.out.println("\n[Case #5] Creating app logger: " + name + step);
        // this should return named logger in the global context
        Logger foo = Logger.getLogger(name + step);
        foo.setLevel(Level.FINE);

        System.out.println("    Creating platform logger: " + name + step);
        PlatformLogger foo1 = PlatformLogger.getLogger(name + step);
        System.out.println("    Configuring platform logger...");
        setPlatformLevel(foo1, PlatformLogger.Level.INFO);

        System.out.println("    Checking levels...");
        assertEquals(foo.getName(), foo1.getName(), "Bad logger names");
         // both logger share the same config
        assertEquals(foo.getLevel(), Level.INFO, "Bad level for user logger");
        assertEquals(foo1.level(), PlatformLogger.Level.INFO,
                "Bad level for platform logger");

    }

    static void updateConfiguration(Properties props) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, "");
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LogManager.getLogManager().updateConfiguration(bais, (k) -> (o,n) -> n != null ? n : o);
    }

    static void readConfiguration(Properties props) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(baos, "");
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        LogManager.getLogManager().readConfiguration(bais);
    }

    // Tests that though two loggers exist, only one handler is created for the
    // pair when reading configuration.
    //
    public static void testUpdateConfiguration(String name, String step) throws IOException {

        System.out.println("\n*** Testing LogManager.updateConfiguration " + name + step);

        final String name1a = "system.logger.one.a." + name + step;
        final String name1b = "system.logger.one.b." + name + step;
        final String msg1a = "logger name: " + name1a;
        final String msg1b = "logger name: " + name1b;
        List<LogRecord> records;

        TestHandler1.COUNT.set(0);
        TestHandler2.COUNT.set(0);
        Properties props = new Properties();
        props.setProperty(name1a+".handlers", TestHandler1.class.getName());
        updateConfiguration(props);
        assertEquals(0, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(0, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        System.out.println("\n[Case #4] Creating app logger: " + name1a);
        Logger app1a = Logger.getLogger(name1a);
        System.out.println("    Configuring app logger...");
        TestHandler test1 = new TestHandler();
        app1a.setLevel(Level.ALL);
        app1a.setUseParentHandlers(false);
        app1a.addHandler(test1);
        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(0, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        System.out.println("    Creating system logger: " + name1a);
        Logger system1a = createSystemLogger(name1a);
        assertEquals(Level.ALL, system1a.getLevel(), "Bad level for system logger " + name1a);
        System.out.println("    Logging through system logger: " + name1a);
        system1a.finest(msg1a);
        records = test1.drain();
        assertEquals(1, records.size(), "Unexpected size for " + records.toString());
        assertEquals(msg1a, records.get(0).getMessage(), "Unexpected message: ");
        records = test1.drain();
        assertEquals(0, records.size(), "Unexpected size for " + records.toString());

        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(0, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        props.setProperty(name1a+".handlers", TestHandler2.class.getName());
        updateConfiguration(props);

        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(1, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        updateConfiguration(props);

        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(1, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        readConfiguration(props);

        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        // readConfiguration reset handlers but does not recreate them
        assertEquals(1, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        updateConfiguration(props);

        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(1, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        LogManager.getLogManager().reset();
        updateConfiguration(props);

        assertEquals(1, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(2, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        props.setProperty(name1a+".handlers",
                TestHandler2.class.getName() + "," + TestHandler1.class.getName());
        updateConfiguration(props);

        assertEquals(2, TestHandler1.COUNT.get(), "Bad instance count for "
                + TestHandler1.class.getName());
        assertEquals(3, TestHandler2.COUNT.get(), "Bad instance count for "
                + TestHandler2.class.getName());

        Reference.reachabilityFence(app1a);
        Reference.reachabilityFence(system1a);

        LogManager.getLogManager().readConfiguration();
        System.gc();
    }



}
