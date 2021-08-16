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

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCExpression;

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.UnknownTypeException;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.SimpleTypeVisitor7;

@SupportedAnnotationTypes("Check")
public class ModelChecker extends JavacTestingAbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver())
            return true;

        Trees trees = Trees.instance(processingEnv);

        TypeElement testAnno = elements.getTypeElement("Check");
        for (Element elem: roundEnv.getElementsAnnotatedWith(testAnno)) {
            TreePath p = trees.getPath(elem);
            new IntersectionCastTester(trees).scan(p, null);
        }
        return true;
    }

    class IntersectionCastTester extends TreePathScanner<Void, Void> {
        Trees trees;

        public IntersectionCastTester(Trees trees) {
            super();
            this.trees = trees;
        }

        @Override
        public Void visitVariable(VariableTree node, Void p) {

            TreePath varPath = new TreePath(getCurrentPath(), node);
            Element v = trees.getElement(varPath);

            IntersectionTypeInfo it = v.getAnnotation(IntersectionTypeInfo.class);
            assertTrue(it != null, "IntersectionType annotation must be present");

            ExpressionTree varInit = node.getInitializer();
            assertTrue(varInit != null && varInit.getKind() == Tree.Kind.TYPE_CAST,
                    "variable must have be initialized to an expression containing an intersection type cast");

            TypeMirror t = ((JCExpression)((TypeCastTree)varInit).getType()).type;

            validateIntersectionTypeInfo(t, it);

            for (Element e2 : types.asElement(t).getEnclosedElements()) {
                assertTrue(false, "an intersection type has no declared members");
            }

            for (Element e2 : elements.getAllMembers((TypeElement)types.asElement(t))) {
                Member m = e2.getAnnotation(Member.class);
                if (m != null) {
                    assertTrue(e2.getKind() == m.value(), "Expected " + m.value() + " - found " + e2.getKind());
                }
            }

            assertTrue(assertionCount == 9, "Expected 9 assertions - found " + assertionCount);
            return super.visitVariable(node, p);
        }
    }

    private void validateIntersectionTypeInfo(TypeMirror expectedIntersectionType, IntersectionTypeInfo it) {

        assertTrue(expectedIntersectionType.getKind() == TypeKind.INTERSECTION, "INTERSECTION kind expected");

        try {
            new SimpleTypeVisitor6<Void, Void>(){}.visit(expectedIntersectionType);
            throw new RuntimeException("Expected UnknownTypeException not thrown.");
        } catch (UnknownTypeException ute) {
            ; // Expected
        }

        try {
            new SimpleTypeVisitor7<Void, Void>(){}.visit(expectedIntersectionType);
            throw new RuntimeException("Expected UnknownTypeException not thrown.");
        } catch (UnknownTypeException ute) {
            ; // Expected
        }

        IntersectionType intersectionType = new SimpleTypeVisitor<IntersectionType, Void>(){
            @Override
            protected IntersectionType defaultAction(TypeMirror e, Void p) {return null;}

            @Override
            public IntersectionType visitIntersection(IntersectionType t, Void p) {return t;}
        }.visit(expectedIntersectionType);
        assertTrue(intersectionType != null, "Must get a non-null intersection type.");

        assertTrue(it.value().length == intersectionType.getBounds().size(), "Cardinalities do not match");

        String[] typeNames = it.value();
        for(int i = 0; i < typeNames.length; i++) {
            TypeMirror typeFromAnnotation = nameToType(typeNames[i]);
            assertTrue(types.isSameType(typeFromAnnotation, intersectionType.getBounds().get(i)),
                       "Types were not equal.");
        }
    }

    private TypeMirror nameToType(String name) {
        return elements.getTypeElement(name).asType();
    }

    private static void assertTrue(boolean cond, String msg) {
        assertionCount++;
        if (!cond)
            throw new AssertionError(msg);
    }

    static int assertionCount = 0;
}
