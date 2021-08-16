/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8197439
 * @summary Ensure that constructors don't cause crash in Attr.postAttr
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javax.tools.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.JCTree;

public class PostAttrConstructor {

    static class JavaSource extends SimpleJavaFileObject {

        final static String source =
                        "class C {\n" +
                        "    public C() {}\n" +
                        "}";

        JavaSource() {
            super(URI.create("myfo:/C.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    public static void main(String... args) throws IOException {
        new PostAttrConstructor().run();
    }

    void run() throws IOException {
        File destDir = new File("classes"); destDir.mkdir();
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavaSource source = new JavaSource();
        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null,
                Arrays.asList("-d", destDir.getPath()),
                null,
                Arrays.asList(source));
        CompilationUnitTree cut = ct.parse().iterator().next();
        Attr attr = Attr.instance(ct.getContext());
        attr.postAttr((JCTree) cut);
    }

}
