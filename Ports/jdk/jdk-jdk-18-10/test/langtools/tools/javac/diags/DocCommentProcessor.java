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

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import java.io.PrintWriter;
import javax.tools.Diagnostic;

/**
 * Standard annotation processor for use by examples to
 * scan DocCommentTree nodes looking for ErroneousTree,
 * on which to call {@code getMessage}.
 */
@SupportedAnnotationTypes("*")
public class DocCommentProcessor extends AbstractProcessor {
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(ProcessingEnvironment pEnv) {
        super.init(pEnv);
        trees = DocTrees.instance(pEnv);
        messager = pEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment rEnv) {
        for (Element e : rEnv.getRootElements()) {
            new DocCommentScanner().scan(e);
        }
        return true;
    }

    class DocCommentScanner extends TreePathScanner<Void,Void> {
        public void scan(Element e) {
            scan(trees.getPath(e), null);
        }

        @Override
        public Void visitClass(ClassTree tree, Void ignore) {
            check();
            return super.visitClass(tree, ignore);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void ignore) {
            check();
            return super.visitMethod(tree, ignore);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void ignore) {
            check();
            return super.visitVariable(tree, ignore);
        }

        private void check() {
            DocCommentTree dc = trees.getDocCommentTree(getCurrentPath());
            if (dc == null)
                return;

            DocTreeScanner<Void, Void> s = new DocTreeScanner<Void, Void>() {
                @Override
                public Void visitErroneous(ErroneousTree tree, Void ignore) {
                    messager.printMessage(Diagnostic.Kind.NOTE, tree.getDiagnostic().getMessage(null));
                    return null;
                }
            };

            s.scan(dc, null);
        }

    }

    private DocTrees trees;
    private Messager messager;
}
