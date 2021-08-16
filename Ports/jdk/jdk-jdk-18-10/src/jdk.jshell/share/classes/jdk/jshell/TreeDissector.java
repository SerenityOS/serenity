/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jshell;


import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.Name;
import static jdk.jshell.Util.isDoIt;
import jdk.jshell.TaskFactory.AnalyzeTask;
import jdk.jshell.Wrap.Range;

import java.util.List;

import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.lang.model.type.TypeMirror;
import jdk.jshell.TypePrinter.AnonymousTypeKind;
import jdk.jshell.Util.Pair;

/**
 * Utilities for analyzing compiler API parse trees.
 * @author Robert Field
 */

class TreeDissector {

    private final TaskFactory.BaseTask bt;
    private final ClassTree targetClass;
    private final CompilationUnitTree targetCompilationUnit;
    private SourcePositions theSourcePositions = null;

    private TreeDissector(TaskFactory.BaseTask bt, CompilationUnitTree targetCompilationUnit, ClassTree targetClass) {
        this.bt = bt;
        this.targetCompilationUnit = targetCompilationUnit;
        this.targetClass = targetClass;
    }

    static TreeDissector createByFirstClass(TaskFactory.BaseTask bt) {
        Pair<CompilationUnitTree, ClassTree> pair = classes(bt.firstCuTree())
                .findFirst().orElseGet(() -> new Pair<>(bt.firstCuTree(), null));

        return new TreeDissector(bt, pair.first, pair.second);
    }

    private static final Predicate<? super Tree> isClassOrInterface =
            t -> t.getKind() == Tree.Kind.CLASS || t.getKind() == Tree.Kind.INTERFACE;

    private static Stream<Pair<CompilationUnitTree, ClassTree>> classes(CompilationUnitTree cut) {
        return cut == null
                ? Stream.empty()
                : cut.getTypeDecls().stream()
                        .filter(isClassOrInterface)
                        .map(decl -> new Pair<>(cut, (ClassTree)decl));
    }

    private static Stream<Pair<CompilationUnitTree, ClassTree>> classes(Iterable<? extends CompilationUnitTree> cuts) {
        return Util.stream(cuts)
                .flatMap(TreeDissector::classes);
    }

    static TreeDissector createBySnippet(TaskFactory.BaseTask bt, Snippet si) {
        String name = si.className();

        Pair<CompilationUnitTree, ClassTree> pair = classes(bt.cuTrees())
                .filter(p -> p.second.getSimpleName().contentEquals(name))
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Class " + name + " is not found."));

        return new TreeDissector(bt, pair.first, pair.second);
    }

    Types types() {
        return bt.types();
    }

    Trees trees() {
        return bt.trees();
    }

    SourcePositions getSourcePositions() {
        if (theSourcePositions == null) {
            theSourcePositions = trees().getSourcePositions();
        }
        return theSourcePositions;
    }

    int getStartPosition(Tree tree) {
        return (int) getSourcePositions().getStartPosition(targetCompilationUnit, tree);
    }

    int getEndPosition(Tree tree) {
        return (int) getSourcePositions().getEndPosition(targetCompilationUnit, tree);
    }

    Range treeToRange(Tree tree) {
        return new Range(getStartPosition(tree), getEndPosition(tree));
    }

    Range treeListToRange(List<? extends Tree> treeList) {
        int start = Integer.MAX_VALUE;
        int end = -1;
        for (Tree t : treeList) {
            int tstart = getStartPosition(t);
            int tend = getEndPosition(t);
            if (tstart < start) {
                start = tstart;
            }
            if (tend > end) {
                end = tend;
            }
        }
        if (start == Integer.MAX_VALUE) {
            return null;
        }
        return new Range(start, end);
    }

    MethodTree method(MethodSnippet msn) {
        if (targetClass == null) {
            return null;
        }
        OuterWrap ow = msn.outerWrap();
        if (!(ow instanceof OuterSnippetsClassWrap)) {
            return null;
        }
        int ordinal = ((OuterSnippetsClassWrap) ow).ordinal(msn);
        if (ordinal < 0) {
            return null;
        }
        int count = 0;
        String name = msn.name();
        for (Tree mem : targetClass.getMembers()) {
            if (mem.getKind() == Tree.Kind.METHOD) {
                MethodTree mt = (MethodTree) mem;
                if (mt.getName().toString().equals(name)) {
                    if (count == ordinal) {
                        return mt;
                    }
                    ++count;
                }
            }
        }
        return null;
    }

    StatementTree firstStatement() {
        if (targetClass != null) {
            for (Tree mem : targetClass.getMembers()) {
                if (mem.getKind() == Tree.Kind.METHOD) {
                    MethodTree mt = (MethodTree) mem;
                    if (isDoIt(mt.getName())) {
                        List<? extends StatementTree> stmts = mt.getBody().getStatements();
                        if (!stmts.isEmpty()) {
                            return stmts.get(0);
                        }
                    }
                }
            }
        }
        return null;
    }

    VariableTree firstVariable() {
        if (targetClass != null) {
            for (Tree mem : targetClass.getMembers()) {
                if (mem.getKind() == Tree.Kind.VARIABLE) {
                    VariableTree vt = (VariableTree) mem;
                    return vt;
                }
            }
        }
        return null;
    }

    String typeOfMethod(MethodSnippet msn) {
        Tree unitTree = method(msn);
        if (unitTree instanceof JCMethodDecl) {
            JCMethodDecl mtree = (JCMethodDecl) unitTree;
            Type mt = types().erasure(mtree.type);
            if (mt instanceof MethodType) {
                return signature(types(), (MethodType) mt);
            }
        }
        return null;
    }

    static String signature(Types types, MethodType mt) {
        TDSignatureGenerator sg = new TDSignatureGenerator(types);
        sg.assembleSig(mt);
        return sg.toString();
    }

    public static String printType(AnalyzeTask at, JShell state, TypeMirror type) {
        Type typeImpl = (Type) type;
        try {
            TypePrinter tp = new TypePrinter(at.messages(), at.types(),
                    state.maps::fullClassNameAndPackageToClass, true, AnonymousTypeKind.DISPLAY);
            return tp.toString(typeImpl);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Signature Generation
     */
    private static class TDSignatureGenerator extends Types.SignatureGenerator {

        /**
         * An output buffer for type signatures.
         */
        StringBuilder sb = new StringBuilder();

        TDSignatureGenerator(Types types) {
            super(types);
        }

        @Override
        protected void append(char ch) {
            sb.append(ch);
        }

        @Override
        protected void append(byte[] ba) {
            sb.append(new String(ba));
        }

        @Override
        protected void append(Name name) {
            sb.append(name);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
}
