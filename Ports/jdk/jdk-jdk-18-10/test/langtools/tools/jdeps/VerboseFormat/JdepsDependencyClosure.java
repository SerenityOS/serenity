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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8080608
 * @summary Test that jdeps verbose output has a summary line when dependencies
 *          are found within the same archive. For each testcase, compare the
 *          result obtained from jdeps with the expected result.
 * @modules jdk.jdeps/com.sun.tools.jdeps
 *          java.base/sun.security.x509
 * @build use.indirect.DontUseJdkInternal2
 * @build use.indirect.UseJdkInternalIndirectly
 * @build use.indirect2.DontUseJdkInternal3
 * @build use.indirect2.UseJdkInternalIndirectly2
 * @build use.internal.DontUseJdkInternal
 * @build use.internal.UseClassWithJdkInternal
 * @build use.internal.UseJdkInternalClass
 * @build use.internal.UseJdkInternalClass2
 * @run main JdepsDependencyClosure --test:0
 * @run main JdepsDependencyClosure --test:1
 * @run main JdepsDependencyClosure --test:2
 * @run main JdepsDependencyClosure --test:3
 */
public class JdepsDependencyClosure {

    static boolean VERBOSE = false;
    static boolean COMPARE_TEXT = true;

    static final String JDEPS_SUMMARY_TEXT_FORMAT = "%s -> %s%n";
    static final String JDEPS_VERBOSE_TEXT_FORMAT = "   %-50s -> %-50s %s%n";

    /**
     * Helper class used to store arguments to pass to
     * {@code JdepsDependencyClosure.test} as well as expected
     * results.
     */
    static class TestCaseData {
        final Map<String, Set<String>> expectedDependencies;
        final String expectedText;
        final String[] args;
        final boolean closure;

        TestCaseData(Map<String, Set<String>> expectedDependencies,
                        String expectedText,
                        boolean closure,
                        String[] args) {
            this.expectedDependencies = expectedDependencies;
            this.expectedText = expectedText;
            this.closure = closure;
            this.args = args;
        }

        public void test() {
            if (expectedDependencies != null) {
                String format = closure
                        ? "Running (closure): jdeps %s %s %s %s"
                        : "Running: jdeps %s %s %s %s";
                System.out.println(String.format(format, (Object[])args));
            }
            JdepsDependencyClosure.test(args, expectedDependencies, expectedText, closure);
        }

        /**
         * Make a new test case data to invoke jdeps and test its output.
         * @param pattern The pattern that will passed through to jdeps -e
         *                This is expected to match only one class.
         * @param arcPath The archive to analyze. A jar or a class directory.
         * @param classes For each reported archive dependency couple, the
         *                expected list of classes in the source that will
         *                be reported as having a dependency on the class
         *                in the target that matches the given pattern.
         * @param dependencies For each archive dependency couple, a singleton list
         *                containing the name of the class in the target that
         *                matches the pattern. It is expected that the pattern
         *                will match only one class in the target.
         *                If the pattern matches several classes the
         *                expected text may no longer match the jdeps output.
         * @param archives A list of archive dependency couple in the form
         *               {{sourceName1, sourcePath1, targetDescription1, targetPath1}
         *                {sourceName2, sourcePath2, targetDescription2, targetPath2}
         *                ... }
         *               For a JDK module - e.g. java.base, the targetDescription
         *               is usually something like "JDK internal API (java.base)"
         *               and the targetPath is usually the module name "java.base".
         * @param closure Whether jdeps should be recursively invoked to build
         *                the closure.
         * @return An instance of TestCaseData containing all the information
         *         needed to perform the jdeps invokation and test its output.
         */
        public static TestCaseData make(String pattern, String arcPath, String[][] classes,
                String[][] dependencies, String[][] archives, boolean closure) {
            final String[] args = new String[] {
                "-e", pattern, "-v", arcPath
            };
            Map<String, Set<String>> expected = new HashMap<>();
            String expectedText = "";
            for (int i=0; i<classes.length; i++) {
                final int index = i;
                expectedText += Stream.of(classes[i])
                    .map((cn) -> String.format(JDEPS_VERBOSE_TEXT_FORMAT, cn,
                            dependencies[index][0], archives[index][2]))
                    .reduce(String.format(JDEPS_SUMMARY_TEXT_FORMAT, archives[i][0],
                            archives[index][3]), (s1,s2) -> s1.concat(s2));
                for (String cn : classes[index]) {
                    expected.putIfAbsent(cn, new HashSet<>());
                    expected.get(cn).add(dependencies[index][0]);
                }
            }
            return new TestCaseData(expected, expectedText, closure, args);
        }

