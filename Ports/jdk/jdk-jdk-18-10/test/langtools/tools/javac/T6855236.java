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
 * @bug 6855236
 * @summary Compiler Tree API TreePath class generates NullPointerException from Iterator
 * @modules jdk.compiler
 * @compile T6855236.java
 * @compile -processor T6855236 -proc:only T6855236.java
 */

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

@SupportedAnnotationTypes("*")
public class T6855236 extends AbstractProcessor {

    private Trees trees;

    @Override
    public void init(ProcessingEnvironment pe) {
        super.init(pe);
        trees = Trees.instance(pe);
    }

    @Override
    public boolean process(Set<? extends TypeElement> arg0, RoundEnvironment roundEnvironment) {
        // Scanner class to scan through various component elements
        CodeVisitor visitor = new CodeVisitor();

        for (Element e : roundEnvironment.getRootElements()) {
            TreePath tp = trees.getPath(e);
            visitor.scan(tp, trees);
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    class CodeVisitor extends TreePathScanner<Object, Trees> {

        @Override
        public Object visitMethodInvocation(MethodInvocationTree node, Trees p) {
            System.out.println("current path: ");
            for (Tree t : getCurrentPath()) {
                System.out.println("    " + t.getKind() + ": " + trim(t, 64));
            }
            System.out.println("parent path: " + getCurrentPath().getParentPath());
            System.out.println("method select: " + node.getMethodSelect().toString());
            for (ExpressionTree arg : node.getArguments()) {
                System.out.println("argument: " + arg.toString());
            }
            return super.visitMethodInvocation(node, p);
        }

        @Override
        public Object visitExpressionStatement(ExpressionStatementTree node, Trees p) {
            ExpressionTree t = node.getExpression();
            System.out.println();
            System.out.println("expression statement: " + trim(t, 64));
            return super.visitExpressionStatement(node, p);
        }

    }

    private String trim(Tree t, int len) {
        String s = t.toString().trim().replaceAll("\\s+", " ");
        if (s.length() > len)
            s = s.substring(0, len) + "...";
        return s;
    }

}


