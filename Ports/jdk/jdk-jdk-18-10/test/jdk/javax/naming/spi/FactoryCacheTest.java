/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8223260
 * @summary NamingManager should cache InitialContextFactory
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils jdk.test.lib.process.*
 *        FactoryCacheTest
 *        DummyContextFactory
 *        DummyContextFactory2
 * @run main FactoryCacheTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Arrays.asList;

import static jdk.test.lib.Utils.TEST_CLASSES;

public class FactoryCacheTest {

    private static final Path SPIJAR = Path.of("testDir", "ContextFactory.jar");
    private static final String SPIJAR_CP = SPIJAR.toAbsolutePath().toString();

    public static void main(String[] args) throws Throwable {
        List<String> argLine = new ArrayList<>();
        argLine.add(JDKToolFinder.getJDKTool("java"));
        argLine.addAll(asList(Utils.getTestJavaOpts()));
        argLine.addAll(List.of("-cp", TEST_CLASSES));
        argLine.addAll(List.of("-Durl.dir=" + TEST_CLASSES));
        argLine.add("DummyContextFactory");

        ProcessTools.executeCommand(argLine.stream()
                .filter(t -> !t.isEmpty())
                .toArray(String[]::new))
                .shouldHaveExitValue(0);

        // now test the ServiceLoader approach
        setupService();
        argLine = new ArrayList<>();
        argLine.add(JDKToolFinder.getJDKTool("java"));
        argLine.addAll(asList(Utils.getTestJavaOpts()));
        argLine.addAll(List.of("-cp", SPIJAR_CP));
        argLine.addAll(List.of("-Durl.dir=" + TEST_CLASSES));
        argLine.add("DummyContextFactory");

        ProcessTools.executeCommand(argLine.stream()
                .filter(t -> !t.isEmpty())
                .toArray(String[]::new))
                .shouldHaveExitValue(0);
    }

    private static void setupService() throws Exception {
        Path xdir = Path.of(TEST_CLASSES);
        Path config = xdir.resolve(Path.of(TEST_CLASSES,"META-INF/services/javax.naming.spi.InitialContextFactory"));
        Files.createDirectories(config.getParent());
        Files.write(config, "DummyContextFactory".getBytes(), CREATE);
        JarUtils.createJarFile(SPIJAR, xdir);
    }
}
