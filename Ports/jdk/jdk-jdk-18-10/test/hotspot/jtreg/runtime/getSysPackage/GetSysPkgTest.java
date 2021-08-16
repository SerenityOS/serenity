/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.loader
 *          java.logging
 * @requires vm.flagless
 * @library /test/lib
 * @run driver GetSysPkgTest
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

// Test that JVM get_system_package() returns the module location for defined packages.
public class GetSysPkgTest {

    private static Object invoke(Method m, Object obj, Object... args) throws Throwable {
        try {
            return m.invoke(obj, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Method findMethod(String name) {
        for (Method m : jdk.internal.loader.BootLoader.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new RuntimeException("Failed to find method " + name + " in java.lang.Module");
    }

    // Throw RuntimeException if getSystemPackageLocation() does not return
    // the expected location.
    static void getPkg(String name, String expected_loc) throws Throwable {
        String loc = (String)invoke(findMethod("getSystemPackageLocation"), null, name);
        if (loc == null) {
            if (expected_loc == null) return;
            System.out.println("Expected location: " + expected_loc +
                ", for package: " + name + ", got: null");
        } else if (expected_loc == null) {
            System.out.println("Expected location: null, for package: " +
                name + ", got: " + loc);
        } else if (!loc.equals(expected_loc)) {
            System.out.println("Expected location: " +
                expected_loc + ", for package: " + name + ", got: " + loc);
        } else {
            return;
        }
        throw new RuntimeException();
    }

    public static void main(String args[]) throws Throwable {
        if (args.length == 0 || !args[0].equals("do_tests")) {

            // Create a package found via -Xbootclasspath/a
            String source = "package BootLdr_package; " +
                            "public class BootLdrPkg { " +
                            "    public int mth() { return 4; } " +
                            "}";
            byte[] klassbuf =
                InMemoryJavaCompiler.compile("BootLdr_package.BootLdrPkg", source);
            ClassFileInstaller.writeClassToDisk("BootLdr_package/BootLdrPkg", klassbuf, "bl_dir");

            // Create a package found via -cp.
            source = "package GetSysPkg_package; " +
                     "public class GetSysClass { " +
                     "    public int mth() { return 4; } " +
                     "}";
            klassbuf =
                InMemoryJavaCompiler.compile("GetSysPkg_package.GetSysClass", source);
            ClassFileInstaller.writeClassToDisk("GetSysPkg_package/GetSysClass", klassbuf);

            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xbootclasspath/a:bl_dir",
                "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED", "-cp", "." + File.pathSeparator +
                System.getProperty("test.classes"), "GetSysPkgTest", "do_tests");
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
            return;
        }

        getPkg("java/lang", "jrt:/java.base");
        getPkg("javax/script", null);          // Package not defined

        // Test a package that does not yet have any referenced classes.
        // Note: if another class in com/sun/crypto/provider/ happens to get
        //       loaded or if class PrivateKeyInfo disappears from this package
        //       then this test will fail.
        getPkg("com/sun/crypto/provider", null);
        // Now make sure a class in the package is referenced.
        Class newClass = Class.forName("com.sun.crypto.provider.PrivateKeyInfo");
        getPkg("com/sun/crypto/provider", "jrt:/java.base");

        getPkg("java/nio/charset", "jrt:/java.base");

        // Test a package in a module not owned by boot loader.
        Class clss = Class.forName("jdk.nio.zipfs.ZipPath");
        if (clss == null)
            throw new RuntimeException("Could not find class jdk.nio.zipfs.ZipPath");

        // Test a package not in jimage file.
        clss = Class.forName("GetSysPkg_package.GetSysClass");
        if (clss == null)
            throw new RuntimeException("Could not find class GetSysPkg_package.GetSysClass");
        getPkg("GetSysPkg_package", null);

        // Access a class with a package in a boot loader module other than java.base
        clss = Class.forName("java.util.logging.Level");

        if (clss == null)
            throw new RuntimeException("Could not find class java.util.logging.Level");
        getPkg("java/util/logging", "jrt:/java.logging");

        // Test getting the package location from a class found via -Xbootclasspath/a
        clss = Class.forName("BootLdr_package.BootLdrPkg");
        if (clss == null)
            throw new RuntimeException("Could not find class BootLdr_package.BootLdrPkg");
        String bootldrPkg = (String)invoke(findMethod("getSystemPackageLocation"), null, "BootLdr_package");
        if (bootldrPkg == null) {
            throw new RuntimeException("Expected BootLdr_package to return non-null value");
        }
        if (!bootldrPkg.equals("bl_dir")) {
            throw new RuntimeException("Expected BootLdr_package to return bl_dir, got: " + bootldrPkg);
        }

        // Test when package's class reference is an array.
        // Note: if another class in javax/crypto happens to get loaded
        //       or if class AEADBadTagException disappears from this package
        //       then this test will fail.
        getPkg("javax/crypto", null);
        javax.crypto.AEADBadTagException[] blah = new javax.crypto.AEADBadTagException[3];
        getPkg("javax/crypto", "jrt:/java.base");

    }
}
