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

import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * @test
 * @bug 8140364
 * @summary Tests the default body of the System.Logger interface.
 * @author danielfuchs
 */
public class LoggerInterfaceTest {

    public static class LoggerImpl implements System.Logger {

        public static class LogEvent implements Cloneable {
            Level level;
            ResourceBundle bundle;
            String msg;
            Throwable thrown;
            Object[] params;
            StackTraceElement[] callStack;

            @Override
            protected LogEvent clone() {
                try {
                    return (LogEvent)super.clone();
                } catch (CloneNotSupportedException x) {
                    throw new RuntimeException(x);
                }
            }


        }

        public static class LogEventBuilder {
            private LogEvent event = new LogEvent();
            public LogEventBuilder level(Level level) {
                event.level = level;
                return this;
            }
            public LogEventBuilder stack(StackTraceElement... stack) {
                event.callStack = stack;
                return this;
            }
            public LogEventBuilder bundle(ResourceBundle bundle) {
                event.bundle = bundle;
                return this;
            }
            public LogEventBuilder msg(String msg) {
                event.msg = msg;
                return this;
            }
            public LogEventBuilder thrown(Throwable thrown) {
                event.thrown = thrown;
                return this;
            }
            public LogEventBuilder params(Object... params) {
                event.params = params;
                return this;
            }
            public LogEvent build() {
                return event.clone();
            }

            public LogEventBuilder clear() {
                event = new LogEvent();
                return this;
            }

        }

        Level level = Level.WARNING;
        Consumer<LogEvent> consumer;
        final LogEventBuilder builder = new LogEventBuilder();

        @Override
        public String getName() {
            return "noname";
        }

        @Override
        public boolean isLoggable(Level level) {
            return level.getSeverity() >= this.level.getSeverity();
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            builder.clear().level(level).bundle(bundle).msg(msg).thrown(thrown)
                    .stack(new Exception().getStackTrace());
            consumer.accept(builder.build());
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            builder.clear().level(level).bundle(bundle).msg(format).params(params)
                    .stack(new Exception().getStackTrace());
            consumer.accept(builder.build());
        }

    }

    static class Throwing {
        @Override
        public String toString() {
            throw new RuntimeException("should not have been called");
        }
    }
    static class NotTrowing {
        private final String toString;
        private int count = 0;
        public NotTrowing(String toString) {
            this.toString = toString;
        }

        @Override
        public String toString() {
            return toString + "[" + (++count) + "]";
        }
    }

