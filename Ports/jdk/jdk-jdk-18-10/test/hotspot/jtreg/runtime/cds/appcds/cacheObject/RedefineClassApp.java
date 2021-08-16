/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import sun.hotspot.WhiteBox;

public class RedefineClassApp {
    static WhiteBox wb = WhiteBox.getWhiteBox();

    public static interface Intf {            // Loaded from Boot class loader (-Xbootclasspath/a).
        public String get();
    }
    public static class Bar implements Intf { // Loaded from Boot class loader.
        public String get() {
            return "buzz";
        }
    }
    public static class Foo implements Intf { // Loaded from AppClassLoader
        public String get() {
            return "buzz";
        }
    }

    static int numTests = 0;
    static int failed = 0;
    static Instrumentation instrumentation;

    public static void main(String args[]) throws Throwable {
        if (wb.areSharedStringsIgnored()) {
          System.out.println("Shared strings are ignored.");
          return;
        }

        File bootJar = new File(args[0]);
        File appJar  = new File(args[1]);

        instrumentation = InstrumentationRegisterClassFileTransformer.getInstrumentation();
        System.out.println("INFO: instrumentation = " + instrumentation);

        testBootstrapCDS("Bootstrap Loader", bootJar);
        testAppCDSv1("Application Loader", appJar);

        if (failed > 0) {
            throw new RuntimeException("FINAL RESULT: " + failed + " out of " + numTests + " test case(s) have failed");
        } else {
            System.out.println("FINAL RESULT: All " + numTests + " test case(s) have passed!");
        }

        // Full GC. The cached objects in adjustable archive heap regions are
        // scanned. The archive regions are verified. No error should be
        // reported.
        wb.fullGC();
    }

    static void testBootstrapCDS(String group, File jar) throws Throwable {
        doTest(group, new Bar(), jar);
    }

    static void testAppCDSv1(String group, File jar) throws Throwable {
        doTest(group, new Foo(), jar);
    }

    static void checkArchivedMirrorObject(Class klass) {
        if (wb.areOpenArchiveHeapObjectsMapped()) {
            if (!wb.isShared(klass)) {
                failed ++;
                System.out.println("FAILED. " + klass + " mirror object is not archived");
                return;
            }
        }
    }

    static void doTest(String group, Intf object, File jar) throws Throwable {
        numTests ++;

        Class klass = object.getClass();
        System.out.println();
        System.out.println("++++++++++++++++++++++++++");
        System.out.println("Test group: " + group);
        System.out.println("Testing with classloader = " + klass.getClassLoader());
        System.out.println("Testing with class       = " + klass);
        System.out.println("Test is shared           = " + wb.isSharedClass(klass));
        System.out.println("++++++++++++++++++++++++++");

        // Check archived mirror object before redefine
        checkArchivedMirrorObject(klass);

        // Call get() before redefine. All strings in archived classes are shared.
        String res = object.get();
        System.out.println("get() returns " + res);
        if (res.equals("buzz") && wb.isShared(res)) {
            System.out.println("get() returns " + res + ", string is shared");
        } else {
            if (!res.equals("buzz")) {
                System.out.println("FAILED. buzz is expected but got " + res);
            } else {
                System.out.println("FAILED. " + res + " is not shared");
            }
            failed ++;
            return;
        }
        res = null; // release the local reference to the string

        // Run GC
        System.gc();
        System.gc();
        System.gc();

        // Redefine the shared class
        byte[] buff = Util.getClassFileFromJar(jar, klass.getName());
        Util.replace(buff, "buzz", "huzz");
        String f = "(failed)";
        try {
            instrumentation.redefineClasses(new ClassDefinition(klass, buff));
            f = object.get();
        } catch (UnmodifiableClassException|UnsupportedOperationException e) {
            e.printStackTrace();
        }
        if (f.equals("huzz")) {
            System.out.println("PASSED: object.get() after redefinition returns " + f);
        } else {
            System.out.println("FAILED: object.get() after redefinition returns " + f);
            failed ++;
        }

        // Run GC. Should not crash.
        System.gc();
        System.gc();
        System.gc();

        // Check archived mirror object after redefine and GC
        checkArchivedMirrorObject(klass);

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++ (done)\n\n");
    }
}
