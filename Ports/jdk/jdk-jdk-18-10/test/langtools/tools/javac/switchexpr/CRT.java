/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214031
 * @summary Test the CharacterRangeTable generated for switch expressions
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.Assert toolbox.ToolBox toolbox.JavacTask
 * @run main CRT
 */


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task.OutputKind;
import toolbox.ToolBox;

public class CRT {
    public static void main(String... args) throws Exception {
        new CRT().run();
    }

    private static final String SOURCE_VERSION = Integer.toString(Runtime.version().feature());

    private ToolBox tb = new ToolBox();

    private void run() throws Exception {
        doTest("    private String convert(int i) {\n" +
               "        String res;" +
               "        switch (i) {\n" +
               "            case 0: res = \"a\"; break;\n" +
               "            default: res = \"\"; break;\n" +
               "        };\n" +
               "        return res;\n" +
               "    }\n",
               "CharacterRangeTable:\n" +
               "             0,  0,    c24,    c25,    8        //  0,  0,    3:36,    3:37, flow-controller\n" +
               "            20, 22,   1015,   101f,    1        // 20, 22,    4:21,    4:31, statement\n" +
               "            23, 25,   1020,   1026,    1        // 23, 25,    4:32,    4:38, statement\n" +
               "            20, 25,   1015,   1026,   10        // 20, 25,    4:21,    4:38, flow-target\n" +
               "            26, 28,   1416,   141f,    1        // 26, 28,    5:22,    5:31, statement\n" +
               "            29, 31,   1420,   1426,    1        // 29, 31,    5:32,    5:38, statement\n" +
               "            26, 31,   1416,   1426,   10        // 26, 31,    5:22,    5:38, flow-target\n" +
               "             0, 31,    c1c,   180a,    1        //  0, 31,    3:28,    6:10, statement\n" +
               "            32, 33,   1c09,   1c14,    1        // 32, 33,    7:09,    7:20, statement\n" +
               "             0, 33,    823,   2006,    2        //  0, 33,    2:35,    8:06, block\n");
        doTest("    private String convert(int i) {\n" +
               "        return switch (i) {\n" +
               "            case 0 -> \"a\";\n" +
               "            default -> \"\";\n" +
               "        };\n" +
               "    }\n",
               "CharacterRangeTable:\n" +
               "             0,  0,    c18,    c19,    8        //  0,  0,    3:24,    3:25, flow-controller\n" +
               "            20, 24,   1017,   101b,   11        // 20, 24,    4:23,    4:27, statement, flow-target\n" +
               "            25, 29,   1418,   141b,   11        // 25, 29,    5:24,    5:27, statement, flow-target\n" +
               "             0, 30,    c09,   180b,    1        //  0, 30,    3:09,    6:11, statement\n" +
               "             0, 30,    823,   1c06,    2        //  0, 30,    2:35,    7:06, block");
        doTest("    private boolean convert(int i) {\n" +
               "        return switch (i) {\n" +
               "            case 0 -> true;\n" +
               "            default -> false;\n" +
               "        } && i == 0;\n" +
               "    }\n",
               "CharacterRangeTable:\n" +
               "             0,  0,    c18,    c19,    8        //  0,  0,    3:24,    3:25, flow-controller\n" +
               "            20, 22,   1017,   101c,   11        // 20, 22,    4:23,    4:28, statement, flow-target\n" +
               "            23, 25,   1418,   141e,   11        // 23, 25,    5:24,    5:30, statement, flow-target\n" +
               "             0, 25,    c10,   180a,    8        //  0, 25,    3:16,    6:10, flow-controller\n" +
               "            26, 26,   180e,   1814,   10        // 26, 26,    6:14,    6:20, flow-target\n" +
               "             0, 35,    c09,   1815,    1        //  0, 35,    3:09,    6:21, statement\n" +
               "             0, 35,    824,   1c06,    2        //  0, 35,    2:36,    7:06, block\n");
        doTest("    private boolean convert(int i) {\n" +
               "        return i >= 0 ? i == 0\n" +
               "                        ? true\n" +
               "                        : false\n" +
               "                      : i == -1\n" +
               "                        ? false\n" +
               "                        : true;\n" +
               "    }\n",
               "CharacterRangeTable:\n" +
               "             0,  0,    c10,    c16,    8        //  0,  0,    3:16,    3:22, flow-controller\n" +
               "             1,  3,    c10,    c16,  100        //  1,  3,    3:16,    3:22, branch-false\n" +
               "             4,  4,    c19,    c1f,    8        //  4,  4,    3:25,    3:31, flow-controller\n" +
               "             5,  7,    c19,    c1f,  100        //  5,  7,    3:25,    3:31, branch-false\n" +
               "             8,  8,   101b,   101f,   10        //  8,  8,    4:27,    4:31, flow-target\n" +
               "            12, 12,   141b,   1420,   10        // 12, 12,    5:27,    5:32, flow-target\n" +
               "             4, 12,    c19,   1420,   10        //  4, 12,    3:25,    5:32, flow-target\n" +
               "            16, 17,   1819,   1820,    8        // 16, 17,    6:25,    6:32, flow-controller\n" +
               "            18, 20,   1819,   1820,  100        // 18, 20,    6:25,    6:32, branch-false\n" +
               "            21, 21,   1c1b,   1c20,   10        // 21, 21,    7:27,    7:32, flow-target\n" +
               "            25, 25,   201b,   201f,   10        // 25, 25,    8:27,    8:31, flow-target\n" +
               "            16, 25,   1819,   201f,   10        // 16, 25,    6:25,    8:31, flow-target\n" +
               "             0, 26,    c09,   2020,    1        //  0, 26,    3:09,    8:32, statement\n" +
               "             0, 26,    824,   2406,    2        //  0, 26,    2:36,    9:06, block\n");
        doTest("    private boolean convert(int i) {\n" +
               "        return i >= 0 ? switch (i) {\n" +
               "            case 0 -> true;\n" +
               "            default -> false;\n" +
               "        } : switch (i) {\n" +
               "            case -1 -> false;\n" +
               "            default -> true;\n" +
               "        };\n" +
               "    }\n",
               "CharacterRangeTable:\n" +
               "             0,  0,    c10,    c16,    8        //  0,  0,    3:16,    3:22, flow-controller\n" +
               "             1,  3,    c10,    c16,  100        //  1,  3,    3:16,    3:22, branch-false\n" +
               "             4,  4,    c21,    c22,    8        //  4,  4,    3:33,    3:34, flow-controller\n" +
               "            24, 27,   1017,   101c,   11        // 24, 27,    4:23,    4:28, statement, flow-target\n" +
               "            28, 31,   1418,   141e,   11        // 28, 31,    5:24,    5:30, statement, flow-target\n" +
               "             4, 31,    c19,   180a,   10        //  4, 31,    3:25,    6:10, flow-target\n" +
               "            35, 35,   1815,   1816,    8        // 35, 35,    6:21,    6:22, flow-controller\n" +
               "            56, 59,   1c18,   1c1e,   11        // 56, 59,    7:24,    7:30, statement, flow-target\n" +
               "            60, 63,   2018,   201d,   11        // 60, 63,    8:24,    8:29, statement, flow-target\n" +
               "            35, 63,   180d,   240a,   10        // 35, 63,    6:13,    9:10, flow-target\n" +
               "             0, 64,    c09,   240b,    1        //  0, 64,    3:09,    9:11, statement\n" +
               "             0, 64,    824,   2806,    2        //  0, 64,    2:36,   10:06, block\n");
        doTest(
                """
                private boolean convert(int i) {
                    return switch (i) {
                        default -> (i < 256) ? true : false;
                    };
                }
                """,
                """
                CharacterRangeTable:
                             0,  0,    c14,    c15,    8        //  0,  0,    3:20,    3:21, flow-controller
                            12, 15,   1014,   101d,    8        // 12, 15,    4:20,    4:29, flow-controller
                            16, 18,   1014,   101d,  100        // 16, 18,    4:20,    4:29, branch-false
                            19, 19,   1020,   1024,   10        // 19, 19,    4:32,    4:36, flow-target
                            23, 23,   1027,   102c,   10        // 23, 23,    4:39,    4:44, flow-target
                            12, 26,   1014,   102d,   11        // 12, 26,    4:20,    4:45, statement, flow-target
                             0, 27,    c05,   1407,    1        //  0, 27,    3:05,    5:07, statement
                             0, 27,    820,   1802,    2        //  0, 27,    2:32,    6:02, block
                """
        );
        doTest(
                """
                private boolean convert(int i) {
                    return switch (i) {
                        case 1 -> switch (Integer.toString(i)) {
                            case "1" -> true;
                            default -> throw new IllegalStateException("failure");
                        };
                        default -> throw new IllegalStateException("failure");
                    };
                }
                """,
                """
                CharacterRangeTable:
                             0,  0,    c14,    c15,    8        //  0,  0,    3:20,    3:21, flow-controller
                            20, 24,   1013,   102f,    1        // 20, 24,    4:19,    4:47, statement
                            80, 83,   1419,   141e,   11        // 80, 83,    5:25,    5:30, statement, flow-target
                            84, 93,   1818,   1843,   11        // 84, 93,    6:24,    6:67, statement, flow-target
                            20, 96,   1013,   1c0b,   11        // 20, 96,    4:19,    7:11, statement, flow-target
                            97, 106,   2014,   203f,   11       // 97, 106,    8:20,    8:63, statement, flow-target
                             0, 107,    c05,   2407,    1       //  0, 107,    3:05,    9:07, statement
                             0, 107,    820,   2802,    2       //  0, 107,    2:32,   10:02, block
                """
        );
    }

    private void doTest(String code, String expected) throws Exception {
        Path base = Paths.get(".");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);
        tb.cleanDirectory(classes);
        new JavacTask(tb)
                .options("-Xjcov")
                .outdir(classes)
                .sources("public class Test {\n" +
                         code +
                         "}\n")
                .run()
                .writeAll();
        String out = new JavapTask(tb)
                .options("-private",
                         "-verbose",
                         "-s")
                .classpath(classes.toString())
                .classes("Test")
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT)
                .stream()
                .collect(Collectors.joining("\n"));
        String crt = cutCRT(out);
        if (!expected.trim().equals(crt.trim())) {
            throw new AssertionError("Expected CharacterRangeTable not found, found: " + crt);
        }
    }

    private static String cutCRT(String from) {
        int start = from.indexOf("CharacterRangeTable:", from.indexOf("convert(int);"));
        int end = from.indexOf("StackMapTable:");
        return from.substring(start, end);
    }

}
