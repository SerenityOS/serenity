/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177471
 * @summary  jlink should use the version from java.base.jmod to find modules
 * @bug 8185130
 * @summary jlink should throw error if target image and current JDK versions don't match
 * @modules java.base/jdk.internal.module
 * @library /test/lib
 * @build jdk.test.lib.process.* CheckRuntimeVersion
 * @run testng/othervm JLinkMRJavaBaseVersionTest
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarFile;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.module.ModulePath;
import jdk.test.lib.process.ProcessTools;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JLinkMRJavaBaseVersionTest {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new RuntimeException("jar tool not found"));
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() -> new RuntimeException("javac tool not found"));
    private static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() -> new RuntimeException("jlink tool not found"));

    private static final Path javaHome = Paths.get(System.getProperty("java.home"));

    // resource text for version 9
    private byte[] resource9 = ("9 resource file").getBytes();
    // resource text for current version
    private byte[] resource = (Runtime.version().major() + " resource file").getBytes();

    static Path getJmods() {
        Path jmods = Paths.get(System.getProperty("java9.home", javaHome.toString())).resolve("jmods");
        if (Files.notExists(jmods)) {
            throw new RuntimeException(jmods + " not found");
        }
        return jmods;
    }

    @BeforeClass
    public void initialize() throws IOException {
        Path srcdir = Paths.get(System.getProperty("test.src"));

        // create class files from source
        Path base = srcdir.resolve("base");
        Path mr9 = Paths.get("mr9");
        javac(base, mr9, base.toString());

        // current version
        Path rt = srcdir.resolve("rt");
        Path rtmods = Paths.get("rtmods");
        javac(rt, rtmods, rt.toString());

        // build multi-release jar file with different module-info.class
        String[] args = {
                "-cf", "m1.jar",
                "-C", rtmods.resolve("m1").toString(), ".",
                "--release ", "9",
                "-C", mr9.resolve("m1").toString(), "."

        };
        JAR_TOOL.run(System.out, System.err, args);
    }

    private void javac(Path source, Path destination, String srcpath) throws IOException {
        String[] args = Stream.concat(
                Stream.of("-d", destination.toString(), "--release", "9",
                          "--module-source-path", srcpath),
                Files.walk(source)
                     .map(Path::toString)
                     .filter(s -> s.endsWith(".java"))
        ).toArray(String[]::new);
        int rc = JAVAC_TOOL.run(System.out, System.err, args);
        Assert.assertEquals(rc, 0);
    }

    @Test
    public void basicTest() throws Throwable {
        if (Files.notExists(javaHome.resolve("lib").resolve("modules"))) {
            // exploded image
            return;
        }

        Runtime.Version version = targetRuntimeVersion();
        System.out.println("Testing jlink with " + getJmods() + " of target version " + version);

        // use jlink to build image from multi-release jar
        if (jlink("m1.jar", "myimage")) {
            return;
        }

        // validate runtime image
        Path java = Paths.get("myimage", "bin", "java");
        ProcessTools.executeProcess(java.toString(), "-m", "m1/p.Main");

        // validate the image linked with the proper MR version

        if (!version.equalsIgnoreOptional(Runtime.version())) {
            ProcessTools.executeProcess(java.toString(), "-cp", System.getProperty("test.classes"),
                                        "CheckRuntimeVersion", String.valueOf(version.major()),
                                        "java.base", "m1")
                .shouldHaveExitValue(0);
        }
    }

    private Runtime.Version targetRuntimeVersion() {
        ModuleReference mref = ModulePath.of(Runtime.version(), true, getJmods())
            .find("java.base")
            .orElseThrow(() -> new RuntimeException("java.base not found from " + getJmods()));

        return Runtime.Version.parse(mref.descriptor().version().get().toString());
    }

    private boolean jlink(String jar, String image) {
        List<String> args = List.of("--output", image,
                                    "--add-modules", "m1",
                                    "--module-path",
                                    getJmods().toString() + File.pathSeparator + jar);
        System.out.println("jlink " + args.stream().collect(Collectors.joining(" ")));
        int exitCode = JLINK_TOOL.run(System.out, System.err, args.toArray(new String[0]));
        boolean isJDK9 = System.getProperty("java9.home") != null;
        if (isJDK9) {
            Assert.assertNotEquals(exitCode, 0);
        } else {
            Assert.assertEquals(exitCode, 0);
        }
        return isJDK9;
    }
}
