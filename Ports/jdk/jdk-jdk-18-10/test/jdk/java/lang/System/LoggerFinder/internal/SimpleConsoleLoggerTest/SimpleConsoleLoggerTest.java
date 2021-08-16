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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import jdk.internal.logger.SimpleConsoleLogger;
import jdk.internal.logger.SurrogateLogger;
import sun.util.logging.PlatformLogger;

/**
 * @test
 * @bug     8140364
 * @summary JDK implementation specific unit test for SimpleConsoleLogger.
 *          Tests the behavior of SimpleConsoleLogger.
 * @modules java.base/sun.util.logging
 *          java.base/jdk.internal.logger
 * @build SimpleConsoleLoggerTest
 * @run  main/othervm SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=OFF SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=ERROR SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=WARNING SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=INFO SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=DEBUG SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=TRACE SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=ALL SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=WOMBAT SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=FINEST SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=DEBUG -Djava.util.logging.SimpleFormatter.format=++++_%2$s%n%4$s:_%5$s%6$s%n SimpleConsoleLoggerTest
 * @run  main/othervm -Djdk.system.logger.level=DEBUG -Djdk.system.logger.format=++++_%2$s%n%4$s:_%5$s%6$s%n SimpleConsoleLoggerTest
 *
 * @author danielfuchs
 */
public class SimpleConsoleLoggerTest {

    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
    final static boolean VERBOSE = false;

    public static class MyBundle extends ResourceBundle {

        final ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();

        @Override
        protected Object handleGetObject(String key) {
            if (key.contains(" (translated)")) {
                throw new RuntimeException("Unexpected key: " + key);
            }
            return map.computeIfAbsent(key, k -> k.toUpperCase(Locale.ROOT) + " (translated)");
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(map.keySet());
        }

    }
    public static class MyLoggerBundle extends MyBundle {

    }


    static class ErrorStream extends PrintStream {

        static AtomicBoolean forward = new AtomicBoolean();
        ByteArrayOutputStream out;
        String saved = "";
        public ErrorStream(ByteArrayOutputStream out) {
            super(out);
            this.out = out;
        }

        @Override
        public void write(int b) {
            super.write(b);
            if (forward.get()) err.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            super.write(b);
            if (forward.get()) err.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (forward.get()) err.write(buf, off, len);
        }

        public String peek() {
            flush();
            return out.toString();
        }

        public String drain() {
            flush();
            String res = out.toString();
            out.reset();
            return res;
        }

        public void store() {
            flush();
            saved = out.toString();
            out.reset();
        }

        public void restore() {
            out.reset();
            try {
                out.write(saved.getBytes());
            } catch(IOException io) {
                throw new UncheckedIOException(io);
            }
        }

        static final PrintStream err = System.err;
        static final ErrorStream errorStream = new ErrorStream(new ByteArrayOutputStream());
    }

    private static StringBuilder appendProperty(StringBuilder b, String name) {
        String value = System.getProperty(name);
        if (value == null) return b;
        return b.append(name).append("=").append(value).append('\n');
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        System.setErr(ErrorStream.errorStream);
        try {
            test(args);
        } finally {
            try {
                System.setErr(ErrorStream.err);
            } catch (Error | RuntimeException x) {
                x.printStackTrace(ErrorStream.err);
            }
        }
    }


    public static void test(String[] args) {

        ErrorStream.errorStream.restore();
        String l = System.getProperty("jdk.system.logger.level");
        String f = System.getProperty("jdk.system.logger.format");
        String jf = System.getProperty("java.util.logging.SimpleFormatter.format");
        System.out.println("Running test: "
            + "\n\tjdk.system.logger.level=\"" + l + "\""
            + "\n\tjdk.system.logger.format=\"" + f + "\""
            + "\n\tjava.util.logging.SimpleFormatter.format=\"" + jf + "\"");

        test(l,f,jf);
        System.out.println("\nPASSED: tested " + SEQUENCER.get() + " test cases");
    }

    static final AtomicLong SEQUENCER = new AtomicLong();
    public static void test(String defaultLevel, String defaultFormat, String julFormat) {

        final Map<Logger, String> loggerDescMap = new HashMap<>();

        SimpleConsoleLogger simple = SimpleConsoleLogger.makeSimpleLogger("test.logger");
        loggerDescMap.put(simple, "SimpleConsoleLogger.makeSimpleLogger(\"test.logger\")");
        SimpleConsoleLogger temporary = SurrogateLogger.makeSurrogateLogger("test.logger");
        loggerDescMap.put(temporary, "SurrogateLogger.makeSimpleLogger(\"test.logger\")");

        Level level;
        try {
            level = defaultLevel == null ? null : Level.valueOf(defaultLevel);
        } catch (IllegalArgumentException ex) {
            level = null;
        }
        testLogger(loggerDescMap, simple, level, false, defaultFormat);
        testLogger(loggerDescMap, temporary, null, true, julFormat);
    }

