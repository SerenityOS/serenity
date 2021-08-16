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
 * @bug 8141609 8154403
 * @summary Verify JDK 8 can use jrt-fs.jar to work with jrt file system.
 * @run main RemoteRuntimeImageTest
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
public class RemoteRuntimeImageTest {
    //the jrt-fs.jar shipped together with jdk
    private static final String JRTFS_JAR = "jrt-fs.jar";
    private static final String SRC_DIR = System.getProperty("test.src");
    private static final String CLASSES_DIR = "classes";
    private static final String TEST_JAVAHOME = System.getProperty("test.jdk");

    public static void main(String[] args) throws Exception {
        // By default, set to ${JT_JAVA}
        String jdk8Home = System.getenv("JDK8_HOME");
        if (jdk8Home == null || jdk8Home.isEmpty()) {
            System.err.println("Failed to locate JDK8 with system "
                + "environment variable 'JDK8_HOME'. Skip testing!");
            return;
        }

        Path jdk8Path = Paths.get(jdk8Home);
        if (!isJdk8(jdk8Path)) {
            System.err.println("This test is only for JDK 8. Skip testing");
            return;
        }

        String java = jdk8Path.resolve("bin/java").toAbsolutePath().toString();
        String javac = jdk8Path.resolve("bin/javac").toAbsolutePath().toString();
        Files.createDirectories(Paths.get(".", CLASSES_DIR));
        String jrtJar = Paths.get(TEST_JAVAHOME, "lib", JRTFS_JAR).toAbsolutePath().toString();

        // Compose command-lines for compiling and executing tests
        List<List<String>> cmds = Arrays.asList(
                // Commands to compile test classes
                Arrays.asList(javac, "-d", CLASSES_DIR, "-cp", jrtJar,
                        SRC_DIR + File.separatorChar + "Main.java"),
                // Run test
                Arrays.asList(java, "-cp", CLASSES_DIR, "Main", TEST_JAVAHOME),
                // Run test with jrtfs.jar in class path,
                // which means to install jrt FileSystem provider
                Arrays.asList(java, "-cp", CLASSES_DIR + File.pathSeparatorChar + jrtJar,
                        "Main", TEST_JAVAHOME, "installed")
                );

        cmds.forEach(cmd -> execCmd(cmd));
    }

    private static void execCmd(List<String> command){
        System.out.println();
        System.out.println("Executing command: " + command);
        Process p = null;
        try {
            p = new ProcessBuilder(command).inheritIO().start();
            p.waitFor();
            int rc = p.exitValue();
            if (rc != 0) {
                throw new RuntimeException("Unexpected exit code:" + rc);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (p != null && p.isAlive()){
                p.destroy();
            }
        }
    }

    private static boolean isJdk8(Path jdk8HomePath) throws IOException {
        File releaseFile = jdk8HomePath.resolve("release").toFile();
        if (!releaseFile.exists()) {
            throw new RuntimeException(releaseFile.getPath() +
                    " doesn't exist");
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(releaseFile)) {
            props.load(in);
        }

        String version = props.getProperty("JAVA_VERSION", "");
        System.out.println("JAVA_VERSION is " + version);
        return version.startsWith("\"1.8");
    }
}
