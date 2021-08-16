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
 * @bug 8156499
 * @summary Test image creation from Multi-Release JAR
 * @author Steve Drach
 * @library /test/lib
 * @modules java.base/jdk.internal.jimage
 *          java.base/jdk.internal.module
 *          jdk.compiler
 *          jdk.jartool
 *          jdk.jlink
 *          jdk.zipfs
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run testng JLinkMultiReleaseJarTest
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.jimage.BasicImageReader;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JLinkMultiReleaseJarTest {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new RuntimeException("jar tool not found"));
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() -> new RuntimeException("javac tool not found"));
    private static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() -> new RuntimeException("jlink tool not found"));

    private final Path userdir = Paths.get(System.getProperty("user.dir", "."));
    private final Path javahome = Paths.get(System.getProperty("java.home"));
    private final Path jmodsdir = javahome.resolve("jmods");

    private final String pathsep = System.getProperty("path.separator");

    private byte[] resource = (Runtime.version().major() + " resource file").getBytes();

    @BeforeClass
    public void initialize() throws IOException {
        Path srcdir = Paths.get(System.getProperty("test.src"));

        // create class files from source
        Path base = srcdir.resolve("base");
        Path basemods = userdir.resolve("basemods");
        javac(base, basemods, base.toString());

        Path rt = srcdir.resolve("rt");
        Path rtmods = userdir.resolve("rtmods");
        javac(rt, rtmods, rt.toString());

        // create resources in basemods and rtmods
        Path dest = basemods.resolve("m1").resolve("resource.txt");
        byte[] text = "base resource file".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream(text);
        Files.copy(is, dest);

        dest = rtmods.resolve("m1").resolve("resource.txt");
        is = new ByteArrayInputStream(resource);
        Files.copy(is, dest);

        // build multi-release jar file with different module-infos
        String[] args = {
                "-cf", "m1.jar",
                "-C", basemods.resolve("m1").toString(), ".",
                "--release ", String.valueOf(JarFile.runtimeVersion().major()),
                "-C", rtmods.resolve("m1").toString(), "."
        };
        JAR_TOOL.run(System.out, System.err, args);

        // now move the module-info that requires logging to temporary place
        Files.move(rtmods.resolve("m1").resolve("module-info.class"),
                userdir.resolve("module-info.class"));

        // and build another jar
        args[1] = "m1-no-logging.jar";
        JAR_TOOL.run(System.out, System.err, args);

        // replace the no logging module-info with the logging module-info
        Files.move(userdir.resolve("module-info.class"),
                basemods.resolve("m1").resolve("module-info.class"),
                StandardCopyOption.REPLACE_EXISTING);

        // and build another jar
        args[1] = "m1-logging.jar";
        JAR_TOOL.run(System.out, System.err, args);
    }

    private void javac(Path source, Path destination, String srcpath) throws IOException {
        var args = Stream.of("-d", destination.toString(), "--module-source-path", srcpath);
        try (Stream<Path> pathStream = Files.walk(source)) {
            args = Stream.concat(args,
                    pathStream.map(Path::toString)
                              .filter(s -> s.endsWith(".java")));

            int rc = JAVAC_TOOL.run(System.out, System.err, args.toArray(String[]::new));
            Assert.assertEquals(rc, 0);
        }
    }

    @Test
    public void basicTest() throws Throwable {
        if (ignoreTest()) return;

        // use jlink to build image from multi-release jar
        jlink("m1.jar", "myimage");

        // validate image
        Path jimage = userdir.resolve("myimage").resolve("lib").resolve("modules");
        try (BasicImageReader reader = BasicImageReader.open(jimage)) {

            // do we have the right entry names?
            Set<String> names = Arrays.stream(reader.getEntryNames())
                    .filter(n -> n.startsWith("/m1"))
                    .collect(Collectors.toSet());
            Assert.assertEquals(names, Set.of(
                    "/m1/module-info.class",
                    "/m1/p/Main.class",
                    "/m1/p/Type.class",
                    "/m1/q/PublicClass.class",
                    "/m1/META-INF/MANIFEST.MF",
                    "/m1/resource.txt"));

            // do we have the right module-info.class?
            byte[] b = reader.getResource("/m1/module-info.class");
            Set<String> requires = ModuleDescriptor
                    .read(new ByteArrayInputStream(b))
                    .requires()
                    .stream()
                    .map(mdr -> mdr.name())
                    .filter(nm -> !nm.equals("java.base"))
                    .collect(Collectors.toSet());
            Assert.assertEquals(requires, Set.of("java.logging"));

            // do we have the right resource?
            b = reader.getResource("/m1/resource.txt");
            Assert.assertEquals(b, resource);

            // do we have the right class?
            b = reader.getResource("/m1/p/Main.class");
            Class<?> clazz = (new ByteArrayClassLoader()).loadClass("p.Main", b);
            MethodHandle getVersion = MethodHandles.lookup()
                    .findVirtual(clazz, "getVersion", MethodType.methodType(int.class));
            int version = (int) getVersion.invoke(clazz.getConstructor().newInstance());
            Assert.assertEquals(version, JarFile.runtimeVersion().major());
        }
    }

    @Test
    public void noLoggingTest() throws Throwable {
        if (ignoreTest()) return;

        jlink("m1-no-logging.jar", "no-logging-image");
        runImage("no-logging-image", false);
    }

    @Test
    public void loggingTest() throws Throwable {
        if (ignoreTest()) return;

        jlink("m1-logging.jar", "logging-image");
        runImage("logging-image", true);

    }

    // java.base.jmod must exist for this test to make sense
    private boolean ignoreTest() {
        if (Files.isRegularFile(jmodsdir.resolve("java.base.jmod"))) {
            return false;
        }
        System.err.println("Test skipped. NO jmods/java.base.jmod");
        return true;
    }


    private void jlink(String jar, String image) {
        String args = "--output " + image + " --add-modules m1 --module-path " +
                jar + pathsep + jmodsdir.toString();
        int exitCode = JLINK_TOOL.run(System.out, System.err, args.split(" +"));
        Assert.assertEquals(exitCode, 0);
    }

    public void runImage(String image, boolean expected) throws Throwable {
        Path java = Paths.get(image, "bin", "java");
        OutputAnalyzer oa = ProcessTools.executeProcess(java.toString(), "-m", "m1/p.Main");
        String sout = oa.getStdout();
        boolean actual = sout.contains("logging found");
        Assert.assertEquals(actual, expected);
        System.out.println(sout);
        System.err.println(oa.getStderr());
        Assert.assertEquals(oa.getExitValue(), 0);
    }

    private static class ByteArrayClassLoader extends ClassLoader {
        public Class<?> loadClass(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