    public static class Foo {

    }

    static void verbose(String msg) {
       if (VERBOSE) {
           System.out.println(msg);
       }
    }

    static String getName(Level level, boolean usePlatformLevel) {
        if (usePlatformLevel) {
            return PlatformLogger.toPlatformLevel(level).name();
        } else {
            return level.getName();
        }
    }

    // Calls the 8 methods defined on Logger and verify the
    // parameters received by the underlying TestProvider.LoggerImpl
    // logger.
    private static void testLogger(Map<Logger, String> loggerDescMap,
            SimpleConsoleLogger simple,
            Level defaultLevel,
            boolean usePlatformLevel,
            String defaultFormat) {

        System.out.println("Testing " + loggerDescMap.get(simple) + " [" + simple +"]");

        String formatStrMarker = defaultFormat == null ? ""
                : defaultFormat.startsWith("++++") ? "++++" : "";
        String unexpectedMarker = defaultFormat == null ? "++++"
                : defaultFormat.startsWith("++++") ? "????" : "++++";
        String formatStrSpec = defaultFormat == null ? "[date]"
                : defaultFormat.startsWith("++++") ? "++++" : "????";
        String sep = defaultFormat == null ? ": " : ":_";
        String sepw = defaultFormat == null ? " " : "_";

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                ErrorStream.errorStream.drain();
                String desc = "logger.log(messageLevel, foo): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, foo);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF
                    || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String expected = getName(messageLevel, usePlatformLevel) + sep + fooMsg;
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(expected)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + expected
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        String msg = "blah";
        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\"): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, msg);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF
                    || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String expected = getName(messageLevel, usePlatformLevel) + sep + msg;
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(expected)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + expected
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        Supplier<String> fooSupplier = new Supplier<String>() {
            @Override
            public String get() {
                return this.toString();
            }
        };

        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, fooSupplier);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF
                    || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String expected = getName(messageLevel, usePlatformLevel) + sep + fooSupplier.get();
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(expected)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + expected
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }


        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = msg;
        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, format, foo, msg);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String msgFormat = format;
                    String text = java.text.MessageFormat.format(msgFormat, foo, msg);
                    String expected = getName(messageLevel, usePlatformLevel) + sep + text;
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || !logged.contains(expected)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + expected
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, msg, thrown);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thrown.printStackTrace(new PrintStream(baos));
                    String text = baos.toString();
                    String expected = getName(messageLevel, usePlatformLevel) + sep + msg;
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || !logged.contains(expected)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + msg +"\n"
                                + text
                                + ">>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }


        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, thrown, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, fooSupplier, thrown);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thrown.printStackTrace(new PrintStream(baos));
                    String text = baos.toString();
                    String expected = getName(messageLevel, usePlatformLevel) + sep + fooSupplier.get();
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || !logged.contains(expected)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + expected +"\n"
                                + text
                                + ">>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, bundle, format, foo, msg);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String text = java.text.MessageFormat.format(bundle.getString(format), foo, msg);
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(getName(messageLevel, usePlatformLevel) + sep + text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + getName(messageLevel, usePlatformLevel) + " " + text
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        for (Level loggerLevel : defaultLevel == null
                ? EnumSet.of(Level.INFO) : EnumSet.of(defaultLevel)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                SEQUENCER.incrementAndGet();
                simple.log(messageLevel, bundle, msg, thrown);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String textMsg = bundle.getString(msg);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thrown.printStackTrace(new PrintStream(baos));
                    String text = baos.toString();
                    String expected = getName(messageLevel, usePlatformLevel) + sep + textMsg;
                    if (!logged.contains("SimpleConsoleLoggerTest testLogger")
                        || !logged.contains(formatStrMarker)
                        || !logged.contains(expected)
                        || logged.contains(unexpectedMarker)
                        || !logged.contains(text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + formatStrSpec + sepw + "SimpleConsoleLoggerTest testLogger\n"
                                + expected +"\n"
                                + text
                                + ">>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

    }
}
