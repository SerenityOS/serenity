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
 * @bug 7142086
 * @summary performance problem in Check.checkOverrideClashes(...)
 * @modules jdk.compiler
 * @run main/timeout=10 T7142086
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class T7142086 {

    final static int N_METHODS = 1000;

    static class TestClass extends SimpleJavaFileObject {

        String methTemplate = "abstract void m(A#N p);";
        String classTemplate = "abstract class Test { #M }";

        String source;

        public TestClass() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            StringBuilder buf = new StringBuilder();
            for (int i = 0 ; i < N_METHODS ; i++) {
                buf.append(methTemplate.replace("#N", String.valueOf(i)));
                buf.append("\n");
            }
            source = classTemplate.replace("#M", buf.toString());
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    static class AnSource extends SimpleJavaFileObject {

        String classTemplate = "abstract class A#N { }";

        String source;

        public AnSource(int n) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = classTemplate.replace("#N", String.valueOf(n));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    public static void main(String... args) throws Exception {
        ArrayList<JavaFileObject> sources = new ArrayList<>();
        for (int i = 0 ; i < N_METHODS ; i++) {
            sources.add(new AnSource(i));
        }
        sources.add(new TestClass());
        new T7142086().run(sources);
    }

    void run(List<JavaFileObject> sources) throws Exception {
        DiagnosticChecker dc = new DiagnosticChecker();
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            JavacTask ct = (JavacTask)comp.getTask(null, fm, dc,
                    null, null, sources);
            ct.analyze();
        }
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                throw new AssertionError("unexpected diagnostic: " + diagnostic.getMessage(Locale.getDefault()));
            }
        }
    }
}
