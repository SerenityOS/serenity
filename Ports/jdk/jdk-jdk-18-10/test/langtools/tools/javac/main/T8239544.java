/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8239544
 * @summary Javac does not respect should-stop.ifNoError policy to stop after CompileState PARSE, ENTER and PROCESS
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main T8239544
 */
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.processing.PrintingProcessor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.processing.Processor;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class T8239544 {

    private static final String[] TESTED_COMPILE_POLICIES = {"simple", "byfile", "bytodo"};
    private static final CompileState[] TESTED_COMPILE_STATES =  {CompileState.INIT, CompileState.PARSE, CompileState.ENTER, CompileState.PROCESS,
        CompileState.ATTR, CompileState.FLOW, CompileState.TRANSTYPES, CompileState.TRANSPATTERNS, CompileState.UNLAMBDA, CompileState.LOWER}; //everything except GENERATE

    public static void main(String... args) throws IOException {
        var f = new SimpleJavaFileObject(URI.create("TestLambdaClass.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return "@Deprecated public class TestLambdaClass {{new Thread(() -> {});}}";
            }
        };
        for (String compilePolicy : TESTED_COMPILE_POLICIES) {
            for (CompileState stop : TESTED_COMPILE_STATES) {
                var ctx = new Context();
                var opt = Options.instance(ctx);
                opt.put("should-stop.ifNoError", stop.name());
                opt.put("compilePolicy", compilePolicy);
                opt.put("debug.dumpLambdaToMethodStats", "true");
                var compiler = new JavaCompiler(ctx) {

                    private CompileState reachedState = CompileState.INIT;

                    private CompileState getLatestState() {
                        for (CompileState cs : compileStates.values()) {
                            if (cs.isAfter(reachedState)) reachedState = cs;
                        }
                        return reachedState;
                    }

                    @Override
                    public List<JCTree.JCCompilationUnit> parseFiles(Iterable<JavaFileObject> fileObjects, boolean force) {
                        var res = super.parseFiles(fileObjects, force);
                        if (res.size() > 0 && CompileState.ENTER.isAfter(reachedState)) reachedState = CompileState.PARSE;
                        return res;
                    }

                    @Override
                    public List<JCTree.JCCompilationUnit> enterTrees(List<JCTree.JCCompilationUnit> roots) {
                        var res = super.enterTrees(roots);
                        if (res.size() > 0 && CompileState.ENTER.isAfter(reachedState)) reachedState = CompileState.ENTER;
                        return res;
                    }

                    @Override
                    public void initProcessAnnotations(Iterable<? extends Processor> processors, Collection<? extends JavaFileObject> initialFiles, Collection<String> initialClassNames) {
                        new JavacProcessingEnvironment(context) {
                            @Override
                            public boolean doProcessing(List<JCTree.JCCompilationUnit> roots, List<Symbol.ClassSymbol> classSymbols, Iterable<? extends Symbol.PackageSymbol> pckSymbols, Log.DeferredDiagnosticHandler deferredDiagnosticHandler) {
                                if (roots.size() > 0 && CompileState.PROCESS.isAfter(reachedState)) reachedState = CompileState.PROCESS;
                                return super.doProcessing(roots, classSymbols, pckSymbols, deferredDiagnosticHandler);
                            }
                        };
                        super.initProcessAnnotations(processors, initialFiles, initialClassNames);
                    }
                };
                compiler.compile(List.of(f), List.nil(), Collections.singleton(new PrintingProcessor()), List.nil());
                if (compiler.errorCount() > 0) {
                    throw new AssertionError();
                }
                if (!stop.equals(compiler.getLatestState())) {
                    throw new AssertionError("Compiler with compilePolicy=" + compilePolicy + " expected to stop at " + stop + " but " + compiler.reachedState + " has been reached");
                }
            }
        }
    }
}
