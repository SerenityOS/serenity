/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222369 8225488
 * @summary Test behavior of ExecutableElement.getReceiverType
 * @library /tools/javac/lib
 * @build   JavacTestingAbstractProcessor TestExecutableReceiverType
 * @compile -processor TestExecutableReceiverType -proc:only TestExecutableReceiverType.java
 */

import java.util.Set;
import java.lang.annotation.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;

/**
 * Verify that proper type objects are returned from ExecutableElement.getReceiverType
 */
public class TestExecutableReceiverType extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            int count = 0;
            count += testType(elements.getTypeElement("MethodHost"));
            count += testType(elements.getTypeElement("MethodHost.Nested"));

            if (count == 0) {
                messager.printMessage(ERROR, "No executables visited.");
            }
        }
        return true;
    }

    int testType(TypeElement typeElement) {
        int count = 0;
        for (ExecutableElement executable :
                 ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
            count += testExecutable(executable);
        }

        for (ExecutableElement executable :
                 ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            count += testExecutable(executable);
        }
        return count;
    }

    int testExecutable(ExecutableElement executable) {
        TypeKind expectedKind = executable.getAnnotation(ReceiverTypeKind.class).value();
        TypeKind actualKind = executable.getReceiverType().getKind();

        if (actualKind != expectedKind) {
            messager.printMessage(ERROR,
                                  String.format("Unexpected TypeKind on receiver of %s:" +
                                                " expected %s\t got %s%n",
                                                executable, expectedKind, actualKind));
        }

        // Get kind from the type of the executable directly
        TypeKind kindFromType = new TypeKindVisitor<TypeKind, Object>(null) {
            @Override
            public TypeKind visitExecutable(ExecutableType t, Object p) {
                return t.getReceiverType().getKind();
            }
        }.visit(executable.asType());

        if (kindFromType != expectedKind) {
            messager.printMessage(ERROR,
                                  String.format("Unexpected TypeKind on executable's asType() of %s:" +
                                                " expected %s\t got %s%n",
                                                executable, expectedKind, kindFromType));
        }
        return 1;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface ReceiverTypeKind {
    TypeKind value();
}

/**
 * Class to host various methods, etc.
 */
class MethodHost {
    @ReceiverTypeKind(TypeKind.NONE)
    public MethodHost() {}

    @ReceiverTypeKind(TypeKind.NONE)
    public static void foo() {return;}

    @ReceiverTypeKind(TypeKind.NONE)
    public void bar() {return;}

    @ReceiverTypeKind(TypeKind.DECLARED)
    public void quux(MethodHost this) {return;}

    private class Nested {
        @ReceiverTypeKind(TypeKind.DECLARED)
        public Nested(MethodHost MethodHost.this) {}
    }
}
