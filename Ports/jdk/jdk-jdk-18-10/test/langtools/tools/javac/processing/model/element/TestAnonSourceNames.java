/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * Using the tree API, retrieve element representations of anonymous
 * classes and verify their names are as specified.
 */
public class TestAnonSourceNames extends JavacTestingAbstractProcessor {

   public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
       if (!roundEnv.processingOver()) {
           Trees trees = Trees.instance(processingEnv);

           for(Element rootElement : roundEnv.getRootElements()) {
               TreePath treePath = trees.getPath(rootElement);

               (new ClassTreeScanner(trees)).
                   scan(trees.getTree(rootElement),
                        treePath.getCompilationUnit());
           }
       }
       return true;
   }

   class ClassTreeScanner extends TreeScanner<Void, CompilationUnitTree> {
       private Trees trees;

       public ClassTreeScanner(Trees trees) {
           super();
           this.trees = trees;
       }
       @Override
       public Void visitClass(ClassTree node, CompilationUnitTree cu) {
                     Element element = trees.getElement(trees.getPath(cu, node));
           if (element == null) {
               processingEnv.getMessager().printMessage(ERROR,
                                                        "No element retrieved for node named ''" +
                                                        node.getSimpleName() + "''.");
           } else {

               System.out.println("\nVisiting class ``" + element.getSimpleName() +
                                  "'' of kind " + element.getKind());
                         if (element instanceof TypeElement) {
                   TypeElement typeElement = (TypeElement) element;
                   String s = typeElement.getQualifiedName().toString();
                   System.out.println("\tqualified name:" + s);
               } else {
                   throw new RuntimeException("TypeElement not gotten from ClassTree.");
               }
           }
           return super.visitClass(node, cu);
       }
   }
}
