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
 * @modules java.xml
 * @library /test/lib
 * @build DescribeModuleTest
 * @run testng DescribeModuleTest
 * @summary Basic test for java --describe-module
 */

import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class DescribeModuleTest {

    /**
     * Test that the output describes java.base
     */
    private void expectJavaBase(String... args) throws Exception {
        int exitValue = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .stdoutShouldContain("java.base")
                .stdoutShouldContain("exports java.lang")
                .stdoutShouldContain("uses java.nio.file.spi.FileSystemProvider")
                .stdoutShouldContain("contains sun.launcher")
                .stdoutShouldNotContain("requires ")
                .getExitValue();
        assertTrue(exitValue == 0);
    }

    /**
     * Test that the output describes java.xml
     */
    private void expectJavaXml(String... args) throws Exception {
        int exitValue = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .stdoutShouldContain("java.xml")
                .stdoutShouldContain("exports javax.xml")
                .stdoutShouldContain("requires java.base")
                .stdoutShouldContain("uses javax.xml.stream.XMLInputFactory")
                .getExitValue();
        assertTrue(exitValue == 0);
    }

    /**
     * Test output/exitValue when describing an unknown module
     */
    private void expectUnknownModule(String... args) throws Exception {
        int exitValue = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .stdoutShouldNotContain("requires java.base")
                .getExitValue();
        assertTrue(exitValue != 0);
    }


    public void testDescribeJavaBase() throws Exception {
        expectJavaBase("--describe-module", "java.base");
        expectJavaBase("--describe-module=java.base");
        expectJavaBase("-d", "java.base");
    }

    public void testDescribeJavaXml() throws Exception {
        expectJavaXml("--describe-module", "java.xml");
        expectJavaXml("--describe-module=java.xml");
        expectJavaXml("-d", "java.xml");
    }

    public void testDescribeUnknownModule() throws Exception {
        expectUnknownModule("--describe-module", "jdk.rhubarb");
        expectUnknownModule("--describe-module=jdk.rhubarb");
        expectUnknownModule("-d", "jdk.rhubarb");
    }

}
