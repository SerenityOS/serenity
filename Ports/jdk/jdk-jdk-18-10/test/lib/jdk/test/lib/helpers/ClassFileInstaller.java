/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.helpers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dump a class file for a class on the class path in the current directory, or
 * in the specified JAR file. This class is usually used when you build a class
 * from a test library, but want to use this class in a sub-process.
 *
 * For example, to build the following library class:
 * test/lib/sun/hotspot/WhiteBox.java
 *
 * You would use the following tags:
 *
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 *
 * JTREG would build the class file under
 * ${JTWork}/classes/test/lib/sun/hotspot/WhiteBox.class
 *
 * With you run your main test class using "@run main MyMainClass", JTREG would setup the
 * -classpath to include "${JTWork}/classes/test/lib/", so MyMainClass would be able to
 * load the WhiteBox class.
 *
 * However, if you run a sub process, and do not wish to use the exact same -classpath,
 * You can use ClassFileInstaller to ensure that WhiteBox is available in the current
 * directory of your test:
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * Or, you can use the -jar option to store the class in the specified JAR file. If a relative
 * path name is given, the JAR file would be relative to the current directory of
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar myjar.jar sun.hotspot.WhiteBox
 */
public class ClassFileInstaller {
    /**
     * You can enable debug tracing of ClassFileInstaller by running JTREG with
     * jtreg -DClassFileInstaller.debug=true ... <names of tests>
     */
    public static boolean DEBUG = Boolean.getBoolean("ClassFileInstaller.debug");

    /**
     * @param args The names of the classes to dump
     * @throws Exception
     */
    public static void main(String... args) throws Exception {
        if (args.length > 1 && args[0].equals("-jar")) {
            if (args.length < 2) {
                throw new RuntimeException("Usage: jdk.test.lib.helpers.ClassFileInstaller <options> <classes>\n" +
                                           "where possible options include:\n" +
                                           "  -jar <path>             Write to the JAR file <path>");
            }
            String jarFile = args[1];
            String[] classes = addInnerClasses(args, 2);
            writeJar_impl(jarFile, null, classes);
        } else {
            if (DEBUG) {
                System.out.println("ClassFileInstaller: Writing to " + System.getProperty("user.dir"));
            }
            String[] classes = addInnerClasses(args, 0);
            for (String cls : classes) {
                writeClassToDisk(cls);
            }
        }
    }

    // Add commonly used inner classes that are often omitted by mistake. Currently
    // we support only jdk.test.whitebox.WhiteBox$WhiteBoxPermission and
    // sun/hotspot/WhiteBox$WhiteBoxPermission. See JDK-8199290
    private static String[] addInnerClasses(String[] classes, int startIdx) {
        boolean seenNewWb = false;
        boolean seenNewWbInner = false;
        boolean seenOldWb = false;
        boolean seenOldWbInner = false;
        final String newWb = "jdk.test.whitebox.WhiteBox";
        final String newWbInner = newWb + "$WhiteBoxPermission";
        final String oldWb = "sun.hotspot.WhiteBox";
        final String oldWbInner = oldWb + "$WhiteBoxPermission";

        ArrayList<String> list = new ArrayList<>();

        for (int i = startIdx; i < classes.length; i++) {
            String cls = classes[i];
            list.add(cls);
            switch (cls) {
            case newWb:      seenNewWb      = true; break;
            case newWbInner: seenNewWbInner = true; break;
            case oldWb:      seenOldWb      = true; break;
            case oldWbInner: seenOldWbInner = true; break;
            }
        }
        if (seenNewWb && !seenNewWbInner) {
            list.add(newWbInner);
        }
        if (seenOldWb && !seenOldWbInner) {
            list.add(oldWbInner);
        }

        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }

    public static class Manifest {
        private final InputStream in;

        private Manifest(InputStream in) {
            this.in = in;
        }

        public static Manifest fromSourceFile(String fileName) throws Exception {
            String pathName = System.getProperty("test.src") + File.separator + fileName;
            return new Manifest(new FileInputStream(pathName));
        }

        // Example:
        //  String manifest = "Premain-Class: RedefineClassHelper\n" +
        //                "Can-Redefine-Classes: true\n";
        //  ClassFileInstaller.writeJar("redefineagent.jar",
        //    ClassFileInstaller.Manifest.fromString(manifest),
        //    "RedefineClassHelper");
        public static Manifest fromString(String manifest) throws Exception {
            return new Manifest(new ByteArrayInputStream(manifest.getBytes()));
        }

        public InputStream getInputStream() {
            return in;
        }
    }

