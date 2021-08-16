/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;

import jdk.jshell.SourceCodeAnalysis;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static jdk.jshell.SourceCodeAnalysis.Completeness.*;

public class CompletenessStressTest extends KullaTesting {
    public final static String JDK_ROOT_SRC_PROP = "jdk.root.src";
    public final static String JDK_ROOT_SRC;

    static {
        JDK_ROOT_SRC = System.getProperty(JDK_ROOT_SRC_PROP);
    }

    public File getSourceFile(String fileName) {
        for (File dir : getDirectoriesToTest()) {
            File file = new File(dir, fileName);
            if (file.exists()) {
                return file;
            }
        }
        throw new AssertionError("File not found: " + fileName);
    }

    public File[] getDirectoriesToTest() {
        return new File[]{
                new File(JDK_ROOT_SRC, "langtools/src"),
                new File(JDK_ROOT_SRC, "jaxp/src"),
                new File(JDK_ROOT_SRC, "jaxws/src"),
                new File(JDK_ROOT_SRC, "jdk/src"),
                new File(JDK_ROOT_SRC, "corba/src")
        };
    }

    @DataProvider(name = "crawler")
    public Object[][] dataProvider() throws IOException {
        File[] srcDirs = getDirectoriesToTest();
        List<String[]> list = new ArrayList<>();
        for (File srcDir : srcDirs) {
            String srcDirName = srcDir.getAbsolutePath();
            // this is just to obtain pretty test names for testng tests
            List<String[]> a = Files.walk(Paths.get(srcDirName))
                    .map(Path::toFile)
                    .map(File::getAbsolutePath)
                    .filter(n -> n.endsWith(".java"))
                    .map(n -> n.replace(srcDirName, ""))
                    .map(n -> new String[]{n})
                    .collect(Collectors.toList());
            if (a.isEmpty()) {
                throw new AssertionError("Java sources have not been found in directory: " + srcDirName);
            }
            list.addAll(a);
        }
        return list.toArray(new String[list.size()][]);
    }

    @Test(dataProvider = "crawler")
    public void testFile(String fileName) throws IOException {
        File file = getSourceFile(fileName);
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        boolean success = true;
        StringWriter writer = new StringWriter();
        writer.write("Testing : " + file.toString() + "\n");
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, fileManager, null, null, null, compilationUnits);
        Iterable<? extends CompilationUnitTree> asts = task.parse();
        Trees trees = Trees.instance(task);
        SourcePositions sp = trees.getSourcePositions();

