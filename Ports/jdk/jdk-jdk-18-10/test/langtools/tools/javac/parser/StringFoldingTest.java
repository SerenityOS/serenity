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
 * @bug 7068902 8139751
 * @summary verify that string folding can be enabled or disabled
 * @modules jdk.compiler
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class StringFoldingTest {
    final JavaCompiler tool;
    final JavaSource source;

    public StringFoldingTest() {
        tool = ToolProvider.getSystemJavaCompiler();
        source = new JavaSource();
    }

    static class JavaSource extends SimpleJavaFileObject {

        final static String source =
                "class C {String X=\"F\" + \"O\" + \"L\" + \"D\" + \"E\" + \"D\";}";

        JavaSource() {
            super(URI.create("myfo:/C.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    public static void main(String... args) throws IOException {
        StringFoldingTest t = new StringFoldingTest();
        t.run(false);
        t.run(true);
    }

    void run(boolean disableStringFolding) throws IOException {
        List<String> argsList = new ArrayList<String>();
        if (disableStringFolding) {
            argsList.add("-XDallowStringFolding=false");
        }
        JavacTask ct = (JavacTask)tool.getTask(null, null, null,
                argsList,
                null,
                Arrays.asList(source));
        Iterable<? extends CompilationUnitTree> trees = ct.parse();
        String text = trees.toString();
        System.out.println(text);

        if (disableStringFolding) {
            if (text.contains("FOLDED")) {
                throw new AssertionError("Expected no string folding");
            }
            if (!text.contains("\"F\"")) {
                throw new AssertionError("Expected content not found");
            }
        } else {
            if (!text.contains("FOLDED")) {
                throw new AssertionError("Expected string folding");
            }
        }
    }
}
