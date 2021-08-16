/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7086261
 * @summary javac doesn't report error as expected, it only reports ClientCodeWrapper$DiagnosticSourceUnwrapper
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 */

import javax.tools.*;

import com.sun.tools.javac.api.ClientCodeWrapper.DiagnosticSourceUnwrapper;
import com.sun.tools.javac.util.JCDiagnostic;

import java.net.URI;
import java.util.Arrays;

import static javax.tools.StandardLocation.*;
import static javax.tools.JavaFileObject.Kind.*;


public class T7086261 {

    static class ErroneousSource extends SimpleJavaFileObject {
        public ErroneousSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return "class Test { NonexistentClass c = null; }";
        }
    }

    static class DiagnosticChecker implements DiagnosticListener<javax.tools.JavaFileObject> {
        public void report(Diagnostic message) {
            if (!(message instanceof DiagnosticSourceUnwrapper)) {
                throw new AssertionError("Wrapped diagnostic expected!");
            }
            String actual = message.toString();
            JCDiagnostic jd = (JCDiagnostic)((DiagnosticSourceUnwrapper)message).d;
            String expected = jd.toString();
            if (!actual.equals(expected)) {
                throw new AssertionError("expected = " + expected + "\nfound = " + actual);
            }
        }
    };

    void test() throws Throwable {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        try (JavaFileManager jfm = javac.getStandardFileManager(null, null, null)) {
            JavaCompiler.CompilationTask task =
                    javac.getTask(null, jfm, new DiagnosticChecker(), null, null, Arrays.asList(new ErroneousSource()));
            task.call();
        }
    }

    public static void main(String[] args) throws Throwable {
        new T7086261().test();
    }
}
