/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.module.ModuleInfoWriter;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

/*
 * @test
 * @bug 8205654
 * @summary Unit test for sun.tools.ProcessHelper class. The test launches Java processes with different Java options
 * and checks that sun.tools.ProcessHelper.getMainClass(pid) method returns a correct main class.                                                                                                                               return a .
 *
 * @requires os.family == "linux"
 * @library /test/lib
 * @modules jdk.jcmd/sun.tools.common:+open
 *          java.base/jdk.internal.module
 * @build test.TestProcess
 * @run main/othervm TestProcessHelper
 */
public class TestProcessHelper {

    private static final String TEST_PROCESS_MAIN_CLASS_NAME = "TestProcess";
    private static final String TEST_PROCESS_MAIN_CLASS_PACKAGE = "test";
    private static final String TEST_PROCESS_MAIN_CLASS = TEST_PROCESS_MAIN_CLASS_PACKAGE + "."
            + TEST_PROCESS_MAIN_CLASS_NAME;
    private static final Path TEST_CLASSES = FileSystems.getDefault().getPath(System.getProperty("test.classes"));
    private static final Path USER_DIR = FileSystems.getDefault().getPath(System.getProperty("user.dir", "."));
    private static final Path TEST_MODULES = USER_DIR.resolve("testmodules");
    private static final String JAVA_PATH = JDKToolFinder.getJDKTool("java");
    private static final Path TEST_CLASS = TEST_CLASSES.resolve(TEST_PROCESS_MAIN_CLASS_PACKAGE)
            .resolve(TEST_PROCESS_MAIN_CLASS_NAME + ".class");

    private static final String[] CP_OPTIONS = {"-cp", "-classpath", "--class-path"};
    private static final String[][] VM_ARGS = {{}, {"-Dtest1=aaa"}, {"-Dtest1=aaa", "-Dtest2=bbb ccc"}};
    private static final String[][] ARGS = {{}, {"param1"}, {"param1", "param2"}};
    private static final String[] MP_OPTIONS = {"-p", "--module-path"};
    private static final String[] MODULE_OPTIONS = {"-m", "--module", "--module="};
    private static final String JAR_OPTION = "-jar";
    private static final String MODULE_NAME = "module1";
    private static final String[][] EXTRA_MODULAR_OPTIONS = {null,
            {"--add-opens", "java.base/java.net=ALL-UNNAMED"},
            {"--add-exports", "java.base/java.net=ALL-UNNAMED"},
            {"--add-reads", "java.base/java.net=ALL-UNNAMED"},
            {"--add-modules", "java.management"},
            {"--limit-modules", "java.management"},
            {"--upgrade-module-path", "test"}};

    private static final String[] PATCH_MODULE_OPTIONS = {"--patch-module", null};

    private static final MethodHandle MH_GET_MAIN_CLASS = resolveMainClassMH();

