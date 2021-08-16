/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.regex.Pattern;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

public class ShowTypePlugin implements Plugin {

    public String getName() {
        return "showtype";
    }

    public void init(JavacTask task, String... args) {
        Pattern pattern = null;
        if (args.length == 1)
            pattern = Pattern.compile(args[0]);
        task.addTaskListener(new PostAnalyzeTaskListener(task, pattern));
    }

    private static class PostAnalyzeTaskListener implements TaskListener {
        private final ShowTypeTreeVisitor visitor;

        PostAnalyzeTaskListener(JavacTask task, Pattern pattern) {
            visitor = new ShowTypeTreeVisitor(task, pattern);
        }

        @Override
        public void started(TaskEvent taskEvent) { }

        @Override
        public void finished(TaskEvent taskEvent) {
            if (taskEvent.getKind().equals(TaskEvent.Kind.ANALYZE)) {
                CompilationUnitTree compilationUnit = taskEvent.getCompilationUnit();
                visitor.scan(compilationUnit, null);
            }
        }
    }

    private static class ShowTypeTreeVisitor extends TreePathScanner<Void, Void> {
        private final Trees trees;
        private final Pattern pattern;
        private CompilationUnitTree currCompUnit;

        ShowTypeTreeVisitor(JavacTask task, Pattern pattern) {
            trees = Trees.instance(task);
            this.pattern = pattern;
        }

        @Override
        public Void visitCompilationUnit(CompilationUnitTree tree, Void ignore) {
            currCompUnit = tree;
            return super.visitCompilationUnit(tree, ignore);
        }

        @Override
        public Void visitIdentifier(IdentifierTree tree, Void ignore) {
            show(tree, tree.getName());
            return super.visitIdentifier(tree, ignore);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree, Void ignore) {
            show(tree, tree.getIdentifier());
            return super.visitMemberSelect(tree, ignore);
        }

        void show(Tree tree, CharSequence name) {
            if (pattern == null || pattern.matcher(name).matches()) {
                TypeMirror type = trees.getTypeMirror(getCurrentPath());
                trees.printMessage(Kind.NOTE, "type is " + type, tree, currCompUnit);
            }
        }
    }

}
