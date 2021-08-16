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
 * @bug 6967842
 * @summary Element not returned from tree API for ARM resource variables.
 * @author A. Sundararajan
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build   JavacTestingAbstractProcessor TestResourceElement
 * @compile -processor TestResourceElement -proc:only TestResourceElement.java
 */

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import java.util.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

public class TestResourceElement extends JavacTestingAbstractProcessor implements AutoCloseable {
    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {
           Trees trees = Trees.instance(processingEnv);

           for(Element rootElement : roundEnv.getRootElements()) {
               TreePath treePath = trees.getPath(rootElement);

               VariableScanner varScanner =  new VariableScanner(trees);
               varScanner.scan(trees.getTree(rootElement),
                        treePath.getCompilationUnit());
               if (varScanner.getTrvElement() == null) {
                   throw new AssertionError("Element is null for 'trv'");
               }
           }
       }
       return true;
    }

    @Override
    public void close() {}

    private void test1() {
        // The resource variable "trv"'s Element is checked.
        // Do not change the name of the variable.
        try(TestResourceElement trv = this) {}
    }

    class VariableScanner extends TreeScanner<Void, CompilationUnitTree> {
       private Trees trees;
       private Element trvElement;

       public VariableScanner(Trees trees) {
           super();
           this.trees = trees;
       }
       @Override
       public Void visitVariable(VariableTree node, CompilationUnitTree cu) {
           // if this is "trv", get it's element.
           if (node.getName().contentEquals("trv")) {
               trvElement = trees.getElement(trees.getPath(cu, node));
           }
           return super.visitVariable(node, cu);
       }

       Element getTrvElement() {
           return trvElement;
       }
   }
}
