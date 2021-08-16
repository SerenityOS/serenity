/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6493690 8246774
 * @summary javadoc should have a javax.tools.Tool service provider
 * @modules java.compiler
 *          jdk.compiler
 * @build APITest
 * @run main GetTask_DiagListenerTest
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Tests for DocumentationTool.getTask  diagnosticListener  parameter.
 */
public class GetTask_DiagListenerTest extends APITest {
    public static void main(String... args) throws Exception {
        new GetTask_DiagListenerTest().run();
    }

    /**
     * Verify that a diagnostic listener can be specified.
     * Note that messages from the tool and doclet are imperfectly modeled
     * because the Reporter API works in terms of localized strings.
     * Therefore, messages reported via Reporter are simply wrapped and passed through.
     */
    @Test
    public void testDiagListener() throws Exception {
        JavaFileObject srcFile = createSimpleJavaFileObject("pkg/C", "package pkg; public error { }");
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File outDir = getOutDir();
            fm.setLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(outDir));
            Iterable<? extends JavaFileObject> files = Arrays.asList(srcFile);
            DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
            DocumentationTask t = tool.getTask(null, fm, dc, null, null, files);
            if (t.call()) {
                throw new Exception("task succeeded unexpectedly");
            } else {
                List<String> diagCodes = new ArrayList<String>();
                for (Diagnostic d: dc.getDiagnostics()) {
                    System.err.println("[" + d.getCode() + "]: " + d);
                    diagCodes.add(d.getCode());
                }
                List<String> expect = Arrays.asList(
                        "compiler.err.expected4",   // class, interface, enum, or record expected
                        "javadoc.note.message");    // 1 error
                if (!diagCodes.equals(expect))
                    throw new Exception("unexpected diagnostics occurred");
                System.err.println("diagnostics received as expected");
            }
        }
    }

}

