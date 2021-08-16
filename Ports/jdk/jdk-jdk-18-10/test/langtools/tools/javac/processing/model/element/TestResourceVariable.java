/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  6911256 6964740 6967842 6961571 7025809
 * @summary Test that the resource variable kind is appropriately set
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor TestResourceVariable
 * @compile -processor TestResourceVariable -proc:only TestResourceVariable.java
 */

import java.io.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.util.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import static javax.tools.Diagnostic.Kind.*;

/**
 * Using the tree API, retrieve element representations of the
 * resource of a try-with-resources statement and verify their kind
 * tags are set appropriately.
 */
public class TestResourceVariable extends JavacTestingAbstractProcessor implements AutoCloseable {
    int resourceVariableCount = 0;

    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {
           Trees trees = Trees.instance(processingEnv);

           for(Element rootElement : roundEnv.getRootElements()) {
               TreePath treePath = trees.getPath(rootElement);

               (new ResourceVariableScanner(trees)).
                   scan(trees.getTree(rootElement),
                        treePath.getCompilationUnit());
           }
           if (resourceVariableCount != 3)
               throw new RuntimeException("Bad resource variable count " +
                                          resourceVariableCount);
       }
       return true;
    }

    @Override
    public void close() {}

    private void test1() {
        try(TestResourceVariable trv = this) {}
    }

    private void test2() {
        try(TestResourceVariable trv1 = this; TestResourceVariable trv2 = trv1) {}
    }

    /**
     * Verify that a resource variable modeled as an element behaves
     * as expected under 6 and latest specific visitors.
     */
    private static void testResourceVariable(Element element) {
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
            public Object visitVariableAsResourceVariable(VariableElement e,
                                                          Void p) {
                return e; // a non-null value
            }
        };

        if (visitorLatest.visit(element) == null) {
            throw new RuntimeException("Null result of resource variable visitation.");
        }
    }

    class ResourceVariableScanner extends TreeScanner<Void, CompilationUnitTree> {
       private Trees trees;

       public ResourceVariableScanner(Trees trees) {
           super();
           this.trees = trees;
       }
       @Override
       public Void visitVariable(VariableTree node, CompilationUnitTree cu) {
           Element element = trees.getElement(trees.getPath(cu, node));

           System.out.println("Name: " + element.getSimpleName() +
                              "\tKind: " + element.getKind());
           if (element.getKind() == ElementKind.RESOURCE_VARIABLE) {
               testResourceVariable(element);
               resourceVariableCount++;
           }
           return super.visitVariable(node, cu);
       }
    }
}