        for (CompilationUnitTree cut : asts) {
            for (ImportTree imp : cut.getImports()) {
                success &= testStatement(writer, sp, text, cut, imp);
            }
            for (Tree decl : cut.getTypeDecls()) {
                success &= testStatement(writer, sp, text, cut, decl);
                if (decl instanceof ClassTree) {
                    ClassTree ct = (ClassTree) decl;
                    for (Tree mem : ct.getMembers()) {
                        if (mem instanceof MethodTree) {
                            MethodTree mt = (MethodTree) mem;
                            BlockTree bt = mt.getBody();
                            // No abstract methods or constructors
                            if (bt != null && mt.getReturnType() != null) {
                                // The modifiers synchronized, abstract, and default are not allowed on
                                // top-level declarations and are errors.
                                Set<Modifier> modifier = mt.getModifiers().getFlags();
                                if (!modifier.contains(Modifier.ABSTRACT)
                                        && !modifier.contains(Modifier.SYNCHRONIZED)
                                        && !modifier.contains(Modifier.DEFAULT)) {
                                    success &= testStatement(writer, sp, text, cut, mt);
                                }
                                testBlock(writer, sp, text, cut, bt);
                            }
                        }
                    }
                }
            }
        }
        fileManager.close();
        if (!success) {
            throw new AssertionError(writer.toString());
        }
    }

    private boolean isLegal(StatementTree st) {
        return !(st instanceof ReturnTree) &&
                !(st instanceof ContinueTree) && !(st instanceof BreakTree);
    }

    private boolean testBranch(StringWriter writer, SourcePositions sp, String text, CompilationUnitTree cut, StatementTree statementTree) {
        if (statementTree instanceof BlockTree) {
            return testBlock(writer, sp, text, cut, (BlockTree) statementTree);
        } else if (isLegal(statementTree)) {
            return testStatement(writer, sp, text, cut, statementTree);
        }
        return true;
    }

    private boolean testBlock(StringWriter writer, SourcePositions sp, String text, CompilationUnitTree cut, BlockTree blockTree) {
        boolean success = true;
        for (StatementTree st : blockTree.getStatements()) {
            if (isLegal(st)) {
                success &= testStatement(writer, sp, text, cut, st);
            }
            if (st instanceof IfTree) {
                IfTree ifTree = (IfTree) st;
                success &= testBranch(writer, sp, text, cut, ifTree.getThenStatement());
                success &= testBranch(writer, sp, text, cut, ifTree.getElseStatement());
            } else if (st instanceof WhileLoopTree) {
                WhileLoopTree whileLoopTree = (WhileLoopTree) st;
                success &= testBranch(writer, sp, text, cut, whileLoopTree.getStatement());
            } else if (st instanceof DoWhileLoopTree) {
                DoWhileLoopTree doWhileLoopTree = (DoWhileLoopTree) st;
                success &= testBranch(writer, sp, text, cut, doWhileLoopTree.getStatement());
            } else if (st instanceof ForLoopTree) {
                ForLoopTree forLoopTree = (ForLoopTree) st;
                success &= testBranch(writer, sp, text, cut, forLoopTree.getStatement());
            } else if (st instanceof LabeledStatementTree) {
                LabeledStatementTree labelTree = (LabeledStatementTree) st;
                success &= testBranch(writer, sp, text, cut, labelTree.getStatement());
            } else if (st instanceof SwitchTree) {
                SwitchTree switchTree = (SwitchTree) st;
                for (CaseTree caseTree : switchTree.getCases()) {
                    for (StatementTree statementTree : caseTree.getStatements()) {
                        success &= testBranch(writer, sp, text, cut, statementTree);
                    }
                }
            }
        }
        return success;
    }

    private boolean testStatement(StringWriter writer, SourcePositions sp, String text, CompilationUnitTree cut, Tree statement) {
        if (statement == null) {
            return true;
        }
        int start = (int) sp.getStartPosition(cut, statement);
        int end = (int) sp.getEndPosition(cut, statement);
        char ch = text.charAt(end - 1);
        SourceCodeAnalysis.Completeness expected = COMPLETE;
        LineMap lineMap = cut.getLineMap();
        int row = (int) lineMap.getLineNumber(start);
        int column = (int) lineMap.getColumnNumber(start);
        switch (ch) {
            case ',':
            case ';':
                expected = (statement instanceof ExpressionStatementTree)
                        ? COMPLETE
                        : COMPLETE_WITH_SEMI;
                --end;
                break;
            case '}':
                break;
            default:
                writer.write(String.format("Unexpected end: row %d, column %d: '%c' -- %s\n",
                        row, column, ch, text.substring(start, end)));
                return true;
        }
        String unit = text.substring(start, end);
        SourceCodeAnalysis.CompletionInfo ci = getAnalysis().analyzeCompletion(unit);
        if (ci.completeness() != expected) {
            if (expected == COMPLETE_WITH_SEMI && (ci.completeness() == CONSIDERED_INCOMPLETE || ci.completeness() == EMPTY)) {
                writer.write(String.format("Empty statement: row %d, column %d: -- %s\n",
                        start, end, unit));
            } else {
                writer.write(String.format("Expected %s got %s: '%s'  row %d, column %d: -- %s\n",
                        expected, ci.completeness(), unit, row, column, unit));
                return false;
            }
        }
        return true;
    }
}
