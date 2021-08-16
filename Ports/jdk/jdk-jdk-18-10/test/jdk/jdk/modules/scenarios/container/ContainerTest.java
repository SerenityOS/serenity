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

/**
 * @test
 * @library /test/lib
 * @modules jdk.jartool/sun.tools.jar
 *          jdk.compiler
 *          jdk.zipfs
 *          java.se
 * @build ContainerTest jdk.test.lib.compiler.CompilerUtils
 * @run testng ContainerTest
 * @summary Starts a simple container that uses dynamic configurations
 *          and launches two applications in the same VM
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ContainerTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final Path MLIB_DIR = Paths.get("mlib");
    private static final Path APPLIB_DIR = Paths.get("applib");

    private static final String CONTAINER_MODULE = "container";
    private static final String CONTAINER_MAIN_CLASS = "container.Main";


    /**
     * Creates the container module in mlib/container@1.0.jmod
     */
    void buildContainer() throws Exception {

        Path src = SRC_DIR.resolve(CONTAINER_MODULE);
        Path output = MODS_DIR.resolve(CONTAINER_MODULE);

        boolean compiled = CompilerUtils.compile(src, output);
        assertTrue(compiled);

        // jar --create ...
        Path mlib = Files.createDirectories(MLIB_DIR);
        String classes = output.toString();
        String jar = mlib.resolve(CONTAINER_MODULE + "@1.0.jar").toString();
        String[] args = {
            "--create",
            "--file=" + jar,
            "--main-class=" + CONTAINER_MAIN_CLASS,
            "-C", classes, "."
        };
        boolean success
            = new sun.tools.jar.Main(System.out, System.out, "jar")
                .run(args);
        assertTrue(success);
    }

    /**
     * Creates app1 and its bundled libraries in applib.
     */
    void buildApp1() throws Exception {
        Path dir = Files.createDirectories(APPLIB_DIR);

        // app1 uses its own copy of JAX-WS
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve("java.xml.ws"),
                                    dir.resolve("java.xml.ws"));
        assertTrue(compiled);

        compiled = CompilerUtils.compile(SRC_DIR.resolve("app1"),
                                         dir.resolve("app1"),
                                         "--upgrade-module-path", dir.toString());
        assertTrue(compiled);
    }

    /**
     * Creates app2 and its bundled libraries in applib.
     */
    void buildApp2() throws Exception {
        Path dir = Files.createDirectories(APPLIB_DIR);

        // app2 uses JAX-RS
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve("java.ws.rs"),
                                    dir.resolve("java.ws.rs"));
        assertTrue(compiled);

        compiled = CompilerUtils.compile(SRC_DIR.resolve("app2"),
                                         dir.resolve("app2"),
                                         "--module-path", dir.toString());
        assertTrue(compiled);
    }


    @BeforeTest
    public void setup() throws Exception {
        buildContainer();
        buildApp1();
        buildApp2();
    }

    /**
     * Launches the container
     */
    public void testContainer() throws Exception {

        int exitValue
            = executeTestJava("--module-path", MLIB_DIR.toString(),
                              "-m", CONTAINER_MODULE)
                .outputTo(System.out)
                .errorTo(System.err)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

}