    private static void writeJar_impl(String jarFile, Manifest manifest, String classes[]) throws Exception {
        if (DEBUG) {
            System.out.println("ClassFileInstaller: Writing to " + getJarPath(jarFile));
        }

        (new File(jarFile)).delete();
        FileOutputStream fos = new FileOutputStream(jarFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        // The manifest must be the first or second entry. See comments in JarInputStream
        // constructor and JDK-5046178.
        if (manifest != null) {
            writeToDisk(zos, "META-INF/MANIFEST.MF", manifest.getInputStream());
        }

        for (String cls : classes) {
            writeClassToDisk(zos, cls);
        }

        zos.close();
        fos.close();
    }

    /*
     * You can call ClassFileInstaller.writeJar() from your main test class instead of
     * using "@run ClassFileInstaller -jar ...". E.g.,
     *
     * String jarPath = ClassFileInstaller.getJarPath("myjar.jar", "sun.hotspot.WhiteBox")
     *
     * If you call this API, make sure you build ClassFileInstaller with the following tags:
     *
     * @library /test/lib
     * @build jdk.test.lib.helpers.ClassFileInstaller
     */
    public static String writeJar(String jarFile, String... classes) throws Exception {
        classes = addInnerClasses(classes, 0);
        writeJar_impl(jarFile, null, classes);
        return getJarPath(jarFile);
    }

    public static String writeJar(String jarFile, Manifest manifest, String... classes) throws Exception {
        classes = addInnerClasses(classes, 0);
        writeJar_impl(jarFile, manifest, classes);
        return getJarPath(jarFile);
    }

    /**
     * This returns the absolute path to the file specified in "@ClassFileInstaller -jar myjar.jar",
     * In your test program, instead of using the JAR file name directly:
     *
     * String jarPath = "myjar.jar";
     *
     * you should call this function, like:
     *
     * String jarPath = ClassFileInstaller.getJarPath("myjar.jar")
     *
     * The reasons are:
     * (1) Using absolute path makes it easy to cut-and-paste from the JTR file and rerun your
     *     test in any directory.
     * (2) In the future, we may make the JAR file name unique to avoid clobbering
     *     during parallel JTREG execution.
     *
     */
    public static String getJarPath(String jarFileName) {
        return new File(jarFileName).getAbsolutePath();
    }

    public static void writeClassToDisk(String className) throws Exception {
        writeClassToDisk((ZipOutputStream)null, className);
    }
    private static void writeClassToDisk(ZipOutputStream zos, String className) throws Exception {
        writeClassToDisk(zos, className, "");
    }

    public static void writeClassToDisk(String className, String prependPath) throws Exception {
        writeClassToDisk(null, className, prependPath);
    }
    private static void writeClassToDisk(ZipOutputStream zos, String className, String prependPath) throws Exception {
        ClassLoader cl = ClassFileInstaller.class.getClassLoader();

        // Convert dotted class name to a path to a class file
        String pathName = className.replace('.', '/').concat(".class");
        InputStream is = cl.getResourceAsStream(pathName);
        if (is == null) {
            throw new RuntimeException("Failed to find " + pathName);
        }
        if (prependPath.length() > 0) {
            pathName = prependPath + "/" + pathName;
        }
        writeToDisk(zos, pathName, is);
    }

    public static void writeClassToDisk(String className, byte[] bytecode) throws Exception {
        writeClassToDisk(null, className, bytecode);
    }
    private static void writeClassToDisk(ZipOutputStream zos, String className, byte[] bytecode) throws Exception {
        writeClassToDisk(zos, className, bytecode, "");
    }

    public static void writeClassToDisk(String className, byte[] bytecode, String prependPath) throws Exception {
        writeClassToDisk(null, className, bytecode, prependPath);
    }
    private static void writeClassToDisk(ZipOutputStream zos, String className, byte[] bytecode, String prependPath) throws Exception {
        // Convert dotted class name to a path to a class file
        String pathName = className.replace('.', '/').concat(".class");
        if (prependPath.length() > 0) {
            pathName = prependPath + "/" + pathName;
        }
        writeToDisk(zos, pathName, new ByteArrayInputStream(bytecode));
    }

    private static void writeToDisk(ZipOutputStream zos, String pathName, InputStream is) throws Exception {
        if (DEBUG) {
            System.out.println("ClassFileInstaller: Writing " + pathName);
        }
        if (zos != null) {
            ZipEntry ze = new ZipEntry(pathName);
            zos.putNextEntry(ze);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf))>0){
                zos.write(buf, 0, len);
            }
        } else {
            // Create the class file's package directory
            Path p = Paths.get(pathName);
            if (pathName.contains("/")) {
                Files.createDirectories(p.getParent());
            }
            // Create the class file
            Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
        }
        is.close();
    }
}