        public static TestCaseData valueOf(String[] args) {
            if (args.length == 1 && args[0].startsWith("--test:")) {
                // invoked from jtreg. build test case data for selected test.
                int index = Integer.parseInt(args[0].substring("--test:".length()));
                if (index >= dataSuppliers.size()) {
                    throw new RuntimeException("No such test case: " + index
                            + " - available testcases are [0.."
                            + (dataSuppliers.size()-1) + "]");
                }
                return dataSuppliers.get(index).get();
            } else {
                // invoked in standalone. just take the given argument
                // and perform no validation on the output (except that it
                // must start with a summary line)
                return new TestCaseData(null, null, true, args);
            }
        }

    }

    static TestCaseData makeTestCaseOne() {
        final String arcPath = System.getProperty("test.classes", "build/classes");
        final String arcName = Paths.get(arcPath).getFileName().toString();
        final String[][] classes = new String[][] {
            {"use.indirect2.UseJdkInternalIndirectly2", "use.internal.UseClassWithJdkInternal"},
        };
        final String[][] dependencies = new String[][] {
            {"use.internal.UseJdkInternalClass"},
        };
        final String[][] archives = new String[][] {
            {arcName, arcPath, arcName, arcPath},
        };
        return TestCaseData.make("use.internal.UseJdkInternalClass", arcPath, classes,
                dependencies, archives, false);
    }

    static TestCaseData makeTestCaseTwo() {
        String arcPath = System.getProperty("test.classes", "build/classes");
        String arcName = Paths.get(arcPath).getFileName().toString();
        String[][] classes = new String[][] {
            {"use.internal.UseJdkInternalClass", "use.internal.UseJdkInternalClass2"}
        };
        String[][] dependencies = new String[][] {
            {"sun.security.x509.X509CertInfo"}
        };
        String[][] archive = new String[][] {
            {arcName, arcPath, "JDK internal API (java.base)", "java.base"},
        };
        return TestCaseData.make("sun.security.x509.X509CertInfo", arcPath, classes,
                dependencies, archive, false);
    }

    static TestCaseData makeTestCaseThree() {
        final String arcPath = System.getProperty("test.classes", "build/classes");
        final String arcName = Paths.get(arcPath).getFileName().toString();
        final String[][] classes = new String[][] {
            {"use.indirect2.UseJdkInternalIndirectly2", "use.internal.UseClassWithJdkInternal"},
            {"use.indirect.UseJdkInternalIndirectly"}
        };
        final String[][] dependencies = new String[][] {
            {"use.internal.UseJdkInternalClass"},
            {"use.internal.UseClassWithJdkInternal"}
        };
        final String[][] archives = new String[][] {
            {arcName, arcPath, arcName, arcPath},
            {arcName, arcPath, arcName, arcPath}
        };
        return TestCaseData.make("use.internal.UseJdkInternalClass", arcPath, classes,
                dependencies, archives, true);
    }


    static TestCaseData makeTestCaseFour() {
        final String arcPath = System.getProperty("test.classes", "build/classes");
        final String arcName = Paths.get(arcPath).getFileName().toString();
        final String[][] classes = new String[][] {
            {"use.internal.UseJdkInternalClass", "use.internal.UseJdkInternalClass2"},
            {"use.indirect2.UseJdkInternalIndirectly2", "use.internal.UseClassWithJdkInternal"},
            {"use.indirect.UseJdkInternalIndirectly"}
        };
        final String[][] dependencies = new String[][] {
            {"sun.security.x509.X509CertInfo"},
            {"use.internal.UseJdkInternalClass"},
            {"use.internal.UseClassWithJdkInternal"}
        };
        final String[][] archives = new String[][] {
            {arcName, arcPath, "JDK internal API (java.base)", "java.base"},
            {arcName, arcPath, arcName, arcPath},
            {arcName, arcPath, arcName, arcPath}
        };
        return TestCaseData.make("sun.security.x509.X509CertInfo", arcPath, classes, dependencies,
                archives, true);
    }

