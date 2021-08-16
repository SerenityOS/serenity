/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4981566 5028634 5094412 6304984 7025786 7025789 8001112 8028545 8000961 8030610 8028546 8188870 8173382 8173382 8193290 8205619 8028563 8245147 8245586 8257453
 * @summary Check interpretation of -target and -source options
 * @modules java.compiler
 *          jdk.compiler
 * @run main Versions
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/*
 * If not explicitly specified the latest source and latest target
 * values are the defaults. If explicitly specified, the target value
 * has to be greater than or equal to the source value.
 */
public class Versions {

    protected JavaCompiler javacompiler;
    protected int failedCases;

    public Versions() throws IOException {
        javacompiler = ToolProvider.getSystemJavaCompiler();
        genSourceFiles();
        failedCases = 0;
    }

    public static void main(String... args) throws IOException {
        Versions versions = new Versions();
        versions.run();
    }

    public static final Set<String> RETIRED_SOURCES =
        Set.of("1.2", "1.3", "1.4", "1.5", "1.6");

    public static final Set<String> VALID_SOURCES =
        Set.of("1.7", "1.8", "1.9", "1.10", "11", "12", "13", "14",
               "15", "16", "17", "18");

    public static final String LATEST_MAJOR_VERSION = "62.0";

    static enum SourceTarget {
        SEVEN(true,   "51.0",  "7", Versions::checksrc7),
        EIGHT(true,   "52.0",  "8", Versions::checksrc8),
        NINE(true,    "53.0",  "9", Versions::checksrc9),
        TEN(true,     "54.0", "10", Versions::checksrc10),
        ELEVEN(false, "55.0", "11", Versions::checksrc11),
        TWELVE(false, "56.0", "12", Versions::checksrc12),
        THIRTEEN(false, "57.0", "13", Versions::checksrc13),
        FOURTEEN(false, "58.0", "14", Versions::checksrc14),
        FIFTEEN(false,  "59.0", "15", Versions::checksrc15),
        SIXTEEN(false,  "60.0", "16", Versions::checksrc16),
        SEVENTEEN(false, "61.0", "17", Versions::checksrc17),
        EIGHTEEN(false,  "62.0", "18", Versions::checksrc18);

        private final boolean dotOne;
        private final String classFileVer;
        private final String target;
        private final BiConsumer<Versions, List<String>> checker;

        private SourceTarget(boolean dotOne, String classFileVer, String target,
                             BiConsumer<Versions, List<String>> checker) {
            this.dotOne = dotOne;
            this.classFileVer = classFileVer;
            this.target = target;
            this.checker = checker;
        }

        public void checksrc(Versions version, List<String> args) {
            checker.accept(version, args);
        }

        public boolean dotOne() {
            return dotOne;
        }

        public String classFileVer() {
            return classFileVer;
        }

        public String target() {
            return target;
        }
    }

