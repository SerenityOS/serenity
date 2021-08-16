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
 * @bug 8129547
 * @summary Excess entries in BootstrapMethods with the same (bsm, bsmKind, bsmStaticArgs), but different dynamicArgs
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreeScanner;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.BootstrapMethods_attribute;
import com.sun.tools.classfile.ClassFile;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.jvm.ClassFile.*;

public class TestBootstrapMethodsCount {

    public static void main(String... args) throws Exception {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        new TestBootstrapMethodsCount().run(comp);
    }

    DiagChecker dc;

    TestBootstrapMethodsCount() {
        dc = new DiagChecker();
    }

    public void run(JavaCompiler comp) {
        JavaSource source = new JavaSource();
        JavacTaskImpl ct = (JavacTaskImpl)comp.getTask(null, null, dc,
                Arrays.asList("-g"), null, Arrays.asList(source));
        Context context = ct.getContext();
        Symtab syms = Symtab.instance(context);
        Names names = Names.instance(context);
        Types types = Types.instance(context);
        ct.addTaskListener(new Indifier(syms, names, types));
        try {
            ct.generate();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new AssertionError(
                    String.format("Error thrown when compiling following code\n%s",
                            source.source));
        }
        if (dc.diagFound) {
            throw new AssertionError(
                    String.format("Diags found when compiling following code\n%s\n\n%s",
                            source.source, dc.printDiags()));
        }
        verifyBytecode();
    }

    void verifyBytecode() {
        File compiledTest = new File("Test.class");
        try {
            ClassFile cf = ClassFile.read(compiledTest);
            BootstrapMethods_attribute bsm_attr =
                    (BootstrapMethods_attribute)cf
                            .getAttribute(Attribute.BootstrapMethods);
            int length = bsm_attr.bootstrap_method_specifiers.length;
            if (length != 1) {
                throw new Error("Bad number of method specifiers " +
                        "in BootstrapMethods attribute: " + length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + compiledTest +": " + e);
        }
    }

    class JavaSource extends SimpleJavaFileObject {

        static final String source = "import java.lang.invoke.*;\n" +
                "class Bootstrap {\n" +
                "   public static CallSite bsm(MethodHandles.Lookup lookup, " +
                "String name, MethodType methodType) {\n" +
                "       return null;\n" +
                "   }\n" +
                "}\n" +
                "class Test {\n" +
                "   void m1() { }\n" +
                "   void m2(Object arg1) { }\n" +
                "   void test1() {\n" +
                "      Object o = this; // marker statement \n" +
                "      m1();\n" +
                "   }\n" +
                "   void test2(Object arg1) {\n" +
                "      Object o = this; // marker statement \n" +
                "      m2(arg1);\n" +
                "   }\n" +
                "}";

        JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    class Indifier extends TreeScanner<Void, Void> implements TaskListener {

        MethodSymbol bsm;
        Symtab syms;
        Names names;
        Types types;

        Indifier(Symtab syms, Names names, Types types) {
            this.syms = syms;
            this.names = names;
            this.types = types;
        }

        @Override
        public void started(TaskEvent e) {
            //do nothing
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                scan(e.getCompilationUnit(), null);
            }
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
            super.visitMethodInvocation(node, p);
            JCMethodInvocation apply = (JCMethodInvocation)node;
            JCIdent ident = (JCIdent)apply.meth;
            Symbol oldSym = ident.sym;
            if (!oldSym.isConstructor()) {
                ident.sym = new Symbol.DynamicMethodSymbol(oldSym.name,
                        oldSym.owner, bsm.asHandle(), oldSym.type, new LoadableConstant[0]);
            }
            return null;
        }

        @Override
        public Void visitMethod(MethodTree node, Void p) {
            super.visitMethod(node, p);
            if (node.getName().toString().equals("bsm")) {
                bsm = ((JCMethodDecl)node).sym;
            }
            return null;
        }
    }

    static class DiagChecker
            implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean diagFound;
        ArrayList<String> diags = new ArrayList<>();

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            diags.add(diagnostic.getMessage(Locale.getDefault()));
            diagFound = true;
        }

        String printDiags() {
            StringBuilder buf = new StringBuilder();
            for (String s : diags) {
                buf.append(s);
                buf.append("\n");
            }
            return buf.toString();
        }
    }

}
