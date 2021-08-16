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
 * @bug 6493690
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main TagletPathTest
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Tests for locating a doclet via the file manager's DOCLET_PATH.
 */
public class TagletPathTest extends APITest {
    public static void main(String... args) throws Exception {
        new TagletPathTest().run();
    }

    /**
     * Verify that a taglet can be specified, and located via
     * the file manager's TAGLET_PATH.
     */
    @Test
    public void testTagletPath() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File tagletSrcFile = new File(testSrc, "taglets/UnderlineTaglet.java");
        File tagletDir = getOutDir("classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager cfm = compiler.getStandardFileManager(null, null, null)) {
            cfm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tagletDir));
            Iterable<? extends JavaFileObject> cfiles = cfm.getJavaFileObjects(tagletSrcFile);
            if (!compiler.getTask(null, cfm, null, null, null, cfiles).call())
                throw new Exception("cannot compile taglet");
        }

        JavaFileObject srcFile = createSimpleJavaFileObject("pkg/C", testSrcText);
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir("api");
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            fm.setLocation(DocumentationTool.Location.TAGLET_PATH, Arrays.asList(tagletDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
            Iterable<String> options = Arrays.asList("-taglet", "UnderlineTaglet");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            DocumentationTask t = tool.getTask(pw, fm, null, null, options, files);
            boolean ok = t.call();
            String out = sw.toString();
            System.err.println(">>" + out + "<<");
            if (ok) {
                File f = new File(outDir, "pkg/C.html");
                List<String> doc = Files.readAllLines(f.toPath(), Charset.defaultCharset());
                for (String line: doc) {
                    if (line.contains("<u>" + TEST_STRING + "</u>")) {
                        System.err.println("taglet executed as expected");
                        return;
                    }
                }
                error("expected text not found in output " + f);
            } else {
                error("task failed");
            }
        }
    }

    static final String TEST_STRING = "xyzzy";
    static final String testSrcText =
            "package pkg;\n" +
            "/** {@underline " + TEST_STRING + "} */\n" +
            "public class C { }";
}

