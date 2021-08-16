/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6418666 6423973 6453386 7025809
 * @summary Test the NoTypes: VOID, PACKAGE, NONE
 * @author  Scott Seligman
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor
 * @compile -g NoTypes.java
 * @compile -processor NoTypes -proc:only NoTypes.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

import static javax.lang.model.type.TypeKind.*;

public class NoTypes extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annoTypes,
                           RoundEnvironment round) {
        if (!round.processingOver())
            doit(annoTypes, round);
        return true;
    }

    private void doit(Set<? extends TypeElement> annoTypes,
                      RoundEnvironment round) {

        // The superclass of Object is NONE.
        TypeElement object = elements.getTypeElement("java.lang.Object");
        verifyKind(NONE, object.getSuperclass());

        // The enclosing type of a top-level class is NONE
        verifyKind(NONE, ((DeclaredType)object.asType()).getEnclosingType());

        // The superclass of an interface is NONE.
        TypeElement i = elements.getTypeElement("NoTypes.I");
        verifyKind(NONE, i.getSuperclass());

        // The type of a package is PACKAGE.
        Element pkg = i.getEnclosingElement().getEnclosingElement();
        verifyKind(PACKAGE, pkg.asType());

        Element module = pkg.getEnclosingElement();

        if (module == null)
            throw new AssertionError();

        if (module.getKind() != ElementKind.MODULE)
            throw new AssertionError();

        ModuleElement modle = (ModuleElement) module;

        if (!modle.isUnnamed())
            throw new AssertionError();

        // A module isn't enclosed.
        if (module.getEnclosingElement() != null)
            throw new AssertionError();

        verifyKind(VOID, types.getNoType(VOID));
        verifyKind(NONE, types.getNoType(NONE));

        // The return type of a constructor or void method is VOID.
        class Scanner extends ElementScanner<Void, Void> {
            @Override
            public Void visitExecutable(ExecutableElement e, Void p) {
                verifyKind(VOID, e.getReturnType());
                ExecutableType t = (ExecutableType) e.asType();
                verifyKind(VOID, t.getReturnType());
                return null;
            }
        }
        TypeElement c = elements.getTypeElement("NoTypes.C");
        new Scanner().scan(c);
    }

    /**
     * Verify that a NoType instance is of a particular kind, and that
     * the latest TypeKindVisitor properly dispatches on it.
     */
    private void verifyKind(TypeKind kind, TypeMirror type) {
        class Vis extends TypeKindVisitor<TypeKind, Void> {
            @Override
            public TypeKind visitNoTypeAsVoid(NoType t, Void p) {
                return VOID;
            }
            @Override
            public TypeKind visitNoTypeAsPackage(NoType t, Void p) {
                return PACKAGE;
            }
            @Override
            public TypeKind visitNoTypeAsNone(NoType t, Void p) {
                return NONE;
            }
        }
        if (kind != type.getKind() || kind != new Vis().visit(type))
            throw new AssertionError();
    }

    // Fodder for the tests
    interface I {
    }

    class C {
        C() {}
        void m() {}
    }
}
