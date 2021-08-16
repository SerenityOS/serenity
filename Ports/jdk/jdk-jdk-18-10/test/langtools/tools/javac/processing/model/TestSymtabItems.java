/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021183 7025809
 * @summary 269: assertion failure getting enclosing element of an undefined name
 * @modules jdk.compiler/com.sun.tools.javac.code:+open
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.model
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.lang.reflect.Field;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.UnknownElementException;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnknownTypeException;
import javax.lang.model.util.*;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Context;

/**
 * Scan javac Symtab looking for TypeMirrors and Elements, and ensure that
 * no exceptions are thrown when used with javax.lang.model visitors.
 *
 */
public class TestSymtabItems {
    public static void main(String... args) throws Exception {
        new TestSymtabItems().run();
    }

    void run() throws Exception {
        Context c = new Context();
        JavacFileManager.preRegister(c);
        Symtab syms = Symtab.instance(c);
        JavacTypes types = JavacTypes.instance(c);
        JavaCompiler.instance(c);  // will init ClassReader.sourceCompleter

//        print("noSymbol", syms.noSymbol);
//        print("errSymbol", syms.errSymbol);
//        print("unknownSymbol", syms.unknownSymbol);
//        print("botType", syms.botType);
//        print("errType", syms.errType);
//        print("unknownType", syms.unknownType);

        for (Field f: Symtab.class.getDeclaredFields()) {
//            System.err.println(f.getType() + " " + f.getName());

            // Temporarily ignore methodHandle and transientMethodHandle
            // during API evolution
            if (f.getName().toLowerCase().contains("methodhandle"))
                continue;

            //both noModule and unnamedModule claim the unnamed package, ignore noModule for now:
            if (f.getName().equals("noModule"))
                continue;

            f.setAccessible(true);
            Class<?> ft = f.getType();
            if (TypeMirror.class.isAssignableFrom(ft))
                print(f.getName(), (TypeMirror) f.get(syms), types);
            else if(Element.class.isAssignableFrom(ft))
                print(f.getName(), (Element) f.get(syms));
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void print(String label, Element e) {
        ElemPrinter ep = new ElemPrinter();
        System.err.println("Test " + label);
        ep.visit(e);
        System.err.println();
    }

    void print(String label, TypeMirror t, Types types) {
        TypePrinter tp = new TypePrinter();
        System.err.println("Test " + label);
        tp.visit(t, types);
        System.err.println();
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    class ElemPrinter extends ElementScanner14<Void, Void> {
        @Override
        public Void visitModule(ModuleElement e, Void p) {
            show("module", e);
            indent(+1);
            super.visitModule(e, p);
            indent(-1);
            return null;
        }

        @Override
        public Void visitPackage(PackageElement e, Void p) {
            show("package", e);
            indent(+1);
            super.visitPackage(e, p);
            indent(-1);
            return null;
        }

        @Override
        public Void visitType(TypeElement e, Void p) {
            show("type", e);
            indent(+1);
            super.visitType(e, p);
            indent(-1);
            return null;
        }

        @Override
        public Void visitVariable(VariableElement e, Void p) {
            show("variable", e);
            indent(+1);
            super.visitVariable(e, p);
            indent(-1);
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableElement e, Void p) {
            show("executable", e);
            indent(+1);
            super.visitExecutable(e, p);
            indent(-1);
            return null;
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, Void p) {
            show("type parameter", e);
            indent(+1);
            super.visitTypeParameter(e, p);
            indent(-1);
            return null;
        }

        @Override
        public Void visitUnknown(Element e, Void p) {
            show("unknown", e);
            indent(+1);
            try {
                super.visitUnknown(e, p);
            } catch (UnknownElementException ex) {
                System.err.println("caught " + ex);
            }
            indent(-1);
            return null;
        }

        void indent(int i) {
            indent += i;
        }

        String sp(int w) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < w; i++)
                sb.append("    ");
            return sb.toString();
        }

        void show(String label, Element e) {
            System.err.println(sp(indent) + label
                    + ": mods:" + e.getModifiers()
                    + " " + e.getSimpleName()
                    + ", kind: " + e.getKind()
                    + ", type: " + e.asType()
                    + ", encl: " + e.getEnclosingElement());

            // The following checks help establish why NPE might occur when trying to scan children
            if (e instanceof ClassSymbol) {
                ClassSymbol csym = (ClassSymbol) e;
                if (csym.members_field == null)
                    error("members_field is null");
                if (csym.type == null)
                    System.err.println("type is null");
            }
        }

        int indent;
    };

    class TypePrinter extends SimpleTypeVisitor14<Void, Types> {
        @Override
        public Void defaultAction(TypeMirror m, Types types) {
            System.err.println(m.getKind() + " " + m + " " + types.asElement(m));
            return null;
        }

        @Override
        public Void visitUnknown(TypeMirror t, Types types) {
            try {
                return super.visitUnknown(t, types);
            } catch (UnknownTypeException ex) {
                System.err.println("caught " + ex);
                return null;
            }
        }
    };
}
