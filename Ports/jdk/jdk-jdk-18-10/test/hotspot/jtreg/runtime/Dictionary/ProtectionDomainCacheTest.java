/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8151486 8218266
 * @summary Call Class.forName() on the system classloader from a class loaded
 *          from a custom classloader, using the current class's protection domain.
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.util.JarUtils
 * @build ClassForName ProtectionDomainCacheTest
 * @run driver ProtectionDomainCacheTest
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import jdk.test.lib.Utils;
import jdk.test.lib.util.JarUtils;
import java.io.File;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * Create .jar, load ClassForName from .jar using a URLClassLoader
 */
public class ProtectionDomainCacheTest {
    static class Test {
        private static final long TIMEOUT = (long)(5000.0 * Utils.TIMEOUT_FACTOR);
        private static final String TESTCLASSES = System.getProperty("test.classes", ".");
        private static final String CLASSFILENAME = "ClassForName.class";

        // Use a new classloader to load the ClassForName class.
        public static void loadAndRun(Path jarFilePath)
                throws Exception {
            ClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarFilePath.toUri().toURL()}) {
                @Override public String toString() { return "LeakedClassLoader"; }
            };

            Class<?> loadClass = Class.forName("ClassForName", true, classLoader);
            loadClass.newInstance();

            System.out.println("returning : " + classLoader);
        }

        public static void main(final String[] args) throws Exception {
            // Create a temporary .jar file containing ClassForName.class
            Path testClassesDir = Paths.get(TESTCLASSES);
            Path jarFilePath = Files.createTempFile("cfn", ".jar");
            JarUtils.createJarFile(jarFilePath, testClassesDir, CLASSFILENAME);
            jarFilePath.toFile().deleteOnExit();

            // Remove the ClassForName.class file that jtreg built, to make sure
            // we're loading from the tmp .jar
            Path classFile = FileSystems.getDefault().getPath(TESTCLASSES,
                                                              CLASSFILENAME);
            Files.delete(classFile);

            for (int i = 0; i < 20; i++) {
                loadAndRun(jarFilePath);
            }

            // Give the GC a chance to unload protection domains
            for (int i = 0; i < 100; i++) {
                System.gc();
            }
            System.out.println("All Classloaders and protection domain cache entries successfully unloaded");
        }
    }

    public static void main(String args[]) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                                      "-Djava.security.policy==" + System.getProperty("test.src") + File.separator + "test.policy",
                                      "-Dtest.classes=" + System.getProperty("test.classes", "."),
                                      "-XX:+UnlockDiagnosticVMOptions",
                                      "-XX:VerifySubSet=dictionary",
                                      "-XX:+VerifyAfterGC",
                                      "-Xlog:gc+verify,protectiondomain=trace",
                                      "-Djava.security.manager",
                                      Test.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("PD in set is not alive");
        output.shouldContain("HandshakeForPD::do_thread");
        output.shouldHaveExitValue(0);
    }
}
