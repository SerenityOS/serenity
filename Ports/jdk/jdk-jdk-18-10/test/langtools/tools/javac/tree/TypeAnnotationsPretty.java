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
 * @bug 8023522
 * @summary test Pretty print of type annotations
 * @author wmdietl
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
import java.util.List;
import java.util.LinkedList;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class TypeAnnotationsPretty {
    private final JavaCompiler tool;

    TypeAnnotationsPretty() {
        tool = ToolProvider.getSystemJavaCompiler();
    }

    private List<String> matches = new LinkedList<String>();
    private List<String> mismatches = new LinkedList<String>();

    public static void main(String... args) throws Exception {
        TypeAnnotationsPretty tap = new TypeAnnotationsPretty();

        tap.runField("@TA\nObject cls = null");
        tap.runField("@TA\nObject cls = new @TA Object()");

        tap.runField("@TA\nList<@TB Object> cls = null");
        tap.runField("@TA\nList<@TB Object> cls = new @TA LinkedList<@TB Object>()");

        tap.runField("Class[] cls = null");
        tap.runField("@TA\nClass[] cls = null");
        tap.runField("Class @TA [] cls = null");
        tap.runField("@TA\nClass @TB [] cls = null");

        tap.runField("Class[] cls = new Class[]{Object.class}");
        tap.runField("@TA\nClass[] cls = new @TA Class[]{Object.class}");
        tap.runField("Class @TB [] cls = new Class @TB []{Object.class}");
        tap.runField("@TA\nClass @TB [] cls = new @TA Class @TB []{Object.class}");
        tap.runField("@TA\nClass @TB [] @TC [] cls = new @TA Class @TB [10] @TC []");
        tap.runField("Class @TB [] @TC [] cls = new Class @TB [10] @TC []");
        tap.runField("@TA\nClass @TB [] @TC [] @TD [] cls = new @TA Class @TB [10] @TC [] @TD []");

        tap.runMethod("\n@TA\nObject test(@TB\nList<@TC String> p) {\n" +
                "    return null;\n" +
                "}");


        if (!tap.matches.isEmpty()) {
            for (String m : tap.matches)
                System.out.println(m);
        }
        if (!tap.mismatches.isEmpty()) {
            for (String mm : tap.mismatches)
                System.err.println(mm + NL);
            throw new RuntimeException("Tests failed!");
        }
    }

    private static final String prefix =
            "import java.lang.annotation.*;" +
            "import java.util.*;" +
            "public class Test {";

    private static final String postfix =
            "@Target(ElementType.TYPE_USE)" +
            "@interface TA {}" +
            "@Target(ElementType.TYPE_USE)" +
            "@interface TB {}" +
            "@Target(ElementType.TYPE_USE)" +
            "@interface TC {}" +
            "@Target(ElementType.TYPE_USE)" +
            "@interface TD {}";

    private static final String NL = System.getProperty("line.separator");

    private void runField(String code) throws IOException {
        String src = prefix +
                code + "; }" +
                postfix;

        try (JavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, fm, null, null,
                    null, Arrays.asList(new MyFileObject(src)));

            for (CompilationUnitTree cut : ct.parse()) {
                JCTree.JCVariableDecl var =
                        (JCTree.JCVariableDecl) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(0);
                checkMatch(code, var);
            }
        }
    }

    private void runMethod(String code) throws IOException {
        String src = prefix +
                code + "}" +
                postfix;

        try (JavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, fm, null, null,
                    null, Arrays.asList(new MyFileObject(src)));


            for (CompilationUnitTree cut : ct.parse()) {
                JCTree.JCMethodDecl meth =
                        (JCTree.JCMethodDecl) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(0);
                checkMatch(code, meth);
            }
        }
    }

    void checkMatch(String code, JCTree tree) {
        String expect = code.replace("\n", NL);
        String found = tree.toString();
        if (!expect.equals(found)) {
            mismatches.add("Expected: " + expect + NL +
                    "Obtained: " + found);
        } else {
            matches.add("Passed: " + expect);
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
