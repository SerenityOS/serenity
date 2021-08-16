/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8042239
 * @summary Verify that TreeMaker.Type(Type) can handle all reasonable types
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor MakeTypeTest
 * @compile/process/ref=MakeTypeTest.out -XDaccessInternalAPI -processor MakeTypeTest MakeTypeTest.java
 */

import java.lang.annotation.*;
import java.util.*;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

public class MakeTypeTest extends JavacTestingAbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver())
            return false;

        JavacTask.instance(processingEnv).addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent e) {
            }
            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.ANALYZE &&
                    e.getTypeElement().getQualifiedName().contentEquals("MakeTypeTest")) {
                    doTest();
                }
            }
        });

        return false;
    }

    void doTest() {
        //go through this file, look for @TestType and verify TreeMaker.Type behavior:
        Context ctx = ((JavacProcessingEnvironment) processingEnv).getContext();
        JavacTrees trees = JavacTrees.instance(ctx);
        TypeElement testType = processingEnv.getElementUtils().getTypeElement("MakeTypeTest");
        TreePath path = trees.getPath(testType);

        Set<TypeKind> unseenTypeKinds = EnumSet.allOf(TypeKind.class);

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                handleDecl(new TreePath(getCurrentPath(), node.getType()));
                return super.visitVariable(node, p);
            }

            @Override
            public Void visitMethod(MethodTree node, Void p) {
                if (node.getReturnType() != null)
                    handleDecl(new TreePath(getCurrentPath(), node.getReturnType()));
                return super.visitMethod(node, p);
            }

            @Override
            public Void visitTypeParameter(TypeParameterTree node, Void p) {
                TypeVariable type = (TypeVariable) trees.getTypeMirror(getCurrentPath());
                TreePath aBoundPath = new TreePath(getCurrentPath(), node.getBounds().get(0));
                handleDecl(aBoundPath, (Type) type.getUpperBound());
                return super.visitTypeParameter(node, p);
            }

            void handleDecl(TreePath typePath) {
                handleDecl(typePath, (Type) trees.getTypeMirror(typePath));
            }

            void handleDecl(TreePath typePath, Type type) {
                Element decl = trees.getElement(typePath.getParentPath());
                TestType testType = decl.getAnnotation(TestType.class);

                if (testType == null) return ;

                if (testType.nested() >= 0) {
                    ClassType ct = (ClassType) type;
                    type = ct.getTypeArguments().get(testType.nested());
                }

                JCExpression typeExpr = TreeMaker.instance(ctx).Type(type);

                if (!typeExpr.getKind().equals(testType.expectedKind())) {
                    throw new IllegalStateException("was=" + typeExpr + ", kind=" +
                            typeExpr.getKind() + "; expected kind: " +
                            testType.expectedKind() + "; type=" + type);
                }
                unseenTypeKinds.remove(type.getKind());
            }

        }.scan(path, null);

        unseenTypeKinds.removeAll(Arrays.asList(TypeKind.NONE, TypeKind.NULL, TypeKind.ERROR,
                TypeKind.PACKAGE, TypeKind.EXECUTABLE, TypeKind.OTHER, TypeKind.MODULE));

        if (!unseenTypeKinds.isEmpty())
            throw new IllegalStateException("Unhandled types=" + unseenTypeKinds);

        System.err.println("done.");
    }

    //the following defines the Types that should be passed into TreeMaker.Type and
    //the expected resulting Tree kind:

    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    boolean f1;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    byte f2;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    char f3;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    double f4;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    float f5;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    int f6;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    long f7;
    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    short f8;
    @TestType(expectedKind=Tree.Kind.PARAMETERIZED_TYPE)
    List<? extends String> f9;
    @TestType(expectedKind=Tree.Kind.ARRAY_TYPE)
    int[] fa;
    @TestType(expectedKind=Tree.Kind.EXTENDS_WILDCARD, nested = 0)
    List<? extends String> fb;
    @TestType(expectedKind=Tree.Kind.SUPER_WILDCARD, nested = 0)
    List<? super String> fc;
    @TestType(expectedKind=Tree.Kind.UNBOUNDED_WILDCARD, nested = 0)
    List<?> fd;

    @TestType(expectedKind=Tree.Kind.PRIMITIVE_TYPE)
    void voidMethod() {
        try {
            voidMethod();
        } catch (@TestType(expectedKind=Tree.Kind.UNION_TYPE) NullPointerException |
                                                              IllegalStateException ex) {
        }
    }

    class WithTypeParam<@TestType(expectedKind=Tree.Kind.INTERSECTION_TYPE)
                        T extends CharSequence & Runnable> {
        @TestType(expectedKind=Tree.Kind.IDENTIFIER)
        T voidMethod() {
            return null;
        }
    }

}

//TreeMaker.Type will be tested for the type the element annotated by this annotation
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD,
         ElementType.PARAMETER, ElementType.TYPE_PARAMETER})
@interface TestType {
    //the expected Tree kind of the Tree that will be returned from TreeMaker.Type for the type
    public Tree.Kind expectedKind();
    //if >=0, the current type will be interpreted as a ClassType and the type to test will be
    //the given type argument:
    public int nested() default -1;
}
