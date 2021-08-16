/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8027634 8210810 8240629
 * @summary Verify syntax of argument file
 * @build TestHelper
 * @run main ArgFileSyntax
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ArgFileSyntax extends TestHelper {
    // Buffer size in args.c readArgFile() method
    private static final int ARG_FILE_PARSER_BUF_SIZE = 4096;

    private File createArgFile(List<String> lines) throws IOException {
        File argFile = new File("argfile");
        argFile.delete();
        createAFile(argFile, lines);
        return argFile;
    }

    private void verifyOutput(List<String> args, TestResult tr) {
        if (args.isEmpty()) {
            return;
        }

        int i = 1;
        for (String x : args) {
            tr.matches(".*argv\\[" + i + "\\] = " + Pattern.quote(x) + ".*");
            i++;
        }
        if (! tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }
    }

    // arg file content,  expected options
    static String[] testCases[][] = {
        { // empty file
            {}, {}
        },
        { // comments and # inside quote
            { "# a couple of -X flags",
              "-Xmx32m",
              "-XshowSettings #inline comment",
              "-Dpound.in.quote=\"This property contains #.\"",
              "# add -version",
              "-version",
              "# trail comment"
            },
            { "-Xmx32m",
              "-XshowSettings",
              "-Dpound.in.quote=This property contains #.",
              "-version"
            }
        },
        { // open quote with continuation directive
          // multiple options in a line
            { "-cp \"c:\\\\java lib\\\\all;\\",
              "     c:\\\\lib\"",
              "-Xmx32m -XshowSettings",
              "-version"
            },
            { "-cp",
              "c:\\java lib\\all;c:\\lib",
              "-Xmx32m",
              "-XshowSettings",
              "-version"
            }
        },
        { // no continuation on open quote
          // multiple lines in a property
            { "-cp \"c:\\\\open quote\\\\all;",
              "     # c:\\\\lib\"",
              "-Dmultiple.lines=\"line 1\\nline 2\\n\\rline 3\"",
              "-Dopen.quote=\"Open quote to EOL",
              "-Dcontinue.with.leadingWS=\"Continue with\\",
              "  \\ leading WS.",
              "-Dcontinue.without.leadingWS=\"Continue without \\",
              "   leading WS.",
              "-Descape.seq=\"escaped chars: \\\"\\a\\b\\c\\f\\t\\v\\9\\6\\23\\82\\28\\377\\477\\278\\287\\n\"",
              "-version"
            },
            { "-cp",
              "c:\\open quote\\all;",
              "-Dmultiple.lines=line 1",
              // line 2 and line 3 shoule be in output, but not as arg[x]=
              "-Dopen.quote=Open quote to EOL",
              "-Dcontinue.with.leadingWS=Continue with leading WS.",
              "-Dcontinue.without.leadingWS=Continue without leading WS.",
              // cannot verify \n and \r as that break output lines
              "-Descape.seq=escaped chars: \"abc\f\tv96238228377477278287",
              "-version"
            }
        },
        { // No need to escape if not in quote
          // also quote part of a token
            { "-cp c:\\\"partial quote\"\\all",
              "-Xmx32m -XshowSettings",
              "-version"
            },
            { "-cp",
              "c:\\partial quote\\all",
              "-Xmx32m",
              "-XshowSettings",
              "-version"
            }
        },
        { // No recursive expansion
            { "-Xmx32m",
              "-cp",
              " # @cpfile should remains @cpfile",
              "@cpfile",
              "-version"
            },
            { "-Xmx32m",
              "-cp",
              "@cpfile",
              "-version"
            }
        },
        { // Mix quotation
            { "-Dsingle.in.double=\"Mix 'single' in double\"",
              "-Ddouble.in.single='Mix \"double\" in single'",
              "-Dsingle.in.single='Escape \\\'single\\\' in single'",
              "-Ddouble.in.double=\"Escape \\\"double\\\" in double\""
            },
            { "-Dsingle.in.double=Mix 'single' in double",
              "-Ddouble.in.single=Mix \"double\" in single",
              "-Dsingle.in.single=Escape 'single' in single",
              "-Ddouble.in.double=Escape \"double\" in double"
            },
        },
        { // \t\f as whitespace and in escape
            { "-Xmx32m\t-Xint\f-version",
              "-Dcontinue.with.leadingws=\"Line1\\",
              " \t\fcontinue with \\f<ff> and \\t<tab>"
            },
            { "-Xmx32m",
              "-Xint",
              "-version",
              "-Dcontinue.with.leadingws=Line1continue with \f<ff> and \t<tab>"
            }
        }
    };

    public List<List<List<String>>> loadCases() {
        List<List<List<String>>> rv = new ArrayList<>();
        for (String[][] testCaseArray: testCases) {
            List<List<String>> testCase = new ArrayList<>(2);
            testCase.add(Arrays.asList(testCaseArray[0]));
            testCase.add(Arrays.asList(testCaseArray[1]));
            rv.add(testCase);
        }

        // long lines
        String bag = "-Dgarbage=";
        String ver = "-version";
        // a token 8192 long
        char[] data = new char[2*ARG_FILE_PARSER_BUF_SIZE - bag.length()];
        Arrays.fill(data, 'O');
        List<String> scratch = new ArrayList<>();
        scratch.add("-Xmx32m");
        scratch.add(bag + String.valueOf(data));
        scratch.add(ver);
        rv.add(Collections.nCopies(2, scratch));

        data = new char[2*ARG_FILE_PARSER_BUF_SIZE + 1024];
        Arrays.fill(data, 'O');
        scratch = new ArrayList<>();
        scratch.add(bag + String.valueOf(data));
        scratch.add(ver);
        rv.add(Collections.nCopies(2, scratch));

        // 8210810: position escaping character at boundary
        // reserve space for quote and backslash
        data = new char[ARG_FILE_PARSER_BUF_SIZE - bag.length() - 2];
        Arrays.fill(data, 'O');
        scratch = new ArrayList<>();
        String filling = String.valueOf(data);
        scratch.add(bag + "'" + filling + "\\\\aaa\\\\'");
        scratch.add(ver);
        rv.add(List.of(scratch, List.of(bag + filling + "\\aaa\\", ver)));
        return rv;
    }

    // 8240629: end or start comment at boundary
    @Test
    public void test8240629() throws IOException {
        char[] data = new char[ARG_FILE_PARSER_BUF_SIZE];
        data[0] = '#';
        Arrays.fill(data, 1, data.length, '0');

        int need = ARG_FILE_PARSER_BUF_SIZE - System.lineSeparator().length();
        // Comment end before, at, after boundary
        for (int count = need - 1; count <= need + 1 ; count++) {
            String commentAtBoundary = String.valueOf(data, 0, count);
            List<String> content = new ArrayList<>();
            content.add(commentAtBoundary);
            content.add("# start a new comment at boundary");
            content.add("-Dfoo=bar");
            verifyParsing(content, List.of("-Dfoo=bar"));
        }
    }

    // ensure the arguments in the file are read in correctly
    private void verifyParsing(List<String> lines, List<String> args) throws IOException {
        File argFile = createArgFile(lines);
        String fname = "@" + argFile.getName();
        Map<String, String> env = new HashMap<>();
        env.put(JLDEBUG_KEY, "true");

        TestResult tr;
        if (args.contains("-version")) {
            tr = doExec(env, javaCmd, fname);
        } else {
            tr = doExec(env, javaCmd, fname, "-version");
        }
        tr.checkPositive();
        verifyOutput(args, tr);

        String lastArg = args.contains("-version") ? "-Dlast.arg" : "-version";
        tr = doExec(env, javaCmd, "-Xint", fname, lastArg);
        List<String> scratch = new ArrayList<>();
        scratch.add("-Xint");
        scratch.addAll(args);
        scratch.add(lastArg);
        verifyOutput(scratch, tr);

        argFile.delete();
    }

    @Test
    public void testSyntax() throws IOException {
        List<List<List<String>>> allcases = loadCases();
        for (List<List<String>> test: allcases) {
            verifyParsing(test.get(0), test.get(1));
        }
    }

    @Test
    public void badCases() throws IOException {
        List<String> lines = Arrays.asList(
            "-Dno.escape=\"Forgot to escape backslash\\\" -version");
        File argFile = createArgFile(lines);
        String fname = "@" + argFile.getName();
        Map<String, String> env = new HashMap<>();
        env.put(JLDEBUG_KEY, "true");

        TestResult tr = doExec(env, javaCmd, fname);
        tr.contains("argv[1] = -Dno.escape=Forgot to escape backslash\" -version");
        tr.checkNegative();
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }
        argFile.delete();
    }

    public static void main(String... args) throws Exception {
        ArgFileSyntax a = new ArgFileSyntax();
        a.run(args);
        if (testExitValue > 0) {
            System.out.println("Total of " + testExitValue + " failed");
            System.exit(1);
        } else {
            System.out.println("All tests pass");
        }
    }
}
