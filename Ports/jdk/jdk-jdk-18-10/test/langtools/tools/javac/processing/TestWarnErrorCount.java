/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7022337
 * @summary repeated warnings about bootclasspath not set
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor TestWarnErrorCount
 * @run main TestWarnErrorCount
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;

@SupportedOptions({"errKind", "msgrWarnKind", "javaWarnKind"})
public class TestWarnErrorCount extends JavacTestingAbstractProcessor {
    public static void main(String... args) throws Exception {
        new TestWarnErrorCount().run(args);
    }

    final int MAX_GEN = 10;
    final int ERROR_ROUND = MAX_GEN / 2; // when to generate error

    /**
     * Type of errors to generate in test case.
     */
    enum ErrorKind {
        /** No errors. */
        NONE,
        /** Source code errors. */
        JAVA,
        /** Errors reported to Messager. */
        MESSAGER,
        /** Error as a result of using -Werror. */
        WERROR,
    }

    /**
     * Frequency of warnings in test case.
     */
    enum WarnKind {
        /** No warnings. */
        NONE {
            boolean warn(int round) { return false; }
            int count(int start, int end) { return 0; }
        },
        /** Generate a warning if round count is a multiple of 2. */
        EVERY_TWO {
            boolean warn(int round) { return (round % 2) == 0; }
            int count(int start, int end) { return (end / 2) - ((start - 1)/ 2); }
        },
        /** Generate a warning if round count is a multiple of 3. */
        EVERY_THREE {
            boolean warn(int round) { return (round % 3) == 0; }
            int count(int start, int end) { return (end / 3) - ((start - 1)/ 3); }
        },
        /** Generate a warning every round. */
        ALL {
            boolean warn(int round) { return true; }
            int count(int start, int end) { return (end - start + 1); }
        };

        /** whether to generate a warning in round 'round'. */
        abstract boolean warn(int round);

        /** number of warnings generated in a range of rounds, inclusive. */
        abstract int count(int start, int end);
    }


