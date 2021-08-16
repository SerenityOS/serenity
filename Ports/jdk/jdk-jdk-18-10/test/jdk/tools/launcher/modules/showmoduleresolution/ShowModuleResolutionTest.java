/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @requires !vm.graal.enabled
 * @modules jdk.jdeps jdk.zipfs
 * @library /test/lib
 * @build ShowModuleResolutionTest
 * @run testng ShowModuleResolutionTest
 * @summary Basic test for java --show-module-resolution
 */

import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ShowModuleResolutionTest {

    /**
     * Test that the resolution does not bind any services
     */
    private void expectJavaBase(String... args) throws Exception {
        int exitValue = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .stdoutShouldContain("root java.base")
                .stdoutShouldNotContain("java.base binds")
                .getExitValue();
        assertTrue(exitValue == 0);
    }

    /**
     * Test that the resolution binds services that resolves additional
     * modules
     */
    private void expectProviders(String... args) throws Exception {
        int exitValue = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .stdoutShouldContain("root java.base")
                .stdoutShouldContain("root java.compiler")
                .stdoutShouldContain("root jdk.compiler")
                .stdoutShouldContain("root java.compiler")
                .stdoutShouldContain("jdk.compiler requires java.compiler")
                .stdoutShouldContain("java.base binds jdk.compiler")
                .stdoutShouldContain("java.base binds jdk.jdeps")
                .stdoutShouldContain("java.base binds jdk.zipfs")
                .stdoutShouldContain("java.compiler binds jdk.compiler")
                .stdoutShouldContain("jdk.jdeps requires jdk.compiler")
                .getExitValue();
        assertTrue(exitValue == 0);
    }

    public void test() throws Exception {
        expectJavaBase("--show-module-resolution",
                       "--limit-modules", "java.base",
                       "-version");
        expectProviders("--show-module-resolution",
                        "--limit-modules", "java.base,jdk.jdeps,jdk.zipfs",
                        "-version");
    }
}