    static final List<Supplier<TestCaseData>> dataSuppliers = Arrays.asList(
        JdepsDependencyClosure::makeTestCaseOne,
        JdepsDependencyClosure::makeTestCaseTwo,
        JdepsDependencyClosure::makeTestCaseThree,
        JdepsDependencyClosure::makeTestCaseFour
    );



    /**
     * The OutputStreamParser is used to parse the format of jdeps.
     * It is thus dependent on that format.
     */
    static class OutputStreamParser extends OutputStream {
        // OutputStreamParser will populate this map:
        //
        // For each archive, a list of class in where dependencies where
        //     found...
        final Map<String, Set<String>> deps;
        final StringBuilder text = new StringBuilder();

        StringBuilder[] lines = { new StringBuilder(), new StringBuilder() };
        int line = 0;
        int sepi = 0;
        char[] sep;

        public OutputStreamParser(Map<String, Set<String>> deps) {
            this.deps = deps;
            this.sep = System.getProperty("line.separator").toCharArray();
        }

        @Override
        public void write(int b) throws IOException {
            lines[line].append((char)b);
            if (b == sep[sepi]) {
                if (++sepi == sep.length) {
                    text.append(lines[line]);
                    if (lines[0].toString().startsWith("  ")) {
                        throw new RuntimeException("Bad formatting: "
                                + "summary line missing for\n"+lines[0]);
                    }
                    // Usually the output looks like that:
                    // <archive-1> -> java.base
                    //   <class-1>      -> <dependency> <dependency description>
                    //   <class-2>      -> <dependency> <dependency description>
                    //   ...
                    // <archive-2> -> java.base
                    //   <class-3>      -> <dependency> <dependency description>
                    //   <class-4>      -> <dependency> <dependency description>
                    //   ...
                    //
                    // We want to keep the <archive> line in lines[0]
                    // and have the ith <class-i> line in lines[1]
                    if (line == 1) {
                        // we have either a <class> line or an <archive> line.
                        String line1 = lines[0].toString();
                        String line2 = lines[1].toString();
                        if (line2.startsWith("  ")) {
                            // we have a class line, record it.
                            parse(line1, line2);
                            // prepare for next <class> line.
                            lines[1] = new StringBuilder();
                        } else {
                            // We have an archive line: We are switching to the next archive.
                            // put the new <archive> line in lines[0], and prepare
                            // for reading the next <class> line
                            lines[0] = lines[1];
                            lines[1] = new StringBuilder();
                         }
                    } else {
                        // we just read the first <archive> line.
                        // prepare to read <class> lines.
                        line = 1;
                    }
                    sepi = 0;
                }
            } else {
                sepi = 0;
            }
        }

        // Takes a couple of lines, where line1 is an <archive> line and
        // line 2 is a <class> line. Parses the line to extract the archive
        // name and dependent class name, and record them in the map...
        void parse(String line1, String line2) {
            String archive = line1.substring(0, line1.indexOf(" -> "));
            int l2ArrowIndex = line2.indexOf(" -> ");
            String className = line2.substring(2, l2ArrowIndex).replace(" ", "");
            String depdescr = line2.substring(l2ArrowIndex + 4);
            String depclass = depdescr.substring(0, depdescr.indexOf(" "));
            deps.computeIfAbsent(archive, (k) -> new HashSet<>());
            deps.get(archive).add(className);
            if (VERBOSE) {
                System.out.println(archive+": "+className+" depends on "+depclass);
            }
        }

    }

    /**
     * The main method.
     *
     * Can be run in two modes:
     * <ul>
     * <li>From jtreg: expects 1 argument in the form {@code --test:<test-nb>}</li>
     * <li>From command line: expected syntax is {@code -e <pattern> -v jar [jars..]}</li>
     * </ul>
     * <p>When called from the command line this method will call jdeps recursively
     * to build a closure of the dependencies on {@code <pattern>} and print a summary.
     * <p>When called from jtreg - it will call jdeps either once only or
     * recursively depending on the pattern.
     * @param args either {@code --test:<test-nb>} or {@code -e <pattern> -v jar [jars..]}.
     */
    public static void main(String[] args) {
        runWithLocale(Locale.ENGLISH, TestCaseData.valueOf(args)::test);
    }

