/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6972327
 * @summary JCTree.pos incorrect for annotations without modifiers and package
 * @modules jdk.compiler/com.sun.tools.javac.api
 */

import com.sun.source.tree.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.*;
import java.net.URI;
import java.util.Arrays;
import javax.tools.*;

public class T6972327 {

    public static void main(String[] args) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "\n@interface Test {}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null,
            Arrays.asList("-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        Trees t = Trees.instance(ct);

        long pos = t.getSourcePositions().getStartPosition(cut, clazz);
        if (pos != code.indexOf(code.trim()))
            throw new IllegalStateException("Unexpected position=" + pos);
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
