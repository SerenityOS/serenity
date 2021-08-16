/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7116676
 * @summary RichDiagnosticFormatter throws NPE when formatMessage is called directly
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper.Trusted;
import com.sun.tools.javac.api.DiagnosticFormatter;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T7116676 {

    public static void main(String[] args) throws Exception {
        T7116676 test = new T7116676();
        test.testThroughFormatterFormat();
    }

    static class JavaSource extends SimpleJavaFileObject {
        private String text = "package test;\n" +
                              "public class Test {\n" +
                              "   private void t(java.util.List<? extends String> l) {\n" +
                              "      t(java.util.Collections.singleton(l));\n" +
                              "}  }";

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    void assertEquals(String req, String found) {
        if (!found.equals(req)) {
            throw new AssertionError(String.format("Error. Found: \n\n%s ; Expected: \n\n%s", found, req));
        }
    }

    public void testThroughFormatterFormat() throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        DiagnosticChecker dc = new DiagnosticChecker("compiler.err.prob.found.req");
        JavacTask ct = (JavacTask)tool.getTask(null, null, dc, null, null, Arrays.asList(new JavaSource()));
        ct.analyze();
        DiagnosticFormatter<JCDiagnostic> formatter =
                Log.instance(((JavacTaskImpl) ct).getContext()).getDiagnosticFormatter();
        String msg = formatter.formatMessage(dc.diag, Locale.getDefault());
        //no redundant package qualifiers
        Assert.check(msg.indexOf("java.") == -1, msg);
    }

    @Trusted
    private static final class DiagnosticChecker implements DiagnosticListener<JavaFileObject> {

        String expectedKey;
        JCDiagnostic diag;

        DiagnosticChecker(String expectedKey) {
            this.expectedKey = expectedKey;
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            JCDiagnostic diag = (JCDiagnostic)diagnostic;
            if (diagnostic.getCode().equals(expectedKey)) {
                this.diag = diag;
            }
        }
    }
}
