/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  8235541
 * @summary Test that the binding variable kind is appropriately set
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile TestBindingVariable.java
 * @compile -processor TestBindingVariable -proc:only TestBindingVariableData.java
 */

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import java.util.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

/**
 * Using the tree API, retrieve element representations of the
 * binding variables of the type test pattern, and verify their kind
 * tags are set appropriately.
 */
public class TestBindingVariable extends JavacTestingAbstractProcessor implements AutoCloseable {
    int bindingVariableCount = 0;

    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {
           Trees trees = Trees.instance(processingEnv);

           for(Element rootElement : roundEnv.getRootElements()) {
               TreePath treePath = trees.getPath(rootElement);

               (new BindingVariableScanner(trees)).
                   scan(treePath.getCompilationUnit(), null);
           }
           if (bindingVariableCount != 2)
               throw new RuntimeException("Bad binding variable count " +
                                          bindingVariableCount);
       }
       return true;
    }

    @Override
    public void close() {}

    /**
     * Verify that a binding variable modeled as an element behaves
     * as expected under 6 and latest specific visitors.
     */
    private static void testBindingVariable(Element element) {
        ElementVisitor visitor6 = new ElementKindVisitor6<Void, Void>() {};

        try {
            visitor6.visit(element);
            throw new RuntimeException("Expected UnknownElementException not thrown.");
        } catch (UnknownElementException uee) {
            ; // Expected.
        }

        ElementKindVisitor visitorLatest =
            new ElementKindVisitor<Object, Void>() {
            @Override
            public Object visitVariableAsBindingVariable(VariableElement e,
                                                         Void p) {
                return e; // a non-null value
            }
        };

        if (visitorLatest.visit(element) == null) {
            throw new RuntimeException("Null result of resource variable visitation.");
        }
    }

    class BindingVariableScanner extends TreePathScanner<Void, Void> {
        private Trees trees;

        public BindingVariableScanner(Trees trees) {
            super();
            this.trees = trees;
        }
        @Override
        public Void visitBindingPattern(BindingPatternTree node, Void p) {
            handleTreeAsBindingVar(new TreePath(getCurrentPath(), node.getVariable()));
            return super.visitBindingPattern(node, p);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void p) {
            if (node.getName().contentEquals("bindingVar")) {
                handleTreeAsBindingVar(getCurrentPath());
            }
            return super.visitIdentifier(node, p);
        }

        private void handleTreeAsBindingVar(TreePath tp) {
           Element element = trees.getElement(tp);

           System.out.println("Name: " + element.getSimpleName() +
                              "\tKind: " + element.getKind());
           if (element.getKind() != ElementKind.BINDING_VARIABLE) {
               throw new RuntimeException("Expected a binding variable, but got: " +
                                          element.getKind());
           }
           testBindingVariable(element);
           bindingVariableCount++;
        }
    }
}