    public static void main(String[] args) {
        final LoggerImpl loggerImpl = new LoggerImpl();
        final System.Logger logger = loggerImpl;
        final Queue<LoggerImpl.LogEvent> events = new LinkedList<>();
        loggerImpl.consumer = (x) -> events.add(x);

        System.out.println("\nlogger.isLoggable(Level)");
        assertTrue(logger.isLoggable(Level.WARNING), "logger.isLoggable(Level.WARNING)","  ");
        assertFalse(logger.isLoggable(Level.INFO), "logger.isLoggable(Level.INFO)", "  ");


        System.out.println("\nlogger.log(Level, Object)");
        for (Level l : Level.values()) {
            boolean logged = l.compareTo(Level.WARNING) >= 0;
            Object[][] cases = new Object[][] {
                {null}, {"baz"}
            };
            for (Object[] p : cases) {
                String msg = (String)p[0];
                final Object obj = msg == null ? null : logged ? new NotTrowing(msg) : new Throwing();
                String par1 = msg == null ? "(Object)null"
                        : logged ? "new NotTrowing(\""+ msg+"\")" : "new Throwing()";
                System.out.println("  logger.log(" + l + ", " +  par1 + ")");
                try {
                    logger.log(l, obj);
                    if (obj == null) {
                        throw new RuntimeException("Expected NullPointerException not thrown for"
                                  + " logger.log(" + l + ", " +  par1 + ")");
                    }
                } catch (NullPointerException x) {
                    if (obj == null) {
                        System.out.println("    Got expected exception: " + x);
                        continue;
                    } else {
                        throw x;
                    }
                }
                LoggerImpl.LogEvent e = events.poll();
                if (logged) {
                    assertNonNull(e, "e", "    ");
                    assertEquals(l, e.level, "e.level", "    ");
                    assertToString(e.msg, msg, 1, "e.msg", "    ");
                    assertEquals(e.bundle, null, "e.bundle", "    ");
                    assertEquals(e.params, null, "e.params", "    ");
                    assertEquals(e.thrown, null, "e.thrown", "    ");
                    assertEquals(e.bundle, null, "e.bundle", "    ");
                    assertEquals(e.callStack[0].getMethodName(), "log",
                                 "e.callStack[0].getMethodName()", "    ");
                    assertEquals(e.callStack[0].getClassName(),
                                logger.getClass().getName(),
                                "e.callStack[0].getClassName() ", "    ");
                    assertEquals(e.callStack[1].getMethodName(), "log",
                                 "e.callStack[1].getMethodName()", "    ");
                    assertEquals(e.callStack[1].getClassName(),
                                 System.Logger.class.getName(),
                                 "e.callStack[1].getClassName() ", "    ");
                    assertEquals(e.callStack[2].getMethodName(), "main",
                                 "e.callStack[2].getMethodName()", "    ");
                } else {
                    assertEquals(e, null, "e", "    ");
                }
            }
        }
        System.out.println("  logger.log(" + null + ", " +
                "new NotThrowing(\"foobar\")" + ")");
        try {
            logger.log(null, new NotTrowing("foobar"));
            throw new RuntimeException("Expected NullPointerException not thrown for"
                                      + " logger.log(" + null + ", "
                                      + "new NotThrowing(\"foobar\")" + ")");
        } catch (NullPointerException x) {
            System.out.println("    Got expected exception: " + x);
        }


        System.out.println("\nlogger.log(Level, String)");
        for (Level l : Level.values()) {
            boolean logged = l.compareTo(Level.WARNING) >= 0;
            String par = "bar";
            System.out.println("  logger.log(" + l + ", \"" +  par +"\");");
            logger.log(l, par);
            LoggerImpl.LogEvent e = events.poll();
            assertNonNull(e, "e", "    ");
            assertEquals(e.level, l, "e.level", "    ");
            assertEquals(e.msg, "bar", "e.msg", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.params, null, "e.params", "    ");
            assertEquals(e.thrown, null, "e.thrown", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.callStack[0].getMethodName(), "log",
                         "e.callStack[0].getMethodName()", "    ");
            assertEquals(e.callStack[0].getClassName(),
                         logger.getClass().getName(),
                         "e.callStack[0].getClassName() ", "    ");
            assertEquals(e.callStack[1].getMethodName(), "log",
                         "e.callStack[1].getMethodName()", "    ");
            assertEquals(e.callStack[1].getClassName(),
                         System.Logger.class.getName(),
                         "e.callStack[1].getClassName() ", "    ");
            assertEquals(e.callStack[2].getMethodName(), "main",
                         "e.callStack[2].getMethodName()", "    ");

            System.out.println("  logger.log(" + l + ", (String)null);");
            logger.log(l, (String)null);
            e = events.poll();
            assertNonNull(e, "e", "    ");
            assertEquals(e.level, l, "e.level", "    ");
            assertEquals(e.msg, null, "e.msg", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.params, null, "e.params", "    ");
            assertEquals(e.thrown, null, "e.thrown", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.callStack[0].getMethodName(), "log",
                         "e.callStack[0].getMethodName()", "    ");
            assertEquals(e.callStack[0].getClassName(),
                         logger.getClass().getName(),
                         "e.callStack[0].getClassName() ", "    ");
            assertEquals(e.callStack[1].getMethodName(), "log",
                         "e.callStack[1].getMethodName()", "    ");
            assertEquals(e.callStack[1].getClassName(),
                         System.Logger.class.getName(),
                         "e.callStack[1].getClassName() ", "    ");
            assertEquals(e.callStack[2].getMethodName(), "main",
                         "e.callStack[2].getMethodName()", "    ");
        }

        System.out.println("\nlogger.log(Level, Supplier<String>)");
        for (Level l : Level.values()) {
            boolean logged = l.compareTo(Level.WARNING) >= 0;
            Object[][] cases = new Object[][] {
                {null}, {"baz"}
            };
            for (Object[] p : cases) {
                String msg = (String)p[0];
                final Object obj = msg == null ? null : logged ? new NotTrowing(msg) : new Throwing();
                final Supplier<String> s = msg == null ? null : () -> obj.toString();
                String par1 = msg == null ? "(Supplier<String>)null"
                        : logged ? "() -> new NotTrowing(\""+ msg+"\").toString()" : "new Throwing()";
                System.out.println("  logger.log(" + l + ", " +  par1 + ")");
                try {
                    logger.log(l, s);
                    if (s == null) {
                        throw new RuntimeException("Expected NullPointerException not thrown for"
                                  + " logger.log(" + l + ", " +  par1 + ")");
                    }
                } catch (NullPointerException x) {
                    if (s == null) {
                        System.out.println("    Got expected exception: " + x);
                        continue;
                    } else {
                        throw x;
                    }
                }
                LoggerImpl.LogEvent e = events.poll();
                if (logged) {
                    assertNonNull(e, "e", "    ");
                    assertEquals(l, e.level, "e.level", "    ");
                    assertToString(e.msg, msg, 1, "e.msg", "    ");
                    assertEquals(e.bundle, null, "e.bundle", "    ");
                    assertEquals(e.params, null, "e.params", "    ");
                    assertEquals(e.thrown, null, "e.thrown", "    ");
                    assertEquals(e.bundle, null, "e.bundle", "    ");
                    assertEquals(e.callStack[0].getMethodName(), "log",
                                 "e.callStack[0].getMethodName()", "    ");
                    assertEquals(e.callStack[0].getClassName(),
                                 logger.getClass().getName(),
                                 "e.callStack[0].getClassName() ", "    ");
                    assertEquals(e.callStack[1].getMethodName(), "log",
                                 "e.callStack[1].getMethodName()", "    ");
                    assertEquals(e.callStack[1].getClassName(),
                                 System.Logger.class.getName(),
                                 "e.callStack[1].getClassName() ", "    ");
                    assertEquals(e.callStack[2].getMethodName(), "main",
                                 "e.callStack[2].getMethodName()", "    ");
                } else {
                    assertEquals(e, null, "e", "    ");
                }
            }
        }
        System.out.println("  logger.log(" + null + ", " + "() -> \"biz\"" + ")");
        try {
            logger.log(null, () -> "biz");
            throw new RuntimeException("Expected NullPointerException not thrown for"
                                      + " logger.log(" + null + ", "
                                      + "() -> \"biz\"" + ")");
        } catch (NullPointerException x) {
            System.out.println("    Got expected exception: " + x);
        }

        System.out.println("\nlogger.log(Level, String, Object...)");
        for (Level l : Level.values()) {
            boolean logged = l.compareTo(Level.WARNING) >= 0;
            String par = "bam";
            Object[] params = null;
            System.out.println("  logger.log(" + l + ", \"" +  par +"\", null);");
            logger.log(l, par, params);
            LoggerImpl.LogEvent e = events.poll();
            assertNonNull(e, "e", "    ");
            assertEquals(l, e.level, "e.level", "    ");
            assertEquals(e.msg, "bam", "e.msg", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.params, null, "e.params", "    ");
            assertEquals(e.thrown, null, "e.thrown", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.callStack[0].getMethodName(), "log",
                        "e.callStack[0].getMethodName()", "    ");
            assertEquals(e.callStack[0].getClassName(),
                         logger.getClass().getName(),
                         "e.callStack[0].getClassName() ", "    ");
            assertEquals(e.callStack[1].getMethodName(), "log",
                         "e.callStack[1].getMethodName()", "    ");
            assertEquals(e.callStack[1].getClassName(),
                         System.Logger.class.getName(),
                         "e.callStack[1].getClassName() ", "    ");
            assertEquals(e.callStack[2].getMethodName(), "main",
                         "e.callStack[2].getMethodName()", "    ");

            params = new Object[] {new NotTrowing("one")};
            par = "bam {0}";
            System.out.println("  logger.log(" + l + ", \"" +  par
                               + "\", new NotTrowing(\"one\"));");
            logger.log(l, par, params[0]);
            e = events.poll();
            assertNonNull(e, "e", "    ");
            assertEquals(l, e.level, "e.level", "    ");
            assertEquals(e.msg, par, "e.msg", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertArrayEquals(e.params, params, "e.params", "    ");
            assertEquals(e.thrown, null, "e.thrown", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.callStack[0].getMethodName(), "log",
                         "e.callStack[0].getMethodName()", "    ");
            assertEquals(e.callStack[0].getClassName(),
                         logger.getClass().getName(),
                         "e.callStack[0].getClassName() ", "    ");
            assertEquals(e.callStack[1].getMethodName(), "log",
                         "e.callStack[1].getMethodName()", "    ");
            assertEquals(e.callStack[1].getClassName(),
                         System.Logger.class.getName(),
                         "e.callStack[1].getClassName() ", "    ");
            assertEquals(e.callStack[2].getMethodName(), "main",
                         "e.callStack[2].getMethodName()", "    ");

            params = new Object[] {new NotTrowing("fisrt"), new NotTrowing("second")};
            par = "bam {0} {1}";
            System.out.println("  logger.log(" + l + ", \"" +  par
                              + "\", new NotTrowing(\"fisrt\"),"
                              + " new NotTrowing(\"second\"));");
            logger.log(l, par, params[0], params[1]);
            e = events.poll();
            assertNonNull(e, "e", "    ");
            assertEquals(l, e.level, "e.level", "    ");
            assertEquals(e.msg, par, "e.msg", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertArrayEquals(e.params, params, "e.params", "    ");
            assertEquals(e.thrown, null, "e.thrown", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.callStack[0].getMethodName(), "log",
                         "e.callStack[0].getMethodName()", "    ");
            assertEquals(e.callStack[0].getClassName(),
                         logger.getClass().getName(),
                         "e.callStack[0].getClassName() ", "    ");
            assertEquals(e.callStack[1].getMethodName(), "log",
                         "e.callStack[1].getMethodName()", "    ");
            assertEquals(e.callStack[1].getClassName(),
                         System.Logger.class.getName(),
                         "e.callStack[1].getClassName() ", "    ");
            assertEquals(e.callStack[2].getMethodName(), "main",
                         "e.callStack[2].getMethodName()", "    ");

            params = new Object[] {new NotTrowing("third"), new NotTrowing("fourth")};
            par = "bam {2}";
            System.out.println("  logger.log(" + l + ", \"" +  par
                              + "\", new Object[] {new NotTrowing(\"third\"),"
                              + " new NotTrowing(\"fourth\")});");
            logger.log(l, par, params);
            e = events.poll();
            assertNonNull(e, "e", "    ");
            assertEquals(l, e.level, "e.level", "    ");
            assertEquals(e.msg, par, "e.msg", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertArrayEquals(e.params, params, "e.params", "    ");
            assertEquals(e.thrown, null, "e.thrown", "    ");
            assertEquals(e.bundle, null, "e.bundle", "    ");
            assertEquals(e.callStack[0].getMethodName(), "log",
                         "e.callStack[0].getMethodName()", "    ");
            assertEquals(e.callStack[0].getClassName(), logger.getClass().getName(),
                         "e.callStack[0].getClassName() ", "    ");
            assertEquals(e.callStack[1].getMethodName(), "log",
                         "e.callStack[1].getMethodName()", "    ");
            assertEquals(e.callStack[1].getClassName(),
                         System.Logger.class.getName(),
                         "e.callStack[1].getClassName() ", "    ");
            assertEquals(e.callStack[2].getMethodName(), "main",
                        "e.callStack[2].getMethodName()", "    ");
        }

        System.out.println("\nlogger.log(Level, String, Throwable)");
        for (Level l : Level.values()) {
            boolean logged = l.compareTo(Level.WARNING) >= 0;
            Object[][] cases = new Object[][] {
                {null, null}, {null, new Throwable()}, {"biz", null}, {"boz", new Throwable()}
            };
            for (Object[] p : cases) {
                String msg = (String)p[0];
                Throwable thrown = (Throwable)p[1];
                String par1 = msg == null ? "(String)null" : "\"" + msg + "\"";
                String par2 = thrown == null ? "(Throwable)null" : "new Throwable()";
                System.out.println("  logger.log(" + l + ", " +  par1 +", " + par2 + ")");
                logger.log(l, msg, thrown);
                LoggerImpl.LogEvent e = events.poll();
                assertNonNull(e, "e", "    ");
                assertEquals(e.level, l, "e.level", "    ");
                assertEquals(e.msg, msg, "e.msg", "    ");
                assertEquals(e.bundle, null, "e.bundle", "    ");
                assertEquals(e.params, null, "e.params", "    ");
                assertEquals(e.thrown, thrown, "e.thrown", "    ");
                assertEquals(e.bundle, null, "e.bundle", "    ");
                assertEquals(e.callStack[0].getMethodName(),
                             "log", "e.callStack[0].getMethodName()", "    ");
                assertEquals(e.callStack[0].getClassName(),
                            logger.getClass().getName(),
                            "e.callStack[0].getClassName() ", "    ");
                assertEquals(e.callStack[1].getMethodName(), "log",
                             "e.callStack[1].getMethodName()", "    ");
                assertEquals(e.callStack[1].getClassName(),
                            System.Logger.class.getName(),
                            "e.callStack[1].getClassName() ", "    ");
                assertEquals(e.callStack[2].getMethodName(), "main",
                             "e.callStack[2].getMethodName()", "    ");
            }
        }

        System.out.println("\nlogger.log(Level, Supplier<String>, Throwable)");
        for (Level l : Level.values()) {
            boolean logged = l.compareTo(Level.WARNING) >= 0;
            Object[][] cases = new Object[][] {
                {null, null}, {null, new Throwable()}, {"biz", null}, {"boz", new Throwable()}
            };
            for (Object[] p : cases) {
                String msg = (String)p[0];
                Throwable thrown = (Throwable)p[1];
                final Object obj = msg == null ? null : logged ? new NotTrowing(msg) : new Throwing();
                final Supplier<String> s = msg == null ? null : () -> obj.toString();
                String par1 = msg == null ? "(Supplier<String>)null"
                        : logged ? "() -> new NotTrowing(\""+ msg+"\").toString()" : "new Throwing()";
                String par2 = thrown == null ? "(Throwable)null" : "new Throwable()";
                System.out.println("  logger.log(" + l + ", " +  par1 +", " + par2 + ")");
                try {
                    logger.log(l, s, thrown);
                    if (s== null) {
                        throw new RuntimeException("Expected NullPointerException not thrown for"
                                  + " logger.log(" + l + ", " +  par1 +", " + par2 + ")");
                    }
                } catch (NullPointerException x) {
                    if (s == null) {
                        System.out.println("    Got expected exception: " + x);
                        continue;
                    } else {
                        throw x;
                    }
                }
                LoggerImpl.LogEvent e = events.poll();
                if (logged) {
                    assertNonNull(e, "e", "    ");
                    assertEquals(l, e.level, "e.level", "    ");
                    assertToString(e.msg, msg, 1, "e.msg", "    ");
                    assertEquals(e.bundle, null, "e.bundle", "    ");
                    assertEquals(e.params, null, "e.params", "    ");
                    assertEquals(e.thrown, thrown, "e.thrown", "    ");
                    assertEquals(e.bundle, null, "e.bundle", "    ");
                    assertEquals(e.callStack[0].getMethodName(), "log",
                                 "e.callStack[0].getMethodName()", "    ");
                    assertEquals(e.callStack[0].getClassName(),
                                 logger.getClass().getName(),
                                 "e.callStack[0].getClassName() ", "    ");
                    assertEquals(e.callStack[1].getMethodName(), "log",
                                 "e.callStack[1].getMethodName()", "    ");
                    assertEquals(e.callStack[1].getClassName(),
                                 System.Logger.class.getName(),
                                 "e.callStack[1].getClassName() ", "    ");
                    assertEquals(e.callStack[2].getMethodName(), "main",
                                 "e.callStack[2].getMethodName()", "    ");
                } else {
                    assertEquals(e, null, "e", "    ");
                }
            }
        }
        System.out.println("  logger.log(" + null + ", " + "() -> \"biz\""
                           + ", " + "new Throwable()" + ")");
        try {
            logger.log(null, () -> "biz", new Throwable());
            throw new RuntimeException("Expected NullPointerException not thrown for"
                                      + " logger.log(" + null + ", "
                                      + "() -> \"biz\"" + ", "
                                      + "new Throwable()" + ")");
        } catch (NullPointerException x) {
            System.out.println("    Got expected exception: " + x);
        }

        System.out.println("Checking that we have no spurious events in the queue");
        assertEquals(events.poll(), null, "events.poll()", "  ");
    }

    static void assertTrue(boolean test, String what, String prefix) {
        if (!test) {
            throw new RuntimeException("Expected true for " + what);
        }
        System.out.println(prefix + "Got expected " + what + ": " + test);
    }
    static void assertFalse(boolean test, String what, String prefix) {
        if (test) {
            throw new RuntimeException("Expected false for " + what);
        }
        System.out.println(prefix + "Got expected " + what + ": " + test);
    }
    static void assertToString(String actual, String expected, int count, String what, String prefix) {
        assertEquals(actual, expected + "["+count+"]", what, prefix);
    }
    static void assertEquals(Object actual, Object expected, String what, String prefix) {
        if (!Objects.equals(actual, expected)) {
            throw new RuntimeException("Bad " + what + ":"
                    + "\n\t expected: " + expected
                    + "\n\t   actual: " + actual);
        }
        System.out.println(prefix + "Got expected " + what + ": " + actual);
    }
    static void assertArrayEquals(Object[] actual, Object[] expected, String what, String prefix) {
        if (!Objects.deepEquals(actual, expected)) {
            throw new RuntimeException("Bad " + what + ":"
                    + "\n\t expected: " + expected == null ? "null" : Arrays.deepToString(expected)
                    + "\n\t   actual: " + actual == null ? "null" : Arrays.deepToString(actual));
        }
        System.out.println(prefix + "Got expected " + what + ": " + Arrays.deepToString(actual));
    }
    static void assertNonNull(Object actual, String what, String prefix) {
        if (Objects.equals(actual, null)) {
            throw new RuntimeException("Bad " + what + ":"
                    + "\n\t expected: non null"
                    + "\n\t   actual: " + actual);
        }
        System.out.println(prefix + "Got expected " + what + ": " + "non null");
    }
}
