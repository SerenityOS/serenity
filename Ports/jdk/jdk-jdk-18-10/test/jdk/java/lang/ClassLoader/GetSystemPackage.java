/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8060130
 * @library /test/lib
 * @build package2.Class2 GetSystemPackage
 * @summary Test if getSystemPackage() return consistent values for cases
 *          where a manifest is provided or not and ensure only jars on
 *          bootclasspath gets resolved via Package.getSystemPackage
 * @run main GetSystemPackage
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import jdk.test.lib.process.ProcessTools;

public class GetSystemPackage {

    static final String testClassesDir = System.getProperty("test.classes", ".");
    static final File tmpFolder = new File(testClassesDir);
    static final String manifestTitle = "Special JAR";

    public static void main(String ... args) throws Exception {
        if (args.length == 0) {
            buildJarsAndInitiateSystemPackageTest();
            return;
        }
        switch (args[0]) {
            case "system-manifest":
                verifyPackage(true, true);
                break;
            case "system-no-manifest":
                verifyPackage(false, true);
                break;
            case "non-system-manifest":
                verifyPackage(true, false);
                break;
            case "non-system-no-manifest":
            default:
                verifyPackage(false, false);
                break;
        }
    }

    private static void buildJarsAndInitiateSystemPackageTest()
            throws Exception
    {
        Manifest m = new Manifest();
        // not setting MANIFEST_VERSION prevents META-INF/MANIFEST.MF from
        // getting written
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().put(Attributes.Name.SPECIFICATION_TITLE,
                                  manifestTitle);

        buildJar("manifest.jar", m);
        buildJar("no-manifest.jar", null);

        runSubProcess("System package with manifest improperly resolved.",
                "-Xbootclasspath/a:" + testClassesDir + "/manifest.jar",
                "GetSystemPackage", "system-manifest");

        runSubProcess("System package from directory improperly resolved.",
                "-Xbootclasspath/a:" + testClassesDir, "GetSystemPackage",
                "system-no-manifest");

        runSubProcess("System package with no manifest improperly resolved",
                "-Xbootclasspath/a:" + testClassesDir + "/no-manifest.jar",
                "GetSystemPackage", "system-no-manifest");

        runSubProcess("Classpath package with manifest improperly resolved",
                "-cp", testClassesDir + "/manifest.jar", "GetSystemPackage",
                "non-system-manifest");

        runSubProcess("Classpath package with no manifest improperly resolved",
                "-cp", testClassesDir + "/no-manifest.jar", "GetSystemPackage",
                "non-system-no-manifest");

    }

    private static void buildJar(String name, Manifest man) throws Exception {
        JarBuilder jar = new JarBuilder(tmpFolder, name, man);
        jar.addClassFile("package2/Class2.class",
                testClassesDir + "/package2/Class2.class");
        jar.addClassFile("GetSystemPackage.class",
                testClassesDir + "/GetSystemPackage.class");
        jar.build();
    }

    private static void runSubProcess(String messageOnError, String ... args)
            throws Exception
    {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
        int res = pb.directory(tmpFolder).inheritIO().start().waitFor();
        if (res != 0) {
            throw new RuntimeException(messageOnError);
        }
    }

    private static void verifyPackage(boolean hasManifest,
                                      boolean isSystemPackage)
            throws Exception
    {
        Class<?> c = Class.forName("package2.Class2");
        Package pkg = c.getPackage();
        if (pkg == null || pkg != Package.getPackage("package2") ||
                !"package2".equals(pkg.getName())) {
            fail("package2 not found via Package.getPackage()");
        }

        String specificationTitle = pkg.getSpecificationTitle();
        if (!"package2".equals(pkg.getName())) {
            fail("Invalid package for Class2");
        }

        if (hasManifest && (specificationTitle == null
                || !manifestTitle.equals(specificationTitle))) {
            fail("Invalid manifest for package " + pkg.getName());
        }
        if (!hasManifest && specificationTitle != null) {
            fail("Invalid manifest for package " + pkg.getName() + ": was " +
                    specificationTitle + " expected: null");
        }

        ClassLoader ld = c.getClassLoader();
        Package systemPkg = ld != null ? null : Package.getPackage("package2");

        if (findPackage("java.lang") == null) {
            fail("java.lang not found via Package.getPackages()");
        }
        Package foundPackage = findPackage("package2");
        if (isSystemPackage) {
            if (systemPkg == null) {
                fail("System package could not be found via getSystemPackage");
            }
            if (foundPackage != systemPkg || systemPkg != pkg) {
                fail("Inconsistent package found via Package.getPackages()");
            }
        } else {
            if (systemPkg != null) {
                fail("Non-system package could be found via getSystemPackage");
            }
            if (foundPackage == null) {
                fail("Non-system package not found via Package.getPackages()");
            }
        }
    }

    private static Package findPackage(String name) {
        Package[] packages = Package.getPackages();
        for (Package p : packages) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private static void fail(String message) {
        throw new RuntimeException(message);
    }
}

/*
 * Helper class for building jar files
 */
class JarBuilder {

    private JarOutputStream os;

    public JarBuilder(File tmpFolder, String jarName, Manifest manifest)
            throws FileNotFoundException, IOException
    {
        File jarFile = new File(tmpFolder, jarName);
        if (manifest != null) {
            this.os = new JarOutputStream(new FileOutputStream(jarFile),
                                          manifest);
        } else {
            this.os = new JarOutputStream(new FileOutputStream(jarFile));
        }
    }

    public void addClassFile(String pathFromRoot, String file)
            throws IOException
    {
        byte[] buf = new byte[1024];
        try (FileInputStream in = new FileInputStream(file)) {
            JarEntry entry = new JarEntry(pathFromRoot);
            os.putNextEntry(entry);
            int len;
            while ((len = in.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            os.closeEntry();
        }
    }

    public void build() throws IOException {
        os.close();
    }
}
