/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6479237
 * @summary Test the format of StackTraceElement::toString and its serial form
 * @modules java.logging
 *
 * @run main SerialTest
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

public class SerialTest {
    private static final Path SER_DIR = Paths.get("sers");
    private static final String JAVA_BASE = "java.base";
    private static final String JAVA_LOGGING = "java.logging";

    private static boolean isImage;

    public static void main(String... args) throws Exception {
        Files.createDirectories(SER_DIR);

        // detect if exploded image build
        Path home = Paths.get(System.getProperty("java.home"));
        isImage = Files.exists(home.resolve("lib").resolve("modules"));

        // test stack trace from built-in loaders
        try {
            Logger.getLogger(null);
        } catch (NullPointerException e) {
            Arrays.stream(e.getStackTrace())
                  .filter(ste -> ste.getClassName().startsWith("java.util.logging.") ||
                                 ste.getClassName().equals("SerialTest"))
                  .forEach(SerialTest::test);
        }

        // test stack trace with class loader name from other class loader
        Loader loader = new Loader("myloader");
        Class<?> cls = Class.forName("SerialTest", true, loader);
        Method method = cls.getMethod("throwException");
        StackTraceElement ste = (StackTraceElement)method.invoke(null);
        test(ste, loader);

        // verify the class loader name and in the stack trace
        if (!cls.getClassLoader().getName().equals("myloader.hacked")) {
            throw new RuntimeException("Unexpected loader name: " +
                cls.getClassLoader().getName());
        }
        if (!ste.getClassLoaderName().equals("myloader")) {
            throw new RuntimeException("Unexpected loader name: " +
                ste.getClassLoaderName());
        }
    }

    private static void test(StackTraceElement ste) {
        test(ste, null);
    }

    private static void test(StackTraceElement ste, ClassLoader loader) {
        try {
            SerialTest serialTest = new SerialTest(ste);
            StackTraceElement ste2 = serialTest.serialize().deserialize();
            System.out.println(ste2);
            // verify StackTraceElement::toString returns the same string
            if (!ste.equals(ste2) || !ste.toString().equals(ste2.toString())) {
                throw new RuntimeException(ste + " != " + ste2);
            }

            String mn = ste.getModuleName();
            if (mn != null) {
                switch (mn) {
                    case JAVA_BASE:
                    case JAVA_LOGGING:
                        checkNamedModule(ste, loader, false);
                        break;
                    default:  // ignore
                }
            } else {
                checkUnnamedModule(ste, loader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkUnnamedModule(StackTraceElement ste, ClassLoader loader) {
        String mn = ste.getModuleName();
        String s = ste.toString();
        int i = s.indexOf('/');

        if (mn != null) {
            throw new RuntimeException("expected null but got " + mn);
        }

        if (loader != null) {
            // Expect <loader>//<classname>.<method>(<src>:<ln>)
            if (i <= 0) {
                throw new RuntimeException("loader name missing: " + s);
            }
            if (!getLoaderName(loader).equals(s.substring(0, i))) {
                throw new RuntimeException("unexpected loader name: " + s);
            }
            int j = s.substring(i+1).indexOf('/');
            if (j != 0) {
                throw new RuntimeException("unexpected element for unnamed module: " + s);
            }
        }
    }

    /*
     * Loader::getName is overridden to return some other name
     */
    private static String getLoaderName(ClassLoader loader) {
        if (loader == null)
            return "";

        if (loader instanceof Loader) {
            return ((Loader) loader).name;
        } else {
            return loader.getName();
        }
    }

    private static void checkNamedModule(StackTraceElement ste,
                                         ClassLoader loader,
                                         boolean showVersion) {
        String loaderName = getLoaderName(loader);
        String mn = ste.getModuleName();
        String s = ste.toString();
        int i = s.indexOf('/');

        if (mn == null) {
            throw new RuntimeException("expected module name: " + s);
        }

        if (i <= 0) {
            throw new RuntimeException("module name missing: " + s);
        }

        // Expect <module>/<classname>.<method>(<src>:<ln>)
        if (!loaderName.isEmpty()) {
            throw new IllegalArgumentException(loaderName);
        }

        // <module>: name@version
        int j = s.indexOf('@');
        if ((showVersion && j <= 0) || (!showVersion && j >= 0)) {
            throw new RuntimeException("unexpected version: " + s);
        }

        String name = j < 0 ? s.substring(0, i) : s.substring(0, j);
        if (!name.equals(mn)) {
            throw new RuntimeException("unexpected module name: " + s);
        }
    }

    private final Path ser;
    private final StackTraceElement ste;
    SerialTest(StackTraceElement ste) throws IOException {
        this.ser = Files.createTempFile(SER_DIR, "SerialTest", ".ser");
        this.ste = ste;
    }

    private StackTraceElement deserialize() throws IOException {
        try (InputStream in = Files.newInputStream(ser);
             BufferedInputStream bis = new BufferedInputStream(in);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (StackTraceElement)ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private SerialTest serialize() throws IOException {
        try (OutputStream out = Files.newOutputStream(ser);
             BufferedOutputStream bos = new BufferedOutputStream(out);
            ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(ste);
        }
        return this;
    }


    public static StackTraceElement throwException() {
        try {
            Integer.parseInt(null);
        } catch (NumberFormatException e) {
            return Arrays.stream(e.getStackTrace())
                .filter(ste -> ste.getMethodName().equals("throwException"))
                .findFirst().get();
        }
        return null;
    }

    public static class Loader extends URLClassLoader {
        final String name;
        Loader(String name) throws MalformedURLException {
            super(name, new URL[] { testClassesURL() } , null);
            this.name = name;
        }

        private static URL testClassesURL() throws MalformedURLException {
            Path path = Paths.get(System.getProperty("test.classes"));
            return path.toUri().toURL();
        }

        public String getName() {
            return name + ".hacked";
        }
    }
}
