/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.io.FileWriter;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import sun.hotspot.WhiteBox;

public class InstrumentationApp {
    static WhiteBox wb = WhiteBox.getWhiteBox();

    public static final String COO_CLASS_NAME = "InstrumentationApp$Coo";

    public static interface Intf {            // Loaded from Boot class loader (-Xbootclasspath/a).
        public String get();
    }
    public static class Bar implements Intf { // Loaded from Boot class loader.
        public String get() {
            // The initial transform:
            //      change "buzz" -> "fuzz"
            // The re-transform:
            //      change "buzz" -> "guzz"
            return "buzz";
        }
    }
    public static class Foo implements Intf { // Loaded from AppClassLoader, or from a custom loader
        public String get() {
            // The initial transform:
            //      change "buzz" -> "fuzz"
            // The re-transform:
            //      change "buzz" -> "guzz"
            return "buzz";
        }
    }
    public static class Coo implements Intf { // Loaded from custom class loader.
        public String get() {
            // The initial transform:
            //      change "buzz" -> "fuzz"
            // The re-transform:
            //      change "buzz" -> "guzz"
            return "buzz";
        }
    }

    // This class file should be archived if AppCDSv2 is enabled on this platform. See
    // the comments around the call to TestCommon.dump in InstrumentationTest.java.
    public static class ArchivedIfAppCDSv2Enabled {}

    public static boolean isAppCDSV2Enabled() {
        return wb.isSharedClass(ArchivedIfAppCDSv2Enabled.class);
    }

    public static class MyLoader extends URLClassLoader {
        public MyLoader(URL[] urls, ClassLoader parent, File jar) {
            super(urls, parent);
            this.jar = jar;
        }
        File jar;

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> clz = findLoadedClass(name);
                if (clz != null) {
                    return clz;
                }

                if (name.equals(COO_CLASS_NAME)) {
                    try {
                        byte[] buff = Util.getClassFileFromJar(jar, name);
                        return defineClass(name, buff, 0, buff.length);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new RuntimeException("Unexpected", t);
                    }
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    static int numTests = 0;
    static int failed = 0;
    static boolean isAttachingAgent = false;
    static Instrumentation instrumentation;

    public static void main(String args[]) throws Throwable {
        System.out.println("INFO: AppCDSv1 " + (wb.isSharedClass(InstrumentationApp.class) ? "enabled" :"disabled"));
        System.out.println("INFO: AppCDSv2 " + (isAppCDSV2Enabled()                        ? "enabled" : "disabled"));

        String flagFile = args[0];
        File bootJar = new File(args[1]);
        File appJar  = new File(args[2]);
        File custJar = new File(args[3]);
        waitAttach(flagFile);

        instrumentation = InstrumentationRegisterClassFileTransformer.getInstrumentation();
        System.out.println("INFO: instrumentation = " + instrumentation);

        testBootstrapCDS("Bootstrap Loader", bootJar);
        testAppCDSv1("Application Loader", appJar);

        if (isAppCDSV2Enabled()) {
          testAppCDSv2("Custom Loader (unregistered)", custJar);
        }

        if (failed > 0) {
            throw new RuntimeException("FINAL RESULT: " + failed + " out of " + numTests + " test case(s) have failed");
        } else {
            System.out.println("FINAL RESULT: All " + numTests + " test case(s) have passed!");
        }
    }

    static void waitAttach(String flagFile) throws Throwable {
        // See InstrumentationTest.java for the hand-shake protocol.
        if (!flagFile.equals("noattach")) {
            File f = new File(flagFile);
            try (FileWriter fw = new FileWriter(f)) {
                long pid = ProcessHandle.current().pid();
                System.out.println("my pid = " + pid);
                fw.write(Long.toString(pid));
                fw.write("\n");
                for (int i=0; i<10; i++) {
                  // Parent process waits until we have written more than 100 bytes, so it won't
                  // read a partial pid
                  fw.write("==========");
                }
                fw.close();
            }

            long start = System.currentTimeMillis();
            while (f.exists()) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println(".... (" + elapsed + ") waiting for deletion of " + f);
                Thread.sleep(1000);
            }
            System.out.println("Attach succeeded (child)");
            isAttachingAgent = true;
        }
    }

    static void testBootstrapCDS(String group, File jar) throws Throwable {
        doTest(group, new Bar(), jar);
    }

    static void testAppCDSv1(String group, File jar) throws Throwable {
        doTest(group, new Foo(), jar);
    }

    static void testAppCDSv2(String group, File jar) throws Throwable {
        URL[] urls = new URL[] {jar.toURI().toURL()};
        MyLoader loader = new MyLoader(urls, InstrumentationApp.class.getClassLoader(), jar);
        Class klass = loader.loadClass(COO_CLASS_NAME);
        doTest(group, (Intf)klass.newInstance(), jar);
    }

    static void doTest(String group, Intf object, File jar) throws Throwable {
        Class klass = object.getClass();
        System.out.println();
        System.out.println("++++++++++++++++++++++++++");
        System.out.println("Test group: " + group);
        System.out.println("Testing with classloader = " + klass.getClassLoader());
        System.out.println("Testing with class       = " + klass);
        System.out.println("++++++++++++++++++++++++++");

        // Initial transform
        String f = object.get();
        assertTrue(f.equals("fuzz"), "object.get(): Initial transform should give 'fuzz'", f);

        // Retransform
        f = "(failed)";
        try {
            instrumentation.retransformClasses(klass);
            f = object.get();
        } catch (UnmodifiableClassException|UnsupportedOperationException e) {
            e.printStackTrace();
        }
        assertTrue(f.equals("guzz"), "object.get(): retransformation should give 'guzz'", f);

        // Redefine
        byte[] buff = Util.getClassFileFromJar(jar, klass.getName());
        Util.replace(buff, "buzz", "huzz");
        f = "(failed)";
        try {
            instrumentation.redefineClasses(new ClassDefinition(klass, buff));
            f = object.get();
        } catch (UnmodifiableClassException|UnsupportedOperationException e) {
            e.printStackTrace();
        }
        assertTrue(f.equals("quzz"), "object.get(): redefinition should give 'quzz'", f);

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++ (done)\n\n");
    }

    private static void assertTrue(boolean expr, String msg, String string) {
        numTests ++;
        System.out.printf("Test case %2d ", numTests);

        if (expr) {
            System.out.println("PASSED: " + msg + " and we got '" + string + "'");
        } else {
            failed ++;
            System.out.println("FAILED: " + msg + " but we got '" + string + "'");
        }
    }
}
