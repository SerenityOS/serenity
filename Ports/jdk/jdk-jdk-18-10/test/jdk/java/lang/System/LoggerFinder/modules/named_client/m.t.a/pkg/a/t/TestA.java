/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package pkg.a.t;

import java.lang.reflect.Method;
import java.lang.System.Logger;
import java.time.Instant;
import java.util.ResourceBundle;
import java.util.ListResourceBundle;

/*
 * Tests when logger client is in named module m.l.a
 */
public class TestA {

    public static void main(String[] args) throws Exception {
        assertTrue(args.length == 2);
        String loggerMode = args[0];
        String loggerClassName = args[1];
        System.err.println("TestA starting at " + Instant.now());
        try {
            testLogger(loggerMode, loggerClassName);
            testLog(loggerClassName);
        } finally {
            System.err.println("TestA finished at " + Instant.now());
        }
    }

    /*
     * Tests System.getLogger(String) get expected logger.
     */
    private static void testLogger(String loggerMode, String loggerClassName) {
        final Module m = TestA.class.getModule();
        final ClassLoader moduleCL = m.getClassLoader();
        assertTrue(m.isNamed());
        assertTrue(moduleCL != null);

        String name = "test.a";
        Logger logger = getLogger(name);
        printLogger(logger);

        final Module lm = logger.getClass().getModule();
        final ClassLoader loggerCL = lm.getClassLoader();
        if (loggerMode.equals("system")) {
            assertTrue(lm.isNamed());
            assertTrue(loggerCL == null);
        } else if(loggerMode.equals("named")) {
            assertTrue(lm.isNamed());
            assertTrue(loggerCL != null);
        } else if(loggerMode.equals("unnamed")) {
            assertTrue(!lm.isNamed());
            assertTrue(loggerCL != null);
        } else {
            throw new RuntimeException("wrong parameter");
        }

        assertTrue(loggerClassName.equals(logger.getClass().getName()));
        assertTrue(!loggerClassName.equals("jdk.internal.logger.LazyLoggers$JdkLazyLogger"));
    }

    /*
     * Tests Logger retrieved by System.getLogger(String, ResourceBundle) and
     * System.getLogger(String) works well.
     */
    private static void testLog(String loggerClassName) throws Exception {
        if (loggerClassName.equals("pkg.a.l.LoggerA")
                || loggerClassName.equals("pkg.b.l.LoggerB")) {

            String name = "test.a.A";
            String plainMsg = "this is test log message #1";
            ResourceBundle rb = new MyResourcesA();
            Throwable ex = new Throwable("this is an expected exception to be logged");
            Class<?> clazz = Class.forName(loggerClassName);
            Method method = clazz.getMethod("checkLog", String.class,
                                            System.Logger.Level.class,
                                            ResourceBundle.class, String.class,
                                            Throwable.class, Object[].class);

            Logger logger = getLogger(name);
            printLogger(logger);
            assertTrue(logger.getClass().getName().equals(loggerClassName));
            assertTrue(logger.getName().equals(loggerClassName));
            logger.log(Logger.Level.WARNING, plainMsg);
            boolean pass = (boolean)method.invoke(null, name,
                                                  Logger.Level.WARNING,
                                                  null, plainMsg, ex, (Object)null);
            assertTrue(pass);
            pass = (boolean)method.invoke(null, name, Logger.Level.INFO,
                                          rb, MyResourcesA.VALUE, (Throwable)null,
                                          (Object)null);
            assertTrue(!pass);

            logger = getLogger(name, rb);
            printLogger(logger);
            assertTrue(logger.getClass().getName()
                             .equals("jdk.internal.logger.LocalizedLoggerWrapper"));
            assertTrue(logger.getName().equals(loggerClassName));
            logger.log(Logger.Level.INFO, MyResourcesA.KEY);
            pass = (boolean)method.invoke(null, name, Logger.Level.INFO,
                                          rb, MyResourcesA.VALUE, (Throwable)null,
                                          (Object)null);
            assertTrue(pass);
            pass = (boolean)method.invoke(null, name, Logger.Level.WARNING,
                                          null, plainMsg, ex, (Object)null);
            assertTrue(pass);
        }
    }

    private static class MyResourcesA extends ListResourceBundle {
        static final String KEY = "this is the key in MyResourcesA";
        static final String VALUE = "THIS IS THE VALUE IN MyResourcesA";

        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {KEY, VALUE}
            };
        }
    }

    private static Logger getLogger(String name) {
        return System.getLogger(name);
    }

    private static Logger getLogger(String name, ResourceBundle rb) {
        return System.getLogger(name, rb);
    }

    private static void printLogger(Logger logger) {
        System.err.println("logger name: " + logger.getName()
                           + ", logger class: " + logger.getClass());
    }

    private static void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("expected true, but get false.");
        }
    }
}
