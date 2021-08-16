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

/*
 * @test
 * @summary Negative tests for jlink
 * @bug 8130861
 * @bug 8174718
 * @bug 8189671
 * @author Andrei Eremeev
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          java.base/jdk.internal.module
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run testng JLinkNegativeTest
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jdk.internal.module.ModuleInfoWriter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tests.Helper;
import tests.JImageGenerator;
import tests.JImageGenerator.InMemoryFile;
import tests.Result;

@Test
public class JLinkNegativeTest {

    private Helper helper;

    @BeforeClass
    public void setUp() throws IOException {
        helper = Helper.newHelper();
        if (helper == null) {
            throw new SkipException("Not run");
        }
        helper.generateDefaultModules();
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void testModuleNotExist() {
        helper.generateDefaultImage("failure1").assertFailure("Error: Module failure1 not found");
    }

    public void testNotExistInAddMods() {
        // cannot find jmod from --add-modules
        JImageGenerator.getJLinkTask()
                .modulePath(".")
                .addMods("not_exist")
                .output(helper.getImageDir().resolve("failure2"))
                .call().assertFailure("Error: Module not_exist not found");
    }

    public void test() throws IOException {
        helper.generateDefaultJModule("failure3");
        Path image = helper.generateDefaultImage("failure3").assertSuccess();
        JImageGenerator.getJLinkTask()
                .modulePath(helper.defaultModulePath())
                .output(image)
                .addMods("leaf1")
                .limitMods("leaf1")
                .call().assertFailure("Error: directory already exists: .*failure3.image(\n|\r|.)*");
    }

    public void testOutputIsFile() throws IOException {
        // output == file
        Path image = helper.createNewImageDir("failure4");
        Files.createFile(image);
        JImageGenerator.getJLinkTask()
                .modulePath(helper.defaultModulePath())
                .output(image)
                .addMods("leaf1")
                .call().assertFailure("Error: directory already exists: .*failure4.image(\n|\r|.)*");
        if (Files.notExists(image)) {
            throw new RuntimeException("output directory should not have been deleted");
        }
    }

    public void testModuleNotFound() {
        // limit module is not found
        Path imageFile = helper.createNewImageDir("test");
        JImageGenerator.getJLinkTask()
                .output(imageFile)
                .addMods("leaf1")
                .limitMods("leaf1")
                .limitMods("failure5")
                .modulePath(helper.defaultModulePath())
                .call().assertFailure("Error: Module failure5 not found");
    }

    public void testJmodIsDir() throws IOException {
        Path imageFile = helper.createNewImageDir("test");
        Path dirJmod = helper.createNewJmodFile("dir");
        Files.createDirectory(dirJmod);
        try {
            JImageGenerator.getJLinkTask()
                    .output(imageFile)
                    .addMods("dir")
                    .modulePath(helper.defaultModulePath())
                    .call().assertFailure("Error: Module dir not found");
        } finally {
            deleteDirectory(dirJmod);
        }
    }

    public void testJarIsDir() throws IOException {
        Path imageFile = helper.createNewImageDir("test");
        Path dirJar = helper.createNewJarFile("dir");
        Files.createDirectory(dirJar);
        try {
            JImageGenerator.getJLinkTask()
                    .output(imageFile)
                    .addMods("dir")
                    .modulePath(helper.defaultModulePath())
                    .call().assertFailure("Error: Module dir not found");
        } finally {
            deleteDirectory(dirJar);
        }
    }

    public void testMalformedJar() throws IOException {
        Path imageFile = helper.createNewImageDir("test");
        Path jar = helper.createNewJarFile("not_zip");
        Files.createFile(jar);
        try {
            JImageGenerator.getJLinkTask()
                    .output(imageFile)
                    .addMods("not_zip")
                    .modulePath(helper.defaultModulePath())
                    .call().assertFailure("Error: Error reading");
        } finally {
            deleteDirectory(jar);
        }
    }

    public void testMalformedJmod() throws IOException {
        Path imageFile = helper.createNewImageDir("test");
        Path jmod = helper.createNewJmodFile("not_zip");
        Files.createFile(jmod);
        try {
            JImageGenerator.getJLinkTask()
                    .output(imageFile)
                    .addMods("not_zip")
                    .modulePath(helper.defaultModulePath())
                    .call().assertFailure("Error: java.io.IOException: Invalid JMOD file");
        } finally {
            deleteDirectory(jmod);
        }
    }

    private static File createJarFile(File dir, String filename, String pkg, String name) throws IOException {
        File jarFile = new File(dir, filename + ".jar");

        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarFile))) {
            JarEntry entry = new JarEntry(filename + "/" + pkg + "/" + name);
            output.putNextEntry(entry);
        }

        return jarFile;
    }

    public void testAutomaticModuleAsRoot() throws IOException {
        Path imageFile = helper.createNewImageDir("test");
        String jarName = "myautomod";
        File jarFile = createJarFile(new File("jars"), jarName, "com/acme", "Bar.class");
        try {
            JImageGenerator.getJLinkTask()
                    .output(imageFile)
                    .addMods(jarName)
                    .modulePath(helper.defaultModulePath())
                    .call().assertFailure("Error: automatic module cannot be used with jlink: " + jarName);
        } finally {
            jarFile.delete();
        }
    }

    public void testAutomaticModuleAsDependency() throws IOException {
        Path imageFile = helper.createNewImageDir("test");
        String autoJarName = "myautomod";
        File autoJarFile = createJarFile(new File("jars"), autoJarName, "com/acme", "Bar.class");
        String rootMod = "autodepender";
        helper.generateDefaultJModule(rootMod, autoJarName).assertSuccess();
        try {
            JImageGenerator.getJLinkTask()
                    .output(imageFile)
                    .addMods(rootMod)
                    .modulePath(helper.defaultModulePath())
                    .call().assertFailure("Error: automatic module cannot be used with jlink: " + autoJarName);
        } finally {
            autoJarFile.delete();
        }
    }

    // Temporarily exclude; the jmod tool can no longer be used to create a jmod
    // with a class in the unnamed package. Find another way, or remove.
//    public void testAddDefaultPackage() throws IOException {
//        String moduleName = "hacked1";
//        Path module = helper.generateModuleCompiledClasses(helper.getJmodSrcDir(), helper.getJmodClassesDir(),
//                moduleName, Arrays.asList("hacked1.Main", "A", "B"), "leaf1");
//        JImageGenerator
//                .getJModTask()
//                .addClassPath(module)
//                .jmod(helper.getJmodDir().resolve(moduleName + ".jmod"))
//                .create().assertSuccess();
//        Path image = helper.generateDefaultImage(moduleName).assertSuccess();
//        helper.checkImage(image, moduleName, null, null);
//    }

    public void testAddSomeTopLevelFiles() throws IOException {
        String moduleName = "hacked2";
        Path module = helper.generateModuleCompiledClasses(helper.getJmodSrcDir(), helper.getJmodClassesDir(),
                moduleName);
        Files.createFile(module.resolve("top-level-file"));
        Path jmod = JImageGenerator
                .getJModTask()
                .addClassPath(module)
                .jmod(helper.getJmodDir().resolve(moduleName + ".jmod"))
                .create().assertSuccess();
        try {
            Path image = helper.generateDefaultImage(moduleName).assertSuccess();
            helper.checkImage(image, moduleName, null, null);
        } finally {
            deleteDirectory(jmod);
        }
    }

    public void testAddNonStandardSection() throws IOException {
        String moduleName = "hacked3";
        Path module = helper.generateDefaultJModule(moduleName).assertSuccess();
        JImageGenerator.addFiles(module, new InMemoryFile("unknown/A.class", new byte[0]));
        try {
            Result result = helper.generateDefaultImage(moduleName);
            System.err.println(result.getMessage());
            if (result.getExitCode() == 0) {
                throw new AssertionError("Crash expected");
            }
        } finally {
            deleteDirectory(module);
        }
    }

    @Test(enabled = true)
    public void testSectionsAreFiles() throws IOException {
        String moduleName = "hacked4";
        Path jmod = helper.generateDefaultJModule(moduleName).assertSuccess();
        JImageGenerator.addFiles(jmod,
                new InMemoryFile("/lib", new byte[0]),
                new InMemoryFile("/conf", new byte[0]),
                new InMemoryFile("/bin", new byte[0]));
        try {
            Result result = helper.generateDefaultImage(moduleName);
            System.err.println(result.getMessage());
            if (result.getExitCode() == 0) {
                throw new AssertionError("Crash expected");
            }
        } finally {
            deleteDirectory(jmod);
        }
    }

    public void testDuplicateModule1() throws IOException {
        String moduleName1 = "dupRes1Jmod1";
        String moduleName2 = "dupRes1Jmod2";
        List<String> classNames = Arrays.asList("java.A", "javax.B");
        Path module1 = helper.generateModuleCompiledClasses(
                helper.getJmodSrcDir(), helper.getJmodClassesDir(), moduleName1, classNames);
        Path module2 = helper.generateModuleCompiledClasses(
                helper.getJmodSrcDir(), helper.getJmodClassesDir(), moduleName2, classNames);

        try (OutputStream out = Files.newOutputStream(module2.resolve("module-info.class"))) {
            ModuleInfoWriter.write(ModuleDescriptor.newModule(moduleName1)
                    .requires("java.base").build(), out);
        }

        Path jmod1 = JImageGenerator.getJModTask()
                .addClassPath(module1)
                .jmod(helper.createNewJmodFile(moduleName1))
                .create()
                .assertSuccess();
        Path jmod2 = JImageGenerator.getJModTask()
                .addClassPath(module2)
                .jmod(helper.createNewJmodFile(moduleName2))
                .create()
                .assertSuccess();
        try {
            helper.generateDefaultImage(moduleName1)
                    .assertFailure("Error: Two versions of module dupRes1Jmod1 found in");
        } finally {
            deleteDirectory(jmod1);
            deleteDirectory(jmod2);
        }
    }

    public void testDuplicateModule2() throws IOException {
        String moduleName = "dupRes2Jmod";
        List<String> classNames = Arrays.asList("java.A", "javax.B");
        Path module1 = helper.generateModuleCompiledClasses(
                helper.getJmodSrcDir(), helper.getJmodClassesDir(), moduleName, classNames);
        Path module2 = helper.generateModuleCompiledClasses(
                helper.getJarSrcDir(), helper.getJarClassesDir(), moduleName, classNames);

        Path jmod = JImageGenerator.getJModTask()
                .addClassPath(module1)
                .jmod(helper.createNewJmodFile(moduleName))
                .create()
                .assertSuccess();
        Path jar = JImageGenerator.createJarFile(helper.getJarDir().resolve(moduleName + ".jar"), module2);
        Path newJar = helper.getJmodDir().resolve(jar.getFileName());
        Files.move(jar, newJar);
        try {
            helper.generateDefaultImage(moduleName)
                    .assertFailure("Error: Two versions of module dupRes2Jmod found in");
        } finally {
            deleteDirectory(jmod);
            deleteDirectory(newJar);
        }
    }

    public void testDuplicateModule3() throws IOException {
        String moduleName1 = "dupRes3Jar1";
        String moduleName2 = "dupRes3Jar2";
        List<String> classNames = Arrays.asList("java.A", "javax.B");
        Path module1 = helper.generateModuleCompiledClasses(
                helper.getJarSrcDir(), helper.getJarClassesDir(), moduleName1, classNames);
        Path module2 = helper.generateModuleCompiledClasses(
                helper.getJarSrcDir(), helper.getJarClassesDir(), moduleName2, classNames);

        try (OutputStream out = Files.newOutputStream(module2.resolve("module-info.class"))) {
            ModuleInfoWriter.write(ModuleDescriptor.newModule(moduleName1)
                    .requires("java.base").build(), out);
        }

        Path jar1 = JImageGenerator.createJarFile(helper.getJarDir().resolve(moduleName1 + ".jar"), module1);
        Path jar2 = JImageGenerator.createJarFile(helper.getJarDir().resolve(moduleName2 + ".jar"), module2);
        try {
            helper.generateDefaultImage(moduleName1)
                    .assertFailure("Error: Two versions of module dupRes3Jar1 found in");
        } finally {
            deleteDirectory(jar1);
            deleteDirectory(jar2);
        }
    }

    public void testInconsistentModuleInfo() throws IOException {
        String moduleName = "inconsistentJar";
        List<String> classNames = Arrays.asList("xorg.acme.internal.B");
        Path module = helper.generateModuleCompiledClasses(
                helper.getJarSrcDir(), helper.getJarClassesDir(), moduleName, classNames);

        try (OutputStream out = Files.newOutputStream(module.resolve("module-info.class"))) {
            ModuleInfoWriter.write(ModuleDescriptor.newModule(moduleName)
                    .requires("java.base")
                    .packages(Set.of("org.acme.internal"))
                    .build(), out);
        }

        Path jar = JImageGenerator.createJarFile(helper.getJarDir().resolve(moduleName + ".jar"), module);
        try {
            helper.generateDefaultImage(moduleName)
                  .assertFailure("Module inconsistentJar's descriptor indicates the set of packages is : " +
                  "[org.acme.internal], but module contains packages: [xorg.acme.internal]");
        } finally {
            deleteDirectory(jar);
        }
    }
}
