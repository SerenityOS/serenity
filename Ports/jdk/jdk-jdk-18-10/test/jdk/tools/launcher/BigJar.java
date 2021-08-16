/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7194005
 * @summary launcher handling of zip64 archives (Scenario A and B)
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile  -XDignore.symbol.file BigJar.java
 * @run main/timeout=600 BigJar
 */
/*
 * This test consists of two scenarios:
 *
 * Scenario A: create a jar with entries exceeding 64K, add a main class and
 * see if the launcher can handle it.
 *
 * Scenario A1: create a jar as in A, but add a zipfile comment as well.
 *
 * Scenario B: create a jar with a large enough file exceeding 4GB, and
 * similarly test the launcher. This test can be run optionally by using the
 * following jtreg option:
 *  "-javaoptions:-DBigJar_testScenarioB=true"
 * or set
 *  "BigJar_testScenarioB" environment variable.
 *
 * Note this test will only run iff all the disk requirements are met at runtime.
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BigJar extends TestHelper {

    private static final long GIGA = 1024 * 1024 * 1024;
    private static final int BUFFER_LEN = Short.MAX_VALUE * 2;

    long getCount(long minlength) {
        return (minlength / BUFFER_LEN) + 1;
    }

    long computeCRC(long minlength) {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[BUFFER_LEN];
        long count = getCount(minlength);
        for (long i = 0; i < count; i++) {
            crc.update(buffer);
        }
        return crc.getValue();
    }

    long computeCRC(File inFile) throws IOException {
        byte[] buffer = new byte[8192];
        CRC32 crc = new CRC32();
        try (FileInputStream fis = new FileInputStream(inFile);
                BufferedInputStream bis = new BufferedInputStream(fis)) {
            int n = bis.read(buffer);
            while (n > 0) {
                crc.update(buffer, 0, n);
                n = bis.read(buffer);
            }
        }
        return crc.getValue();
    }

    void createLargeFile(OutputStream os, long minlength) throws IOException {
        byte[] buffer = new byte[BUFFER_LEN];
        long count = getCount(minlength);
        for (long i = 0; i < count; i++) {
            os.write(buffer);
        }
        os.flush();
    }

    Manifest createMainClass(File javaFile) throws IOException {
        javaFile.delete();
        List<String> content = new ArrayList<>();
        content.add("public class " + baseName(javaFile) + "{");
        content.add("public static void main(String... args) {");
        content.add("System.out.println(\"Hello World\\n\");");
        content.add("System.exit(0);");
        content.add("}");
        content.add("}");
        createFile(javaFile, content);
        compile(javaFile.getName());
        Manifest manifest = new Manifest();
        manifest.clear();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, baseName(javaFile));
        System.out.println(manifest.getMainAttributes().keySet());
        System.out.println(manifest.getMainAttributes().values());
        return manifest;
    }

    void createJarWithLargeFile(File jarFile, long minlength) throws IOException {
        File javaFile = new File("Foo.java");
        Manifest manifest = createMainClass(javaFile);
        File classFile = getClassFile(javaFile);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);
                BufferedOutputStream bos = new BufferedOutputStream(jos);
                FileInputStream fis = new FileInputStream(classFile);) {
            jos.setLevel(ZipOutputStream.STORED);
            jos.setMethod(0);

            JarEntry je = new JarEntry("large.data");
            je.setCompressedSize(getCount(minlength) * BUFFER_LEN);
            je.setSize(getCount(minlength) * BUFFER_LEN);
            je.setCrc(computeCRC(minlength));
            je.setMethod(ZipEntry.STORED);
            jos.putNextEntry(je);
            createLargeFile(bos, minlength);

            je = new JarEntry(classFile.getName());
            je.setCompressedSize(classFile.length());
            je.setSize(classFile.length());
            je.setCrc(computeCRC(classFile));
            je.setMethod(ZipEntry.STORED);
            jos.putNextEntry(je);
            copyStream(fis, bos);
            bos.flush();
            jos.closeEntry();
        }
    }

    void createLargeJar(File jarFile, String comment) throws IOException {
        final int MAX = Short.MAX_VALUE * 2 + 10;
        JarEntry je = null;
        File javaFile = new File("Foo.java");
        File classFile = getClassFile(javaFile);
        Manifest manifest = createMainClass(javaFile);
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest);
                FileInputStream fis = new FileInputStream(classFile)) {
            jos.setLevel(JarOutputStream.STORED);
            jos.setMethod(JarOutputStream.STORED);
            for (int i = 0; i < MAX; i++) {
                je = new JarEntry("X" + i + ".txt");
                je.setSize(0);
                je.setCompressedSize(0);
                je.setCrc(0);
                jos.putNextEntry(je);
            }

            // add a class file
            je = new JarEntry(classFile.getName());
            je.setCompressedSize(classFile.length());
            je.setSize(classFile.length());
            je.setCrc(computeCRC(classFile));
            jos.putNextEntry(je);
            copyStream(fis, jos);
            jos.closeEntry();
            if (comment != null) {
                jos.setComment(comment);
            }
        }
    }

    void testTheJar(File theJar) throws Exception {
        try {
            TestResult tr = doExec(javaCmd, "-jar", theJar.getName());
            tr.checkPositive();
            if (!tr.testStatus) {
                System.out.println(tr);
                throw new Exception("Failed");
            }
        } finally {
            theJar.delete();
        }
    }

    // a jar with entries exceeding 64k + a class file for the existential test
    @Test
    void testScenarioA() throws Exception {
        File largeJar = new File("large.jar");
        createLargeJar(largeJar, null);
        testTheJar(largeJar);
    }

     // a jar with entries exceeding 64k and zip comment
    @Test
    void testScenarioA1() throws Exception {
        File largeJar = new File("largewithcomment.jar");
        createLargeJar(largeJar, "A really large jar with a comment");
        testTheJar(largeJar);
    }

    // a jar with an enormous file + a class file for the existential test
    @Test
    void testScenarioB() throws Exception {
        final String testString = "BigJar_testScenarioB";
        if (Boolean.getBoolean(testString) == false &&
                System.getenv(testString) == null) {
            System.out.println("Warning: testScenarioB passes vacuously");
            return;
        }
        final File largeJar = new File("huge.jar");

        final Path path = largeJar.getAbsoluteFile().getParentFile().toPath();
        final long available = Files.getFileStore(path).getUsableSpace();
        final long MAX_VALUE = 0xFFFF_FFFFL;

        final long absolute = MAX_VALUE + 1L;
        final long required = (long) (absolute * 1.1); // pad for sundries
        System.out.println("\tavailable: " + available / GIGA + " GB");
        System.out.println("\trequired: " + required / GIGA + " GB");

        if (available > required) {
            createJarWithLargeFile(largeJar, absolute);
            testTheJar(largeJar);
        } else {
            System.out.println("Warning: testScenarioB passes vacuously,"
                    + " requirements exceeds available space");
        }
    }

    public static void main(String... args) throws Exception {
        BigJar bj = new BigJar();
        bj.run(args);
        if (testExitValue > 0) {
            System.out.println("Total of " + testExitValue + " failed");
            System.exit(1);
        } else {
            System.out.println("All tests pass");
        }
    }
}