    void run() {
        String TC = "";
        System.out.println("Version.java: Starting");

        check(LATEST_MAJOR_VERSION);
        for (String source : VALID_SOURCES) {
            check(LATEST_MAJOR_VERSION, List.of("-source " + source));
        }

        // Verify that a -source value less than a -target value is
        // accepted and that the resulting class files are dependent
        // on the target setting alone.
        SourceTarget[] sourceTargets = SourceTarget.values();
        for (int i = 0; i < sourceTargets.length; i++) {
            SourceTarget st = sourceTargets[i];
            String classFileVer = st.classFileVer();
            String target = st.target();
            boolean dotOne = st.dotOne();
            check_source_target(dotOne, List.of(classFileVer, target, target));
            for (int j = i - 1; j >= 0; j--) {
                String source = sourceTargets[j].target();
                check_source_target(dotOne, List.of(classFileVer, source, target));
            }
        }

        // Verify acceptance of different combinations of -source N,
        // -target M; N <= M
        for (int i = 0; i < sourceTargets.length; i++) {
            SourceTarget st = sourceTargets[i];

            st.checksrc(this, List.of("-source " + st.target()));
            st.checksrc(this, List.of("-source " + st.target(), "-target " + st.target()));

            if (st.dotOne()) {
                st.checksrc(this, List.of("-source 1." + st.target()));
                st.checksrc(this, List.of("-source 1." + st.target(), "-target 1." + st.target()));
            }

            if (i == sourceTargets.length - 1) {
                // Can use -target without -source setting only for
                // most recent target since the most recent source is
                // the default.
                st.checksrc(this, List.of("-target " + st.target()));

                if (!st.classFileVer().equals(LATEST_MAJOR_VERSION)) {
                    throw new RuntimeException(st +
                                               "does not have class file version" +
                                               LATEST_MAJOR_VERSION);
                }
            }
        }

        // Verify that -source N -target (N-1) is rejected
        for (int i = 1 /* Skip zeroth value */; i < sourceTargets.length; i++) {
            fail(List.of("-source " + sourceTargets[i].target(),
                 "-target " + sourceTargets[i-1].target(),
                         "Base.java"));
        }

        // Previously supported source/target values
        for (String source  : RETIRED_SOURCES) {
            fail(List.of("-source " + source, "-target " + source, "Base.java"));
        }

        if (failedCases > 0) {
            System.err.println("failedCases = " + String.valueOf(failedCases));
            throw new Error("Test failed");
        }

    }

    protected void printargs(String fname, List<String> args) {
        System.out.printf("test: %s", fname);
        for (String onearg : args) {
            System.out.printf(" %s", onearg);
        }
        System.out.printf("\n", fname);
    }

    protected void check_source_target(boolean dotOne, List<String> args) {
        printargs("check_source_target", args);
        check_target(dotOne, List.of(args.get(0), args.get(1), args.get(2)));
        if (dotOne) {
            check_target(dotOne, List.of(args.get(0), "1." + args.get(1), args.get(2)));
        }
    }

    protected void check_target(boolean dotOne, List<String> args) {
        check(args.get(0), List.of("-source " + args.get(1), "-target " + args.get(2)));
        if (dotOne) {
            check(args.get(0), List.of("-source " + args.get(1), "-target 1." + args.get(2)));
        }
    }

    protected void check(String major) {
        check(major, List.of());
    }

    protected void check(String major, List<String> args) {
        printargs("check", args);
        List<String> jcargs = new ArrayList<>();
        jcargs.add("-Xlint:-options");

        // add in args conforming to List requrements of JavaCompiler
        for (String onearg : args) {
            String[] fields = onearg.split(" ");
            for (String onefield : fields) {
                jcargs.add(onefield);
            }
        }

        boolean creturn = compile("Base.java", jcargs);
        if (!creturn) {
            // compilation errors note and return.. assume no class file
            System.err.println("check: Compilation Failed");
            System.err.println("\t classVersion:\t" + major);
            System.err.println("\t arguments:\t" + jcargs);
            failedCases++;

        } else if (!checkClassFileVersion("Base.class", major)) {
            failedCases++;
        }
    }

    protected void checksrc7(List<String> args) {
        printargs("checksrc7", args);
        expectedPass(args, List.of("New7.java"));
        expectedFail(args, List.of("New8.java"));
    }

    protected void checksrc8(List<String> args) {
        printargs("checksrc8", args);
        expectedPass(args, List.of("New7.java", "New8.java"));
        expectedFail(args, List.of("New10.java"));
    }

    protected void checksrc9(List<String> args) {
        printargs("checksrc9", args);
        expectedPass(args, List.of("New7.java", "New8.java"));
        expectedFail(args, List.of("New10.java"));
    }

    protected void checksrc10(List<String> args) {
        printargs("checksrc10", args);
        expectedPass(args, List.of("New7.java", "New8.java", "New10.java"));
        expectedFail(args, List.of("New11.java"));
    }

    protected void checksrc11(List<String> args) {
        printargs("checksrc11", args);
        expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java"));
        expectedFail(args, List.of("New14.java"));
    }

    protected void checksrc12(List<String> args) {
        printargs("checksrc12", args);
        expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java"));
        expectedFail(args, List.of("New14.java"));
    }

