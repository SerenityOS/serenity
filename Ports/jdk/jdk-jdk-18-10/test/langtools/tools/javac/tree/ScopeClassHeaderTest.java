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
 * @bug     8186694
 * @summary Verify that taking a Scope inside a class header
 *          does not taint internal structures
 * @modules jdk.compiler
 * @run main ScopeClassHeaderTest
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.IdentifierTree;

public class ScopeClassHeaderTest {

    public static void main(String... args) throws Exception {
        verifyScopeForClassHeader();
    }

    private static void verifyScopeForClassHeader() throws Exception {
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavaFileObject source = new SimpleJavaFileObject(URI.create("mem://Test.java"), Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return "import java.util.*; class O { public void m() { class X<T extends ArrayList> { public void test() { String o; } } } }";
            }
            @Override public boolean isNameCompatible(String simpleName, Kind kind) {
                return !"module-info".equals(simpleName);
            }
        };
        Iterable<? extends JavaFileObject> fos = Collections.singletonList(source);
        JavacTask task = (JavacTask) tool.getTask(null, null, null, new ArrayList<String>(), null, fos);
        final Trees trees = Trees.instance(task);
        CompilationUnitTree cu = task.parse().iterator().next();

        task.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().contentEquals("ArrayList") || node.getName().contentEquals("String")) {
                    Scope scope = trees.getScope(getCurrentPath());
                    System.err.println("scope: " + scope);
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(cu, null);
    }
}