    private static void runWithLocale(Locale loc, Runnable run) {
        final Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(loc);
        try {
            run.run();
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }


    public static void test(String[] args, Map<String, Set<String>> expected,
            String expectedText, boolean closure) {
        try {
            doTest(args, expected, expectedText, closure);
        } catch (Throwable t) {
            try {
                printDiagnostic(args, expectedText, t, closure);
            } catch(Throwable tt) {
                throw t;
            }
            throw t;
        }
    }

    static class TextFormatException extends RuntimeException {
        final String expected;
        final String actual;
        TextFormatException(String message, String expected, String actual) {
            super(message);
            this.expected = expected;
            this.actual = actual;
        }
    }

    public static void printDiagnostic(String[] args, String expectedText,
            Throwable t, boolean closure) {
        if (expectedText != null || t instanceof TextFormatException) {
            System.err.println("=====   TEST FAILED   =======");
            System.err.println("command: " + Stream.of(args)
                    .reduce("jdeps", (s1,s2) -> s1.concat(" ").concat(s2)));
            System.err.println("===== Expected Output =======");
            System.err.append(expectedText);
            System.err.println("===== Command  Output =======");
            if (t instanceof TextFormatException) {
                System.err.print(((TextFormatException)t).actual);
            } else {
                com.sun.tools.jdeps.Main.run(args, new PrintWriter(System.err));
                if (closure) System.err.println("... (closure not available) ...");
            }
            System.err.println("=============================");
        }
    }

    public static void doTest(String[] args, Map<String, Set<String>> expected,
            String expectedText, boolean closure) {
        if (args.length < 3 || !"-e".equals(args[0]) || !"-v".equals(args[2])) {
            System.err.println("Syntax: -e <classname> -v [list of jars or directories]");
            return;
        }
        Map<String, Map<String, Set<String>>> alldeps = new HashMap<>();
        String depName = args[1];
        List<String> search = new ArrayList<>();
        search.add(depName);
        Set<String> searched = new LinkedHashSet<>();
        StringBuilder text = new StringBuilder();
        while(!search.isEmpty()) {
            args[1] = search.remove(0);
            if (VERBOSE) {
                System.out.println("Looking for " + args[1]);
            }
            searched.add(args[1]);
            Map<String, Set<String>> deps =
                    alldeps.computeIfAbsent(args[1], (k) -> new HashMap<>());
            OutputStreamParser parser = new OutputStreamParser(deps);
            PrintWriter writer = new PrintWriter(parser);
            com.sun.tools.jdeps.Main.run(args, writer);
            if (VERBOSE) {
                System.out.println("Found: " + deps.values().stream()
                        .flatMap(s -> s.stream()).collect(Collectors.toSet()));
            }
            if (expectedText != null) {
                text.append(parser.text.toString());
            }
            search.addAll(deps.values().stream()
                    .flatMap(s -> s.stream())
                    .filter(k -> !searched.contains(k))
                    .collect(Collectors.toSet()));
            if (!closure) break;
        }

        // Print summary...
        final Set<String> classes = alldeps.values().stream()
                .flatMap((m) -> m.values().stream())
                .flatMap(s -> s.stream()).collect(Collectors.toSet());
        Map<String, Set<String>> result = new HashMap<>();
        for (String c : classes) {
            Set<String> archives = new HashSet<>();
            Set<String> dependencies = new HashSet<>();
            for (String d : alldeps.keySet()) {
                Map<String, Set<String>> m = alldeps.get(d);
                for (String a : m.keySet()) {
                    Set<String> s = m.get(a);
                    if (s.contains(c)) {
                        archives.add(a);
                        dependencies.add(d);
                    }
                }
            }
            result.put(c, dependencies);
            System.out.println(c + " " + archives + " depends on " + dependencies);
        }

        // If we're in jtreg, then check result (expectedText != null)
        if (expectedText != null && COMPARE_TEXT) {
            //text.append(String.format("%n"));
            if (text.toString().equals(expectedText)) {
                System.out.println("SUCCESS - got expected text");
            } else {
                throw new TextFormatException("jdeps output is not as expected",
                                expectedText, text.toString());
            }
        }
        if (expected != null) {
            if (expected.equals(result)) {
                System.out.println("SUCCESS - found expected dependencies");
            } else if (expectedText == null) {
                throw new RuntimeException("Bad dependencies: Expected " + expected
                        + " but found " + result);
            } else {
                throw new TextFormatException("Bad dependencies: Expected "
                        + expected
                        + " but found " + result,
                        expectedText, text.toString());
            }
        }
    }
}