    protected void checksrc13(List<String> args) {
        printargs("checksrc13", args);
        expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java"));
        expectedFail(args, List.of("New14.java"));
    }

    protected void checksrc14(List<String> args) {
        printargs("checksrc14", args);
        expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java",
                                   "New14.java"));
        expectedFail(args, List.of("New15.java"));
    }

   protected void checksrc15(List<String> args) {
       printargs("checksrc15", args);
       expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java",
                                  "New14.java", "New15.java"));
        expectedFail(args, List.of("New16.java"));
    }

   protected void checksrc16(List<String> args) {
       printargs("checksrc16", args);
       expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java",
                                  "New14.java", "New15.java", "New16.java"));
        expectedFail(args, List.of("New17.java"));
    }

   protected void checksrc17(List<String> args) {
       printargs("checksrc17", args);
       expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java",
                                  "New14.java", "New15.java", "New16.java", "New17.java"));
       // Add expectedFail after new language features added in a later release.
    }

   protected void checksrc18(List<String> args) {
       printargs("checksrc18", args);
       expectedPass(args, List.of("New7.java", "New8.java", "New10.java", "New11.java",
                                  "New14.java", "New15.java", "New16.java", "New17.java"));
       // Add expectedFail after new language features added in a later release.
    }

    protected void expected(List<String> args, List<String> fileNames,
                            Consumer<List<String>> passOrFail) {
        ArrayList<String> fullArguments = new ArrayList<>(args);
        // Issue compile with each filename in turn.
        for(String fileName : fileNames) {
            fullArguments.add(fileName);
            passOrFail.accept(fullArguments);
            fullArguments.remove(fullArguments.size() - 1);
        }
    }

    protected void expectedPass(List<String> args, List<String> fileNames) {
        expected(args, fileNames, this::pass);
    }

    protected void expectedFail(List<String> args, List<String> fileNames) {
        expected(args, fileNames, this::fail);
    }

    protected void pass(List<String> args) {
        printargs("pass", args);

        List<String> jcargs = new ArrayList<>();
        jcargs.add("-Xlint:-options");

        // add in args conforming to List requrements of JavaCompiler
        for (String onearg : args) {
            String[] fields = onearg.split(" ");
            for (String onefield : fields) {
                jcargs.add(onefield);
            }
        }

        // empty list is error
        if (jcargs.isEmpty()) {
            System.err.println("error: test error in pass() - No arguments");
            System.err.println("\t arguments:\t" + jcargs);
            failedCases++;
            return;
        }

        // the last argument is the filename *.java
        String filename = jcargs.get(jcargs.size() - 1);
        jcargs.remove(jcargs.size() - 1);

        boolean creturn = compile(filename, jcargs);
        // expect a compilation failure, failure if otherwise
        if (!creturn) {
            System.err.println("pass: Compilation erroneously failed");
            System.err.println("\t arguments:\t" + jcargs);
            System.err.println("\t file     :\t" + filename);
            failedCases++;

        }

    }

    protected void fail(List<String> args) {
        printargs("fail", args);

        List<String> jcargs = new ArrayList<>();
        jcargs.add("-Xlint:-options");

        // add in args conforming to List requrements of JavaCompiler
        for (String onearg : args) {
            String[] fields = onearg.split(" ");
            for (String onefield : fields) {
                jcargs.add(onefield);
            }
        }

        // empty list is error
        if (jcargs.isEmpty()) {
            System.err.println("error: test error in fail()- No arguments");
            System.err.println("\t arguments:\t" + jcargs);
            failedCases++;
            return;
        }

        // the last argument is the filename *.java
        String filename = jcargs.get(jcargs.size() - 1);
        jcargs.remove(jcargs.size() - 1);

        boolean creturn = compile(filename, jcargs);
        // expect a compilation failure, failure if otherwise
        if (creturn) {
            System.err.println("fail: Compilation erroneously succeeded");
            System.err.println("\t arguments:\t" + jcargs);
            System.err.println("\t file     :\t" + filename);
            failedCases++;
        }
    }

    protected boolean compile(String sourceFile, List<String> options) {
        JavaCompiler.CompilationTask jctask;
        try (StandardJavaFileManager fm = javacompiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(sourceFile);

            jctask = javacompiler.getTask(
                null,    // Writer
                fm,      // JavaFileManager
                null,    // DiagnosticListener
                options, // Iterable<String>
                null,    // Iterable<String> classes
                files);  // Iterable<? extends JavaFileObject>

            try {
                return jctask.call();
            } catch (IllegalStateException e) {
                System.err.println(e);
                return false;
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    protected void writeSourceFile(String fileName, String body) throws IOException{
        try (Writer fw = new FileWriter(fileName)) {
            fw.write(body);
        }
    }

    protected void genSourceFiles() throws IOException{
        /* Create a file that executes with all supported versions. */
        writeSourceFile("Base.java","public class Base { }\n");

        /*
         * Create a file with a new feature in 7, not in 6 : "<>"
         */
        writeSourceFile("New7.java",
            """
            import java.util.List;
            import java.util.ArrayList;
            class New7 { List<String> s = new ArrayList<>(); }
            """
        );

        /*
         * Create a file with a new feature in 8, not in 7 : lambda
         */
        writeSourceFile("New8.java",
            """
            public class New8 {
                void m() {
                new Thread(() -> { });
                }
            }
             """
        );

        /*
         * Create a file with a new feature in 10, not in 9 : var
         */
        writeSourceFile("New10.java",
            """
            public class New10 {
                void m() {
                var tmp = new Thread(() -> { });
                }
            }
            """
        );

        /*
         * Create a file with a new feature in 11, not in 10 : var for lambda parameters
         */
        writeSourceFile("New11.java",
            """
            public class New11 {
                static java.util.function.Function<String,String> f = (var x) -> x.substring(0);
                void m(String name) {
                var tmp = new Thread(() -> { }, f.apply(name));
                }
            }
            """
        );

        /*
         * Create a file with a new feature in 14, not in 13 : switch expressions
         */
        writeSourceFile("New14.java",
            """
            public class New14 {
                static {
                    int i = 5;
                    System.out.println(
                        switch(i) {
                            case 0 -> false;
                            default -> true;
                        }
                    );
                }
            }
            """
        );

        /*
         * Create a file with a new feature in 15, not in 14 : text blocks
         */
        writeSourceFile("New15.java",
            """
            public class New15 {
                public static final String s =
                \"\"\"
                Hello, World.
                \"\"\"
                ;
            }
            """
        );

        /*
         * Create a file with a new feature in 16, not in 15 : records
         */
        writeSourceFile("New16.java",
            """
            public class New16 {
                public record Record(double rpm) {
                    public static final Record LONG_PLAY = new Record(100.0/3.0);
                }
            }
            """
        );

        /*
         * Create a file with a new feature in 17, not in 16 : sealed classes
         */
        writeSourceFile("New17.java",
            """
            public class New17 {
                public static sealed class Seal {}

                public static final class Pinniped extends Seal {}
                public static final class TaperedThread extends Seal {}
                public static final class Wax extends Seal {}
            }
            """
        );
    }

    protected boolean checkClassFileVersion
        (String filename,String classVersionNumber) {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        try (FileChannel fc = new FileInputStream(filename).getChannel()) {
            bb.clear();
            if (fc.read(bb) < 0)
                throw new IOException("Could not read from file : " + filename);
            bb.flip();
            int minor = bb.getShort(4);
            int major = bb.getShort(6);
            String fileVersion = major + "." + minor;
            if (fileVersion.equals(classVersionNumber)) {
                return true;
            } else {
                System.err.println("checkClassFileVersion : Failed");
                System.err.println("\tclassfile version mismatch");
                System.err.println("\texpected : " + classVersionNumber);
                System.err.println("\tfound    : " + fileVersion);
                return false;
            }
        }
        catch (IOException e) {
            System.err.println("checkClassFileVersion : Failed");
            System.err.println("\terror :\t" + e.getMessage());
            System.err.println("\tfile:\tfilename");
        }
        return false;
    }
}

