/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8191234
 * @summary Test TypeKind visitors on pseudo types.
 * @library /tools/javac/lib
 * @modules java.compiler
 * @build   JavacTestingAbstractProcessor TestTypeKindVisitors
 * @compile -processor TestTypeKindVisitors -proc:only TestTypeKindVisitors.java
 */

import java.lang.annotation.Annotation;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.lang.model.SourceVersion.*;

public class TestTypeKindVisitors extends JavacTestingAbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> tes,
                           RoundEnvironment round) {
        if (round.processingOver())
            return true;

        List<NoType> tradNoTypes = List.of(types.getNoType(TypeKind.NONE),
                                           types.getNoType(TypeKind.VOID),
                                           getPackageNoType());
        NoType moduleNoType = getModuleNoType();

        // For KindVisitors based on 6, 7, and 8
        for (TypeVisitor<TypeKind, String> visitor : getVisitors()) {
            System.out.println(visitor.getClass().getSuperclass().getName());

            for (NoType noType : tradNoTypes) {
                System.out.println("\t" + noType.toString());
                checkTypeKind(noType.getKind(), visitor.visit(noType));
            }

            if (RELEASE_9.compareTo(visitor.getClass().getSuperclass().
                                    getAnnotation(SupportedSourceVersion.class).
                                    value()) > 0) {
                try {
                    System.out.println("\t" + moduleNoType.toString());
                    visitor.visit(moduleNoType);
                } catch (UnknownTypeException ute) {
                    ; // Expected
                }
            } else {
                checkTypeKind(moduleNoType.getKind(), visitor.visit(moduleNoType));
            }
        }

        return true;
    }

    private NoType getPackageNoType() {
        TypeMirror type = elements.getPackageElement("java.lang").asType();
        checkTypeKind(TypeKind.PACKAGE, type.getKind());
        return (NoType) type;
    }

    private NoType getModuleNoType() {
        TypeMirror type = elements.getModuleElement("java.base").asType();
        checkTypeKind(TypeKind.MODULE, type.getKind());
        return (NoType) type;
    }

    private void checkTypeKind(TypeKind expected, TypeKind retreived) {
        if (retreived != expected)
            throw new AssertionError("Unexpected type kind " + retreived);
    }

    List<TypeVisitor<TypeKind, String>> getVisitors() {
        return List.of(new TypeKindVisitor6<>(null) {
                           @Override
                           protected TypeKind defaultAction(TypeMirror e, String p) {
                               throw new AssertionError("Should not reach");
                           }

                           @Override
                           public TypeKind visitNoTypeAsVoid(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsNone(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsPackage(NoType t, String p) {
                               return t.getKind();
                           }
                           // Leave default behavior for a NoType module
                       },

                       new TypeKindVisitor7<>(null){
                           @Override
                           protected TypeKind defaultAction(TypeMirror e, String p) {
                               throw new AssertionError("Should not reach");
                           }

                           @Override
                           public TypeKind visitNoTypeAsVoid(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsNone(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsPackage(NoType t, String p) {
                               return t.getKind();
                           }
                           // Leave default behavior for a NoType module

                       },

                       new TypeKindVisitor8<>(null){
                           @Override
                           protected TypeKind defaultAction(TypeMirror e, String p) {
                               throw new AssertionError("Should not reach");
                           }

                           @Override
                           public TypeKind visitNoTypeAsVoid(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsNone(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsPackage(NoType t, String p) {
                               return t.getKind();
                           }
                           // Leave default behavior for a NoType module

                       },

                       new TypeKindVisitor9<>(null){
                           @Override
                           protected TypeKind defaultAction(TypeMirror e, String p) {
                               throw new AssertionError("Should not reach");
                           }

                           @Override
                           public TypeKind visitNoTypeAsVoid(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsNone(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsPackage(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsModule(NoType t, String p) {
                               return t.getKind();
                           }
                       },

                        new TypeKindVisitor14<>(null){
                           @Override
                           protected TypeKind defaultAction(TypeMirror e, String p) {
                               throw new AssertionError("Should not reach");
                           }

                           @Override
                           public TypeKind visitNoTypeAsVoid(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsNone(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsPackage(NoType t, String p) {
                               return t.getKind();
                           }

                           @Override
                           public TypeKind visitNoTypeAsModule(NoType t, String p) {
                               return t.getKind();
                           }
                       }
        );
    }
}
