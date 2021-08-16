/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013789
 * @summary Compiler should emit bridges in interfaces
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper.DiagnosticSourceUnwrapper;
import com.sun.tools.javac.util.JCDiagnostic;

import java.io.File;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class TestMetafactoryBridges {

    static int checkCount = 0;

    enum ClasspathKind {
        NONE(),
        B7(7, ClassKind.B),
        A7(7, ClassKind.A),
        B8(8, ClassKind.B),
        A8(8, ClassKind.A);

        int version;
        ClassKind ck;

        ClasspathKind() {
            this(-1, null);
        }

        ClasspathKind(int version, ClassKind ck) {
            this.version = version;
            this.ck = ck;
        }
    }

    enum PreferPolicy {
        SOURCE("-Xprefer:source"),
        NEWER("-Xprefer:newer");

        String preferOpt;

        PreferPolicy(String preferOpt) {
            this.preferOpt = preferOpt;
        }
    }

    enum SourcepathKind {
        NONE,
        A(ClassKind.A),
        B(ClassKind.B),
        C(ClassKind.C),
        AB(ClassKind.A, ClassKind.B),
        BC(ClassKind.B, ClassKind.C),
        AC(ClassKind.A, ClassKind.C),
        ABC(ClassKind.A, ClassKind.B, ClassKind.C);

        List<ClassKind> sources;

        SourcepathKind(ClassKind... sources) {
            this.sources = Arrays.asList(sources);
        }
    }

    enum SourceSet {
        ALL() {
            @Override
            List<List<ClassKind>> permutations() {
                return Arrays.asList(
                    Arrays.asList(ClassKind.A, ClassKind.B, ClassKind.C),
                    Arrays.asList(ClassKind.A, ClassKind.B, ClassKind.C),
                    Arrays.asList(ClassKind.B, ClassKind.A, ClassKind.C),
                    Arrays.asList(ClassKind.B, ClassKind.C, ClassKind.A),
                    Arrays.asList(ClassKind.C, ClassKind.A, ClassKind.B),
                    Arrays.asList(ClassKind.C, ClassKind.B, ClassKind.A)
                );
            }
        },
        AC() {
            @Override
            List<List<ClassKind>> permutations() {
                return Arrays.asList(
                    Arrays.asList(ClassKind.A, ClassKind.C),
                    Arrays.asList(ClassKind.C, ClassKind.A)
                );
            }
        },
        C() {
            @Override
            List<List<ClassKind>> permutations() {
                return Arrays.asList(Arrays.asList(ClassKind.C));
            }
        };

        abstract List<List<ClassKind>> permutations();
    }

    enum ClassKind {
        A("A", "interface A { Object m(); }"),
        B("B", "interface B extends A { Integer m(); }", A),
        C("C", "class C { B b = ()->42; }", A, B);

        String name;
        String source;
        ClassKind[] deps;

        ClassKind(String name, String source, ClassKind... deps) {
            this.name = name;
            this.source = source;
            this.deps = deps;
        }
    }

    public static void main(String... args) throws Exception {
        String SCRATCH_DIR = System.getProperty("user.dir");
        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();

        int n = 0;
        for (SourceSet ss : SourceSet.values()) {
            for (List<ClassKind> sources : ss.permutations()) {
                for (SourcepathKind spKind : SourcepathKind.values()) {
                    for (ClasspathKind cpKind : ClasspathKind.values()) {
                        for (PreferPolicy pp : PreferPolicy.values()) {
                            Set<ClassKind> deps = EnumSet.noneOf(ClassKind.class);
                            if (cpKind.ck != null) {
                                deps.add(cpKind.ck);
                            }
                            deps.addAll(sources);
                            if (deps.size() < 3) continue;
                            File testDir = new File(SCRATCH_DIR, "test" + n);
                            testDir.mkdir();
                            try (PrintWriter debugWriter = new PrintWriter(new File(testDir, "debug.txt"))) {
                                new TestMetafactoryBridges(testDir, sources, spKind, cpKind, pp, debugWriter).run(comp);
                                n++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Total check executed: " + checkCount);
    }

    File testDir;
    List<ClassKind> sources;
    SourcepathKind spKind;
    ClasspathKind cpKind;
    PreferPolicy pp;
    PrintWriter debugWriter;
    DiagnosticChecker diagChecker;

    TestMetafactoryBridges(File testDir, List<ClassKind>sources, SourcepathKind spKind,
            ClasspathKind cpKind, PreferPolicy pp, PrintWriter debugWriter) {
        this.testDir = testDir;
        this.sources = sources;
        this.spKind = spKind;
        this.cpKind = cpKind;
        this.pp = pp;
        this.debugWriter = debugWriter;
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        final String source;

        public JavaSource(ClassKind ck) {
            super(URI.create(String.format("myfo:/%s.java", ck.name)), JavaFileObject.Kind.SOURCE);
            this.source = ck.source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool) throws Exception {
        File classesDir = new File(testDir, "classes");
        File outDir = new File(testDir, "out");
        File srcDir = new File(testDir, "src");
        classesDir.mkdir();
        outDir.mkdir();
        srcDir.mkdir();

        debugWriter.append(testDir.getName() + "\n");
        debugWriter.append("sources = " + sources + "\n");
        debugWriter.append("spKind = " + spKind  + "\n");
        debugWriter.append("cpKind = " + cpKind + "\n");
        debugWriter.append("preferPolicy = " + pp.preferOpt + "\n");

        //step 1 - prepare sources (older!!)
        debugWriter.append("Preparing sources\n");
        for (ClassKind ck : spKind.sources) {
            //skip sources explicitly provided on command line
            if (!sources.contains(ck)) {
                debugWriter.append("Copy " + ck.name + ".java to" + srcDir.getAbsolutePath() + "\n");
                File dest = new File(srcDir, ck.name + ".java");
                PrintWriter pw = new PrintWriter(dest);
                pw.append(ck.source);
                pw.close();
            }
        }

        //step 2 - prepare classes
        debugWriter.append("Preparing classes\n");
        if (cpKind != ClasspathKind.NONE) {
            List<JavaSource> sources = new ArrayList<>();
            ClassKind toRemove = null;
            sources.add(new JavaSource(cpKind.ck));
            if (cpKind.ck.deps.length != 0) {
                //at most only one dependency
                toRemove = cpKind.ck.deps[0];
                sources.add(new JavaSource(toRemove));
            }
            JavacTask ct = (JavacTask)tool.getTask(debugWriter, null, null,
                    Arrays.asList("-d", classesDir.getAbsolutePath(), "-source", String.valueOf(cpKind.version)), null, sources);
            try {
                ct.generate();
                if (toRemove != null) {
                    debugWriter.append("Remove " + toRemove.name + ".class from" + classesDir.getAbsolutePath() + "\n");
                    File fileToRemove = new File(classesDir, toRemove.name + ".class");
                    fileToRemove.delete();
                }
            } catch (Throwable ex) {
                throw new AssertionError("Error thrown when generating side-classes");
            }
        }

        //step 3 - compile
        debugWriter.append("Compiling test\n");
        List<JavaSource> sourcefiles = new ArrayList<>();
        for (ClassKind ck : sources) {
            sourcefiles.add(new JavaSource(ck));
        }
        JavacTask ct = (JavacTask)tool.getTask(debugWriter, null, diagChecker,
                    Arrays.asList("--debug=dumpLambdaToMethodStats", "-d", outDir.getAbsolutePath(),
                                  "-sourcepath", srcDir.getAbsolutePath(),
                                  "-classpath", classesDir.getAbsolutePath(),
                                  pp.preferOpt), null, sourcefiles);
        try {
            ct.generate();
        } catch (Throwable ex) {
            throw new AssertionError("Error thrown when compiling test case");
        }
        check();
    }

    void check() {
        checkCount++;
        if (diagChecker.errorFound) {
            throw new AssertionError("Unexpected compilation failure");
        }

        boolean altMetafactory =
                cpKind == ClasspathKind.B7 &&
                !sources.contains(ClassKind.B) &&
                (pp == PreferPolicy.NEWER || !spKind.sources.contains(ClassKind.B));

        if (altMetafactory != diagChecker.altMetafactory) {
            throw new AssertionError("Bad metafactory detected - expected altMetafactory: " + altMetafactory +
                    "\ntest: " + testDir);
        }
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean altMetafactory = false;
        boolean errorFound = false;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            } else if (statProcessor.matches(diagnostic)) {
                statProcessor.process(diagnostic);
            }
        }

        abstract class DiagnosticProcessor {

            List<String> codes;
            Diagnostic.Kind kind;

            public DiagnosticProcessor(Kind kind, String... codes) {
                this.codes = Arrays.asList(codes);
                this.kind = kind;
            }

            abstract void process(Diagnostic<? extends JavaFileObject> diagnostic);

            boolean matches(Diagnostic<? extends JavaFileObject> diagnostic) {
                return (codes.isEmpty() || codes.contains(diagnostic.getCode())) &&
                        diagnostic.getKind() == kind;
            }

            JCDiagnostic asJCDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
                if (diagnostic instanceof JCDiagnostic) {
                    return (JCDiagnostic)diagnostic;
                } else if (diagnostic instanceof DiagnosticSourceUnwrapper) {
                    return ((DiagnosticSourceUnwrapper)diagnostic).d;
                } else {
                    throw new AssertionError("Cannot convert diagnostic to JCDiagnostic: " + diagnostic.getClass().getName());
                }
            }
        }

        DiagnosticProcessor statProcessor = new DiagnosticProcessor(Kind.NOTE,
                "compiler.note.lambda.stat",
                "compiler.note.mref.stat",
                "compiler.note.mref.stat.1") {
            @Override
            void process(Diagnostic<? extends JavaFileObject> diagnostic) {
                JCDiagnostic diag = asJCDiagnostic(diagnostic);
                if ((Boolean)diag.getArgs()[0]) {
                    altMetafactory = true;
                }
            }
        };
    }
}
