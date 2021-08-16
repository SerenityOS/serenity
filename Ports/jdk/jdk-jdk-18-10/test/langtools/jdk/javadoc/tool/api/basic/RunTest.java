/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6493690 8007490
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main RunTest
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

/**
 * Tests for DocumentationTool.run method.
 */
public class RunTest extends APITest {
    public static void main(String... args) throws Exception {
        new RunTest().run();
    }

    /**
     * Verify that run method can be invoked.
     */
//    @Test
    public void testRunOK() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File srcFile = new File(testSrc, "pkg/C.java");
        File outDir = getOutDir();
        String[] args = { "-d", outDir.getPath(), srcFile.getPath() };

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        int rc = tool.run(null, stdout, stderr, args);
        System.err.println("stdout >>" + stdout.toString() + "<<");
        System.err.println("stderr >>" + stderr.toString() + "<<");

        if (rc == 0) {
            System.err.println("call succeeded");
            checkFiles(outDir, standardExpectFiles);
            String out = stdout.toString();
            for (String f: standardExpectFiles) {
                String f1 = f.replace('/', File.separatorChar);
                if (f1.endsWith(".html") && !out.contains(f1))
                    error("expected string not found: " + f1);
            }
        } else {
            error("call failed");
        }
    }

    /**
     * Verify that run method can be invoked.
     */
    @Test
    public void testRunFail() throws Exception {
        File outDir = getOutDir();
        String badfile = "badfile.java";
        String[] args = { "-d", outDir.getPath(), badfile };

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        int rc = tool.run(null, stdout, stderr, args);
        System.err.println("stdout >>" + stdout.toString() + "<<");
        System.err.println("stderr >>" + stderr.toString() + "<<");

        if (rc == 0) {
            error("call succeeded unexpectedly");
        } else {
            String err = stderr.toString();
            if (err.contains(badfile))
                System.err.println("call failed as expected");
            else
                error("expected diagnostic not found");
        }
    }

    /**
     * Verify that null args are accepted.
     */
//    @Test
    public void testNullArgs() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File srcFile = new File(testSrc, "pkg/C.java");
        File outDir = getOutDir();
        String[] args = { "-d", outDir.getPath(), srcFile.getPath() };

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream prevStdout = System.out;
        System.setOut(new PrintStream(stdout));

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream prevStderr = System.err;
        System.setErr(new PrintStream(stderr));

        int rc ;
        try {
            DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
            rc = tool.run(null, null, null, args);
        } finally {
            System.setOut(prevStdout);
            System.setErr(prevStderr);
        }

        System.err.println("stdout >>" + stdout.toString() + "<<");
        System.err.println("stderr >>" + stderr.toString() + "<<");

        if (rc == 0) {
            System.err.println("call succeeded");
            checkFiles(outDir, standardExpectFiles);
            String out = stdout.toString();
            for (String f: standardExpectFiles) {
                String f1 = f.replace('/', File.separatorChar);
                if (f1.endsWith(".html") && !out.contains(f1))
                    error("expected string not found: " + f1);
            }
        } else {
            error("call failed");
        }
    }
}

