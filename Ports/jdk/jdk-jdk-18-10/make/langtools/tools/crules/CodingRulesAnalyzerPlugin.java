/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

package crules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.Log;

/*
 * This code must be run in a context that provides
 * access to the following javac internal packages:
 *      com.sun.tools.javac.api
 *      com.sun.tools.javac.tree
 *      com.sun.tools.javac.util
 */
public class CodingRulesAnalyzerPlugin implements Plugin {

    protected Log log;
    protected Trees trees;

    @DefinedBy(Api.COMPILER_TREE)
    public void init(JavacTask task, String... args) {
        BasicJavacTask impl = (BasicJavacTask)task;
        Context context = impl.getContext();
        log = Log.instance(context);
        trees = Trees.instance(task);
        task.addTaskListener(new PostAnalyzeTaskListener(
                new MutableFieldsAnalyzer(task),
                new AssertCheckAnalyzer(task),
                new DefinedByAnalyzer(task),
                new LegacyLogMethodAnalyzer(task)
        ));
    }

    private void addExports(String moduleName, String... packageNames) {
        for (String packageName : packageNames) {
            try {
                ModuleLayer layer = ModuleLayer.boot();
                Optional<Module> m = layer.findModule(moduleName);
                if (!m.isPresent())
                    throw new Error("module not found: " + moduleName);
                m.get().addExports(packageName, getClass().getModule());
            } catch (Exception e) {
                throw new Error("failed to add exports for " + moduleName + "/" + packageName);
            }
        }
    }

    public class PostAnalyzeTaskListener implements TaskListener {
        private final Map<Kind, List<AbstractCodingRulesAnalyzer>> analyzers = new HashMap<>();

        public PostAnalyzeTaskListener(AbstractCodingRulesAnalyzer... analyzers) {
            for (AbstractCodingRulesAnalyzer analyzer : analyzers) {
                List<AbstractCodingRulesAnalyzer> currentAnalyzers = this.analyzers.get(analyzer.eventKind);

                if (currentAnalyzers == null) {
                    this.analyzers.put(analyzer.eventKind, currentAnalyzers = new ArrayList<>());
                }

                currentAnalyzers.add(analyzer);
            }
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public void started(TaskEvent taskEvent) {}

        @Override @DefinedBy(Api.COMPILER_TREE)
        public void finished(TaskEvent taskEvent) {
            List<AbstractCodingRulesAnalyzer> currentAnalyzers = this.analyzers.get(taskEvent.getKind());

            if (currentAnalyzers != null) {
                TypeElement typeElem = taskEvent.getTypeElement();
                Tree tree = trees.getTree(typeElem);
                if (tree != null) {
                    JavaFileObject prevSource = log.currentSourceFile();
                    try {
                        log.useSource(taskEvent.getCompilationUnit().getSourceFile());
                        for (AbstractCodingRulesAnalyzer analyzer : currentAnalyzers) {
                            analyzer.treeVisitor.scan((JCTree)tree);
                        }
                    } finally {
                        log.useSource(prevSource);
                    }
                }
            }
        }
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public String getName() {
        return "coding_rules";
    }

}
