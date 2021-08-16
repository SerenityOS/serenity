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
 * @bug 6877202 6986246
 * @summary Elements.getDocComment() is not getting JavaDocComments
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor TestDocComments
 * @run main TestDocComments
 */

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;

/*
 * For a mixture of pre-existing and generated source files, ensure that we can
 * get the doc comments.
 * The test uses both a standard ElementScanner to find all the elements being
 * processed, and a TreeScanner to find all the local and anonymous inner classes
 * as well.
 * And, because the relevant code paths in the compiler are different for
 * command line and JSR 199 invocation, the test covers both ways of invoking the
 * compiler.
 */

@SupportedOptions("scan")
public class TestDocComments extends JavacTestingAbstractProcessor {
    enum CompileKind { API, CMD };
    enum ScanKind { TREE, ELEMENT };

    // ----- Main test driver: invoke compiler for the various test cases ------

    public static void main(String... args) throws Exception {
        for (CompileKind ck: CompileKind.values()) {
            for (ScanKind sk: ScanKind.values()) {
                try {
                    test(ck, sk);
                } catch (IOException e) {
                    error(e.toString());
                }
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    static void test(CompileKind ck, ScanKind sk) throws IOException {
        String testClasses = System.getProperty("test.class.path");
        String testSrc = System.getProperty("test.src");
        File testDir = new File("test." + ck + "." + sk);
        testDir.mkdirs();
        String[] opts = {
            "-d", testDir.getPath(),
            "-implicit:none",
            "-processor", TestDocComments.class.getName(),
            "-processorpath", testClasses,
            //"-XprintRounds",
            "-Ascan=" + sk
        };
        File[] files = {
            new File(testSrc, "a/First.java")
        };

        if (ck == CompileKind.API)
            test_javac_api(opts, files);
        else
            test_javac_cmd(opts, files);
    }

    static void test_javac_api(String[] opts, File[] files) throws IOException {
        System.err.println("test javac api: " + Arrays.asList(opts) + " " + Arrays.asList(files));
        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic diagnostic) {
                error(diagnostic.toString());
            }
        };
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(files);
            JavacTask t = (JavacTask) c.getTask(null, fm, dl, Arrays.asList(opts), null, units);
            t.parse();
            t.analyze();
        }
    }

    static void test_javac_cmd(String[] opts, File[] files) {
        System.err.println("test javac cmd: " + Arrays.asList(opts) + " " + Arrays.asList(files));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        List<String> args = new ArrayList<String>(Arrays.asList(opts));
        for (File f: files)
            args.add(f.getPath());
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0)
            System.err.println(out);
        if (rc > 0)
            error("Compilation failed: rc=" + rc);
    }

    static void error(String msg) {
        System.err.println(msg);
        errors++;
        //throw new Error(msg);
    }

    static int errors;

    // ----- Annotation processor: scan for elements and check doc comments ----

    Map<String,String> options;
    Trees trees;
    ScanKind skind;

    int round = 0;

    @Override
    public void init(ProcessingEnvironment pEnv) {
        super.init(pEnv);
        options = pEnv.getOptions();
        trees = Trees.instance(processingEnv);
        skind = ScanKind.valueOf(options.get("scan"));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;

        // Scan elements using an appropriate scanner, and for each element found,
        // call check(Element e) to verify the doc comment on that element
        for (Element e: roundEnv.getRootElements()) {
            System.err.println("scan " + skind + " " + e.getKind() + " " + e.getSimpleName());
            if (skind == ScanKind.TREE) {
                new TestTreeScanner().scan(trees.getPath(e), trees);
            }  else
                new TestElementScanner().scan(e);
        }

        // For a few rounds, generate new source files, so that we can check whether
        // doc comments are correctly handled in subsequent processing rounds
        final int MAX_ROUNDS = 3;
        if (round <= MAX_ROUNDS) {
            String pkg = "p";
            String currClass = "Gen" + round;
            String curr = pkg + "." + currClass;
            String next = (round < MAX_ROUNDS) ? (pkg + ".Gen" + (round + 1)) : "z.Last";
            StringBuilder text = new StringBuilder();
            text.append("package ").append(pkg).append(";\n");
            text.append("/** CLASS ").append(currClass).append(" */\n");
            text.append("public class ").append(currClass).append(" {\n");
            text.append("    /** CONSTRUCTOR <init> **/\n");
            text.append("    ").append(currClass).append("() { }\n");
            text.append("    /** FIELD x */\n");
            text.append("    ").append(next).append(" x;\n");
            text.append("    /** METHOD m */\n");
            text.append("    void m() { }\n");
            text.append("}\n");

            try {
                JavaFileObject fo = filer.createSourceFile(curr);
                Writer out = fo.openWriter();
                try {
                    out.write(text.toString());
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        return true;
    }

    /*
     * Check that the doc comment on an element is as expected.
     * This method is invoked for each element found by the scanners run by process.
     */
    void check(Element e) {
        System.err.println("Checking " + e);

        String dc = elements.getDocComment(e);
        System.err.println("   found " + dc);

        String expect = (e.getKind() + " " + e.getSimpleName()); // default

        Name name = e.getSimpleName();
        Element encl = e.getEnclosingElement();
        Name enclName = encl.getSimpleName();
        ElementKind enclKind = encl.getKind();
        switch (e.getKind()) {
            case PARAMETER:
            case LOCAL_VARIABLE:
                // doc comments not retained for these elements
                expect = null;
                break;

            case CONSTRUCTOR:
                if (enclName.length() == 0 || enclKind == ElementKind.ENUM) {
                    // Enum constructor is synthetic
                    expect = null;
                }
                break;

            case METHOD:
                if (enclKind == ElementKind.ENUM
                        && (name.contentEquals("values") || name.contentEquals("valueOf"))) {
                    // synthetic enum methods
                    expect = null;
                }
                break;

            case CLASS:
                if (e.getSimpleName().length() == 0) {
                    // anon inner class
                    expect = null;
                }
                break;
        }

        System.err.println("  expect " + expect);

        if (dc == null ? expect == null : dc.trim().equals(expect))
            return;

        if (dc == null)
            messager.printMessage(Diagnostic.Kind.ERROR, "doc comment is null", e);
        else {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "unexpected comment: \"" + dc + "\", expected \"" + expect + "\"", e);
        }
    }

    // ----- Scanners to find elements -----------------------------------------

    class TestElementScanner extends ElementScanner<Void, Void> {
        @Override
        public Void visitExecutable(ExecutableElement e, Void p) {
            check(e);
            return super.visitExecutable(e, p);
        }
        @Override
        public Void visitType(TypeElement e, Void p) {
            check(e);
            return super.visitType(e, p);
        }
        @Override
        public Void visitVariable(VariableElement e, Void p) {
            check(e);
            return super.visitVariable(e, p);
        }
    }

    class TestTreeScanner extends TreePathScanner<Void,Trees> {
        @Override
        public Void visitClass(ClassTree tree, Trees trees) {
            check(trees.getElement(getCurrentPath()));
            return super.visitClass(tree, trees);
        }
        @Override
        public Void visitMethod(MethodTree tree, Trees trees) {
            check(trees.getElement(getCurrentPath()));
            return super.visitMethod(tree, trees);
        }
        @Override
        public Void visitVariable(VariableTree tree, Trees trees) {
            check(trees.getElement(getCurrentPath()));
            return super.visitVariable(tree, trees);
        }
    }
}
