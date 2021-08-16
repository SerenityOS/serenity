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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * @test
 * @bug 8163162
 * @summary Checks that LazyLoggers are returned for System.Logger instances
 *          created by modules in the platform class loader.
 * @modules java.base/java.lang:open
 * @build systempkg.log.SystemLoggerAccessor SystemLoggerInPlatformLoader
 * @run main/othervm SystemLoggerInPlatformLoader
 * @author danielfuchs
 */
public class SystemLoggerInPlatformLoader {

    static final class PlatformClassLoaderChild extends ClassLoader {
        private PlatformClassLoaderChild() {
            super(ClassLoader.getPlatformClassLoader());
        }
        public Class<?> definePlatformClass(String name) throws IOException {
            String testClasses = System.getProperty("test.classes", "./build/classes");
            String fname = name.replace('.', '/').concat(".class");
            try (InputStream is = new FileInputStream(new File(testClasses, fname))) {
                byte[] b = is.readAllBytes();
                ClassLoader parent = getParent();
                try {
                    Method m = ClassLoader.class.getDeclaredMethod("defineClass",
                        String.class, byte[].class, int.class, int.class);
                    m.setAccessible(true);
                    return (Class<?>)m.invoke(parent, name, b, 0, b.length);
                } catch (NoSuchMethodException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    throw new IOException(ex);
                }
            }
        }
        static final PlatformClassLoaderChild INSTANCE = new PlatformClassLoaderChild();
        static Class<?> loadLoggerAccessor() throws IOException {
            return INSTANCE.definePlatformClass("systempkg.log.SystemLoggerAccessor");
        }
    }

    static final Class<?> LOGGER_ACCESSOR_CLASS;
    static {
        try {
            LOGGER_ACCESSOR_CLASS = PlatformClassLoaderChild.loadLoggerAccessor();
            ClassLoader platformCL = ClassLoader.getPlatformClassLoader();
            if (LOGGER_ACCESSOR_CLASS.getClassLoader() != platformCL) {
                throw new ExceptionInInitializerError(
                    "Could not load accessor class in platform class loader: "
                     + LOGGER_ACCESSOR_CLASS.getClassLoader());
            }
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    // Returns a system logger created on behalf of a class loaded by the
    // Platform ClassLoader
    static System.Logger getSystemLogger(String name) {
        try {
            return (System.Logger)LOGGER_ACCESSOR_CLASS.getMethod(
                    "getSystemLogger", String.class).invoke(null, name);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException ex) {
            throw new RuntimeException("Failed to invoke LoggerAccessor.getJULLogger", ex);
        }
    }

    public static void main(String[] args) {
        System.Logger platformLogger = getSystemLogger("bar"); // for a platform class
        System.Logger appLogger = System.getLogger("bar"); // for an application class
        if (appLogger == platformLogger) {
            throw new RuntimeException("Same loggers");
        }
        Class<?> platformLoggerType = platformLogger.getClass();
        System.out.println("platformLogger: " + platformLoggerType);
        boolean simpleConsoleOnly = !ModuleLayer.boot().findModule("java.logging").isPresent();
        if (simpleConsoleOnly) {
            /* Happens if the test is called with custom JDK without java.logging module
               or in case usage commandline option --limit-modules java.base */
            if (!platformLoggerType.getSimpleName().equals("SimpleConsoleLogger")) {
                throw new RuntimeException(platformLoggerType.getSimpleName()
                      + ": unexpected class for platform logger"
                      + " (expected a simple console logger class)");
            }
        } else {
            if (!platformLoggerType.getSimpleName().equals("JdkLazyLogger")) {
                throw new RuntimeException(platformLoggerType.getSimpleName()
                      + ": unexpected class for platform logger"
                      + " (expected a lazy logger for a platform class)");
            }
            Class<?> appLoggerType = appLogger.getClass();
            System.out.println("appLogger: " + appLoggerType);
            if (appLoggerType.equals(platformLoggerType)) {
                throw new RuntimeException(appLoggerType
                      + ": unexpected class for application logger"
                      + " (a lazy logger was not expected"
                      + " for a non platform class)");
            }
        }
    }
}