    /**
     * Run test.
     * @param args provide ability to specify particular test cases for debugging.
     */
    void run(String... args) throws Exception {
        for (String arg: args) {
            if (arg.matches("[0-9]+")) {
                if (testCases == null)
                    testCases = new HashSet<Integer>();
                testCases.add(Integer.valueOf(arg));
            } else if (arg.equals("-stopOnError")) {
                stopOnError = true;
            } else
                throw new IllegalArgumentException(arg);
        }

        run ();

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    /**
     * Run test.
     */
    void run() throws Exception {
        for (ErrorKind ek: ErrorKind.values()) {
            for (WarnKind mwk: WarnKind.values()) {
                for (WarnKind jwk: WarnKind.values()) {
                    test(ek, mwk, jwk);
                    if (stopOnError && errors > 0)
                        throw new Exception(errors + " errors found");
                }
            }
        }
    }

    boolean stopOnError;
    Set<Integer> testCases;
    int testNum = 0;

    /**
     * Run a test case.
     * @param ek    The type of errors to generate
     * @param mwk   The frequency of Messager warnings to generate
     * @param jwk   The frequency of Java warnings to generate
     */
    void test(ErrorKind ek, WarnKind mwk, WarnKind jwk) {
        testNum++;

        if (testCases != null && !testCases.contains(testNum))
            return;

        System.err.println("Test " + testNum + ": ek:" + ek + " mwk:" + mwk + " jwk:" + jwk);

        File testDir = new File("test" + testNum);
        testDir.mkdirs();

        String myName = TestWarnErrorCount.class.getSimpleName();
        File testSrc = new File(System.getProperty("test.src"));
        File file = new File(testSrc, myName + ".java");

        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(
            "-XDrawDiagnostics",
            "-d", testDir.getPath(),
            "-processor", myName,
//            "-XprintRounds",
            "-Xlint:all,-path",
            "-AerrKind=" + ek,
            "-AmsgrWarnKind=" + mwk,
            "-AjavaWarnKind=" + jwk));
        if (ek == ErrorKind.WERROR)
            args.add("-Werror");
        args.add(file.getPath());

        String out = compile(args.toArray(new String[args.size()]));

        int errsFound = 0;
        int errsReported = 0;
        int warnsFound = 0;
        int warnsReported = 0;

        // Scan the output looking for messages of interest.

        for (String line: out.split("[\r\n]+")) {
            if (line.contains("compiler.err.")) {
                errsFound++;
            } else if (line.contains("compiler.warn.")) {
                warnsFound++;
            } else if (line.matches("[0-9]+ error(?:s?)")) {
                errsReported = Integer.valueOf(line.substring(0, line.indexOf("error")).trim());
            } else if (line.matches("[0-9]+ warning(?:s?)")) {
                warnsReported = Integer.valueOf(line.substring(0, line.indexOf("warning")).trim());
            }
        }

        // Compute the expected number of errors and warnings, based on
        // the test case parameters.
        // This is highly specific to the annotation processor below, and to
        // the files it generates.
        // Generally, the rules are:
        // -- errors stop annotation processing, allowing for one extra "last round"
        // -- messager warnings are immediate
        // -- javac warnings are not shown before the final compilation
        //      (FIXME?  -Werror does not stop processing for java warnings)
        int errsExpected;
        int msgrWarnsExpected;
        int javaWarnsExpected;
        switch (ek) {
            case NONE:
                errsExpected = 0;
                msgrWarnsExpected = mwk.count(1, 1 + MAX_GEN + 1);
                javaWarnsExpected = jwk.count(2, 1 + MAX_GEN);
                break;
            case MESSAGER:
                errsExpected = 1;
                msgrWarnsExpected = mwk.count(1, ERROR_ROUND + 1);
                javaWarnsExpected = 0;
                break;
            case JAVA:
                errsExpected = 1;
                msgrWarnsExpected = mwk.count(1, ERROR_ROUND + 1);
                javaWarnsExpected = 0;
                break;
            case WERROR:
                errsExpected = (mwk != WarnKind.NONE || jwk != WarnKind.NONE) ? 1 : 0;
                switch (mwk) {
                    case NONE:
                        msgrWarnsExpected = 0;
                        javaWarnsExpected = (jwk == WarnKind.NONE)
                                ? 0
                                : 1;  // this is surprising: javac only reports warning in first file
                        break;
                    case EVERY_TWO:
                        msgrWarnsExpected = mwk.count(1, 2 + 1);
                        javaWarnsExpected = 0;
                        break;
                    case EVERY_THREE:
                        msgrWarnsExpected = mwk.count(1, 3 + 1);
                        javaWarnsExpected = 0;
                        break;
                    case ALL:
                        msgrWarnsExpected = mwk.count(1, 1 + 1);
                        javaWarnsExpected = 0;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                break;
            default:
                throw new IllegalStateException();
        }

        int warnsExpected = msgrWarnsExpected + javaWarnsExpected;
        System.err.println("mwk: " + msgrWarnsExpected
                + ", jwk: " + javaWarnsExpected
                + ", total: " + warnsExpected);

        boolean ok;
        ok  = checkEqual("errors", "reported", errsFound, errsReported);
        ok &= checkEqual("errors", "expected", errsFound, errsExpected);
        ok &= checkEqual("warnings", "reported", warnsFound, warnsReported);
        ok &= checkEqual("warnings", "expected", warnsFound, warnsExpected);
        if (ok)
            System.err.println("OK");

        System.err.println();
    }

    String compile(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            System.err.println("compilation failed: rc=" + rc);
        return out;
    }

    boolean checkEqual(String l1, String l2, int i1, int i2) {
        if (i1 != i2)
            error("number of " + l1 + " found, " + i1 + ", does not match number " + l2 + ", " + i2);
        return (i1 == i2);
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors = 0;

    // ----- Annotation processor -----

    int round = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;

        ErrorKind ek = ErrorKind.valueOf(options.get("errKind"));
        WarnKind mwk = WarnKind.valueOf(options.get("msgrWarnKind"));
        WarnKind jwk = WarnKind.valueOf(options.get("javaWarnKind"));
        messager.printMessage(Diagnostic.Kind.NOTE,
                "Round " + round
                + " " + roundEnv.getRootElements()
                + ", last round: " + roundEnv.processingOver());
        messager.printMessage(Diagnostic.Kind.NOTE,
                "ek: " + ek + ", mwk: " + mwk + ", jwk: " + jwk);

        if (round <= MAX_GEN && !roundEnv.processingOver())
            generate("Gen" + round,
                    (ek == ErrorKind.JAVA) && (round == ERROR_ROUND),
                    jwk.warn(round));

        if (mwk.warn(round))
            messager.printMessage(Diagnostic.Kind.WARNING, "round " + round);

        if ((ek == ErrorKind.MESSAGER) && (round == ERROR_ROUND))
            messager.printMessage(Diagnostic.Kind.ERROR, "round " + round);

        return true;
    }

    void generate(String name, boolean error, boolean warn) {
        try {
            JavaFileObject fo = filer.createSourceFile(name);
            Writer out = fo.openWriter();
            try {
                out.write("class " + name + " {\n"
                        + (warn ? "    void m() throws Exception { try (AutoCloseable ac = null) { } }" : "")
                        + (error ? "   ERROR\n" : "")
                        + "}\n");
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
