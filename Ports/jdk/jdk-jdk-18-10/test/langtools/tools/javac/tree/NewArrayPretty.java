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
 * @bug 8014826
 * @summary test Pretty print of NewArray
 * @author ksrini
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.tree
 */
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.tree.JCTree;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class NewArrayPretty {
    private final JavaCompiler tool;
    NewArrayPretty() {
        tool = ToolProvider.getSystemJavaCompiler();
    }
    public static void main(String... args) throws Exception {
        NewArrayPretty nap = new NewArrayPretty();
        nap.run("Class[] cls = null");
        nap.run("Class[] cls = new Class[]{Object.class}");
    }
    void run(String code) throws IOException {
        String src = "public class Test {" + code + ";}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, null,
                null, Arrays.asList(new MyFileObject(src)));

        for (CompilationUnitTree cut : ct.parse()) {
            JCTree.JCVariableDecl var =
                    (JCTree.JCVariableDecl) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(0);

            if (!code.equals(var.toString())) {
                System.err.println("Expected: " + code);
                System.err.println("Obtained: " + var.toString());
                throw new RuntimeException("strings do not match!");
            }
        }
    }
}
class MyFileObject extends SimpleJavaFileObject {

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
