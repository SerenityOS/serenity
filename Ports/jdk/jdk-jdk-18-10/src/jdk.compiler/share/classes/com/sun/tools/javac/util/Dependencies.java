/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.GraphUtils.DependencyKind;
import com.sun.tools.javac.util.GraphUtils.DotVisitor;
import com.sun.tools.javac.util.GraphUtils.NodeVisitor;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.tools.JavaFileObject;

/**
 *  This class is used to track dependencies in the javac symbol completion process.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class Dependencies {

    protected static final Context.Key<Dependencies> dependenciesKey = new Context.Key<>();

    public static Dependencies instance(Context context) {
        Dependencies instance = context.get(dependenciesKey);
        if (instance == null) {
            //use a do-nothing implementation in case no other implementation has been set by preRegister
            instance = new DummyDependencies(context);
        }
        return instance;
    }

    protected Dependencies(Context context) {
        context.put(dependenciesKey, this);
    }

    /**
     * Push a new completion node on the stack.
     */
    public abstract void push(ClassSymbol s, CompletionCause phase);

    /**
     * Remove current dependency node from the stack.
     */
    public abstract void pop();

    public enum CompletionCause implements GraphUtils.DependencyKind {
        CLASS_READER,
        HEADER_PHASE,
        HIERARCHY_PHASE,
        IMPORTS_PHASE,
        MEMBER_ENTER,
        RECORD_PHASE,
        MEMBERS_PHASE,
        PERMITS_PHASE,
        OTHER;
    }

    /**
     * This class creates a graph of all dependencies as symbols are completed;
     * when compilation finishes, the resulting dependency graph is then dumped
     * onto a dot file. Several options are provided to customize the output of the graph.
     */
    public static class GraphDependencies extends Dependencies implements Closeable, Completer {

        /**
         * set of enabled dependencies modes
         */
        private EnumSet<DependenciesMode> dependenciesModes;

        /**
         * file in which the dependency graph should be written
         */
        private String dependenciesFile;

        /**
         * Register a Context.Factory to create a Dependencies.
         */
        public static void preRegister(Context context) {
            context.put(dependenciesKey, (Context.Factory<Dependencies>) GraphDependencies::new);
        }

        /**
         * Build a Dependencies instance.
         */
        GraphDependencies(Context context) {
            super(context);
            //fetch filename
            Options options = Options.instance(context);
            String[] modes = options.get("debug.completionDeps").split(",");
            for (String mode : modes) {
                if (mode.startsWith("file=")) {
                    dependenciesFile = mode.substring(5);
                }
            }
            //parse modes
            dependenciesModes = DependenciesMode.getDependenciesModes(modes);
            //add to closeables
            JavaCompiler compiler = JavaCompiler.instance(context);
            compiler.closeables = compiler.closeables.prepend(this);
        }

        enum DependenciesMode {
            SOURCE("source"),
            CLASS("class"),
            REDUNDANT("redundant");

            final String opt;

            DependenciesMode(String opt) {
                this.opt = opt;
            }

            /**
             * This method is used to parse the {@code completionDeps} option.
             * Possible modes are separated by colon; a mode can be excluded by
             * prepending '-' to its name. Finally, the special mode 'all' can be used to
             * add all modes to the resulting enum.
             */
            static EnumSet<DependenciesMode> getDependenciesModes(String[] modes) {
                EnumSet<DependenciesMode> res = EnumSet.noneOf(DependenciesMode.class);
                Collection<String> args = Arrays.asList(modes);
                if (args.contains("all")) {
                    res = EnumSet.allOf(DependenciesMode.class);
                }
                for (DependenciesMode mode : values()) {
                    if (args.contains(mode.opt)) {
                        res.add(mode);
                    } else if (args.contains("-" + mode.opt)) {
                        res.remove(mode);
                    }
                }
                return res;
            }
        }

        /**
         * Class representing a node in the dependency graph.
         */
        public static abstract class Node extends GraphUtils.AbstractNode<ClassSymbol, Node>
                implements GraphUtils.DottableNode<ClassSymbol, Node> {
            /**
             * dependant nodes grouped by kind
             */
            EnumMap<CompletionCause, List<Node>> depsByKind;

            Node(ClassSymbol value) {
                super(value);
                this.depsByKind = new EnumMap<>(CompletionCause.class);
                for (CompletionCause depKind : CompletionCause.values()) {
                    depsByKind.put(depKind, new ArrayList<Node>());
                }
            }

            void addDependency(DependencyKind depKind, Node dep) {
                List<Node> deps = depsByKind.get(depKind);
                if (!deps.contains(dep)) {
                    deps.add(dep);
                }
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Node node && data.equals(node.data);
            }

            @Override
            public int hashCode() {
                return data.hashCode();
            }

            @Override
            public GraphUtils.DependencyKind[] getSupportedDependencyKinds() {
                return CompletionCause.values();
            }

            @Override
            public java.util.Collection<? extends Node> getDependenciesByKind(DependencyKind dk) {
                return depsByKind.get(dk);
            }

            @Override
            public Properties nodeAttributes() {
                Properties p = new Properties();
                p.put("label", DotVisitor.wrap(toString()));
                return p;
            }

            @Override
            public Properties dependencyAttributes(Node to, GraphUtils.DependencyKind dk) {
                Properties p = new Properties();
                p.put("label", dk);
                return p;
            }

            @Override
            public String toString() {
                return data.getQualifiedName().toString();
            }
        }

        /**
         * This is a dependency node used to model symbol completion requests.
         * Completion requests can come from either source or class.
         */
        public static class CompletionNode extends Node {

            /**
             * Completion kind (source vs. classfile)
             */
            enum Kind {
                /**
                 * Source completion request
                 */
                SOURCE("solid"),
                /**
                 * Classfile completion request
                 */
                CLASS("dotted");

                final String dotStyle;

                Kind(String dotStyle) {
                    this.dotStyle = dotStyle;
                }
            }

            final Kind ck;

            CompletionNode(ClassSymbol sym) {
                super(sym);
                //infer completion kind by looking at the symbol fields
                boolean fromClass = (sym.classfile == null && sym.sourcefile == null) ||
                        (sym.classfile != null && sym.classfile.getKind() == JavaFileObject.Kind.CLASS);
                ck = fromClass ?
                        CompletionNode.Kind.CLASS :
                        CompletionNode.Kind.SOURCE;
            }

            @Override
            public Properties nodeAttributes() {
                Properties p = super.nodeAttributes();
                p.put("style", ck.dotStyle);
                p.put("shape", "ellipse");
                return p;
            }

            public ClassSymbol getClassSymbol() {
                return data;
            }
        }

        /**
         * stack of dependency nodes currently being processed
         */
        Stack<Node> nodeStack = new Stack<>();

        /**
         * map containing all dependency nodes seen so far
         */
        Map<ClassSymbol, Node> dependencyNodeMap = new LinkedHashMap<>();

        @Override
        public void push(ClassSymbol s, CompletionCause phase) {
            Node n = new CompletionNode(s);
            if (n == push(n, phase)) {
                s.completer = this;
            }
        }

        /**
         * Push a new dependency on the stack.
         */
        protected Node push(Node newNode, CompletionCause cc) {
            Node cachedNode = dependencyNodeMap.get(newNode.data);
            if (cachedNode == null) {
                dependencyNodeMap.put(newNode.data, newNode);
            } else {
                newNode = cachedNode;
            }
            if (!nodeStack.isEmpty()) {
                Node currentNode = nodeStack.peek();
                currentNode.addDependency(cc, newNode);
            }
            nodeStack.push(newNode);
            return newNode;
        }

        @Override
        public void pop() {
            nodeStack.pop();
        }

        @Override
        public void close() throws IOException {
            if (!dependenciesModes.contains(DependenciesMode.REDUNDANT)) {
                //prune spurious edges
                new PruneVisitor().visit(dependencyNodeMap.values(), null);
            }
            if (!dependenciesModes.contains(DependenciesMode.CLASS)) {
                //filter class completions
                new FilterVisitor(CompletionNode.Kind.SOURCE).visit(dependencyNodeMap.values(), null);
            }
            if (!dependenciesModes.contains(DependenciesMode.SOURCE)) {
                //filter source completions
                new FilterVisitor(CompletionNode.Kind.CLASS).visit(dependencyNodeMap.values(), null);
            }
            if (dependenciesFile != null) {
                //write to file
                try (FileWriter fw = new FileWriter(dependenciesFile)) {
                    fw.append(GraphUtils.toDot(dependencyNodeMap.values(), "CompletionDeps", ""));
                }
            }
        }

        @Override
        public void complete(Symbol sym) throws CompletionFailure {
            push((ClassSymbol)sym, CompletionCause.OTHER);
            pop();
            sym.completer = this;
        }

        @Override
        public boolean isTerminal() {
            return true;
        }

        public Collection<Node> getNodes() {
            return dependencyNodeMap.values();
        }

        /**
         * This visitor is used to prune the graph from spurious edges using some heuristics.
         */
        private static class PruneVisitor extends NodeVisitor<ClassSymbol, Node, Void> {
            @Override
            public void visitNode(Node node, Void arg) {
                //do nothing
            }

            @Override
            public void visitDependency(GraphUtils.DependencyKind dk, Node from, Node to, Void arg) {
                //heuristic - skips dependencies that are likely to be fake
                if (from.equals(to)) {
                    to.depsByKind.get(dk).remove(from);
                }
            }
        }

        /**
         * This visitor is used to retain only completion nodes with given kind.
         */
        private class FilterVisitor extends NodeVisitor<ClassSymbol, Node, Void> {

            CompletionNode.Kind ck;

            private FilterVisitor(CompletionNode.Kind ck) {
                this.ck = ck;
            }

            @Override
            public void visitNode(Node node, Void arg) {
                if (node instanceof CompletionNode completionNode) {
                    if (completionNode.ck != ck) {
                        dependencyNodeMap.remove(node.data);
                    }
                }
            }

            @Override
            public void visitDependency(GraphUtils.DependencyKind dk, Node from, Node to, Void arg) {
                if (to instanceof CompletionNode completionNode) {
                    if (completionNode.ck != ck) {
                        from.depsByKind.get(dk).remove(to);
                    }
                }
            }
        }
    }

    /**
     * Dummy class to be used when dependencies options are not set. This keeps
     * performance cost of calling push/pop methods during completion marginally low.
     */
    private static class DummyDependencies extends Dependencies {

        private DummyDependencies(Context context) {
            super(context);
        }

        @Override
        public void push(ClassSymbol s, CompletionCause phase) {
            //do nothing
        }

        @Override
        public void pop() {
            //do nothing
        }
    }
}