    private static MethodHandle resolveMainClassMH() {
        try {
            Method getMainClassMethod = Class
                .forName("sun.tools.common.ProcessHelper")
                .getDeclaredMethod("getMainClass", String.class);
            getMainClassMethod.setAccessible(true);
            return MethodHandles.lookup().unreflect(getMainClassMethod);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String callGetMainClass(Process p) {
        try {
            return (String)MH_GET_MAIN_CLASS.invoke(Long.toString(p.pid()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) throws Exception {
        new TestProcessHelper().runTests();
    }

    public void runTests() throws Exception {
        testClassPath();
        testJar();
        testModule();
    }

    // Test Java processes that are started with -classpath, -cp, or --class-path options
    // and with different combinations of VM and program args.
    private void testClassPath() throws Exception {
        for (String cp : CP_OPTIONS) {
            for (String[] vma : VM_ARGS) {
                for (String[] arg : ARGS) {
                    for (String[] modularOptions : EXTRA_MODULAR_OPTIONS) {
                        List<String> cmd = new LinkedList<>();
                        cmd.add(JAVA_PATH);
                        cmd.add(cp);
                        cmd.add(TEST_CLASSES.toAbsolutePath().toString());
                        for (String v : vma) {
                            cmd.add(v);
                        }
                        if (modularOptions != null) {
                            cmd.add(modularOptions[0]);
                            cmd.add(modularOptions[1]);
                        }
                        cmd.add(TEST_PROCESS_MAIN_CLASS);
                        for (String a : arg) {
                            cmd.add(a);
                        }
                        testProcessHelper(cmd, TEST_PROCESS_MAIN_CLASS);
                    }
                }
            }
        }
    }

    // Test Java processes that are started with -jar option
    // and with different combinations of VM and program args.
    private void testJar() throws Exception {
        File jarFile = prepareJar();
        for (String[] vma : VM_ARGS) {
            for (String[] arg : ARGS) {
                List<String> cmd = new LinkedList<>();
                cmd.add(JAVA_PATH);
                for (String v : vma) {
                    cmd.add(v);
                }
                cmd.add(JAR_OPTION);
                cmd.add(jarFile.getAbsolutePath());
                for (String a : arg) {
                    cmd.add(a);
                }
                testProcessHelper(cmd, jarFile.getAbsolutePath());
            }
        }

    }

    // Test Java processes that are started with -m or --module options
    // and with different combination of VM and program args.
    private void testModule() throws Exception {
        prepareModule();
        for (String mp : MP_OPTIONS) {
            for (String m : MODULE_OPTIONS) {
                for (String[] vma : VM_ARGS) {
                    for (String[] arg : ARGS) {
                        for(String patchModuleOption : PATCH_MODULE_OPTIONS) {
                            List<String> cmd = new LinkedList<>();
                            cmd.add(JAVA_PATH);
                            cmd.add(mp);
                            cmd.add(TEST_MODULES.toAbsolutePath().toString());
                            if (patchModuleOption != null) {
                                cmd.add(patchModuleOption);
                                cmd.add(MODULE_NAME + "=" + TEST_MODULES.toAbsolutePath().toString());
                            }
                            for (String v : vma) {
                                cmd.add(v);
                            }
                            if (m.endsWith("=")) {
                                cmd.add(m + MODULE_NAME + "/" + TEST_PROCESS_MAIN_CLASS);
                            } else {
                                cmd.add(m);
                                cmd.add(MODULE_NAME + "/" + TEST_PROCESS_MAIN_CLASS);
                            }
                            for (String a : arg) {
                                cmd.add(a);
                            }
                            testProcessHelper(cmd, MODULE_NAME + "/" + TEST_PROCESS_MAIN_CLASS);
                        }
                    }
                }
            }
        }
    }

    private void checkMainClass(Process p, String expectedMainClass) {
        String mainClass = callGetMainClass(p);
        // getMainClass() may return null, e.g. due to timing issues.
        // Attempt some limited retries.
        if (mainClass == null) {
            System.err.println("Main class returned by ProcessHelper was null.");
            // sleep time doubles each round, altogether, wait no longer than 1 sec
            final int MAX_RETRIES = 10;
            int retrycount = 0;
            long sleepms = 1;
            while (retrycount < MAX_RETRIES && mainClass == null) {
                System.err.println("Retry " + retrycount + ", sleeping for " + sleepms + "ms.");
                try {
                    Thread.sleep(sleepms);
                } catch (InterruptedException e) {
                    // ignore
                }
                mainClass = callGetMainClass(p);
                retrycount++;
                sleepms *= 2;
            }
        }
        p.destroyForcibly();
        if (!expectedMainClass.equals(mainClass)) {
            throw new RuntimeException("Main class is wrong: " + mainClass);
        }
    }

    private void testProcessHelper(List<String> args, String expectedValue) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        String cmd = pb.command().stream().collect(Collectors.joining(" "));
        System.out.println("Starting the process:" + cmd);
        Process p = ProcessTools.startProcess("test", pb);
        if (!p.isAlive()) {
            throw new RuntimeException("Cannot start the process: " + cmd);
        }
        checkMainClass(p, expectedValue);
    }

    private File prepareJar() throws Exception {
        Path jarFile = USER_DIR.resolve("testprocess.jar");
        Manifest manifest = createManifest();
        JarUtils.createJarFile(jarFile, manifest, TEST_CLASSES, TEST_CLASS);
        return jarFile.toFile();
    }

    private void prepareModule() throws Exception {
        TEST_MODULES.toFile().mkdirs();
        Path moduleJar = TEST_MODULES.resolve("mod1.jar");
        ModuleDescriptor md = createModuleDescriptor();
        createModuleJarFile(moduleJar, md, TEST_CLASSES, TEST_CLASS);
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, TEST_PROCESS_MAIN_CLASS);
        return manifest;
    }

    private ModuleDescriptor createModuleDescriptor() {
        ModuleDescriptor.Builder builder
                = ModuleDescriptor.newModule(MODULE_NAME).requires("java.base");
        return builder.build();
    }

    private static void createModuleJarFile(Path jarfile, ModuleDescriptor md, Path dir, Path... files)
            throws IOException {

        Path parent = jarfile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<Path> entries = findAllRegularFiles(dir, files);

        try (OutputStream out = Files.newOutputStream(jarfile);
             JarOutputStream jos = new JarOutputStream(out)) {
            if (md != null) {
                JarEntry je = new JarEntry("module-info.class");
                jos.putNextEntry(je);
                ModuleInfoWriter.write(md, jos);
                jos.closeEntry();
            }

            for (Path entry : entries) {
                String name = toJarEntryName(entry);
                jos.putNextEntry(new JarEntry(name));
                Files.copy(dir.resolve(entry), jos);
                jos.closeEntry();
            }
        }
    }

    private static String toJarEntryName(Path file) {
        Path normalized = file.normalize();
        return normalized.subpath(0, normalized.getNameCount())
                .toString()
                .replace(File.separatorChar, '/');
    }

    private static List<Path> findAllRegularFiles(Path dir, Path[] files) throws IOException {
        List<Path> entries = new ArrayList<>();
        for (Path file : files) {
            try (Stream<Path> stream = Files.find(dir.resolve(file), Integer.MAX_VALUE,
                    (p, attrs) -> attrs.isRegularFile())) {
                stream.map(dir::relativize)
                        .forEach(entries::add);
            }
        }
        return entries;
    }

}
