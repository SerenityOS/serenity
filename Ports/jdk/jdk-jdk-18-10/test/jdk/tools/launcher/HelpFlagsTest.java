/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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
 * @summary Validate and test -?, -h and --help flags. All tools in the jdk
 *          should take the same flags to display the help message. These
 *          flags should be documented in the printed help message. The
 *          tool should quit without error code after displaying the
 *          help message (if there  is no other problem with the command
 *          line).
 *          Also check that tools that used to accept -help still do
 *          so. Test that tools that never accepted -help don't do so
 *          in future. I.e., check that the tool returns with the same
 *          return code as called with an invalid flag, and does not
 *          print anything containing '-help' in that case.
 * @compile HelpFlagsTest.java
 * @run main HelpFlagsTest
 */

import java.io.File;

public class HelpFlagsTest extends TestHelper {

    // Tools that should not be tested because a usage message is pointless.
    static final String[] TOOLS_NOT_TO_TEST = {
        "appletviewer",     // deprecated, don't test
        "jaccessinspector", // gui, don't test, win only
        "jaccesswalker",    // gui, don't test, win only
        "jconsole",         // gui, don't test
        "servertool",       // none. Shell, don't test.
        "javaw",            // don't test, win only
        // These shall have a help message that resembles that of
        // MIT's tools. Thus -?, -h and --help are supported, but not
        // mentioned in the help text.
        "kinit",
        "klist",
        "ktab",
        // Oracle proprietary tools without help message.
        "javacpl",
        "jmc",
        "jweblauncher",
        "jcontrol",
        "ssvagent"
    };

    // Lists which tools support which flags.
    private static class ToolHelpSpec {
        String toolname;

        // How the flags supposed to be supported are handled.
        //
        // These flags are supported, i.e.,
        // * the tool accepts the flag
        // * the tool prints a help message if the flag is specified
        // * this help message lists the flag
        // * the tool exits with exit code '0'.
        boolean supportsQuestionMark;
        boolean supportsH;
        boolean supportsHelp;

        // One tool returns with exit code != '0'.
        int exitcodeOfHelp;

        // How legacy -help is handled.
        //
        // Tools that so far support -help should still do so, but
        // not print documentation about it. Tools that do not
        // support -help should not do so in future.
        //
        // The tools accepts legacy -help. -help should not be
        // documented in the usage message.
        boolean supportsLegacyHelp;

        // Java itself documents -help. -help prints to stderr,
        // while --help prints to stdout. Leave as is.
        boolean documentsLegacyHelp;

        // The exit code of the tool if an invalid argument is passed to it.
        // An exit code != 0 would be expected, but not all tools handle it
        // that way.
        int exitcodeOfWrongFlag;

        ToolHelpSpec(String n, int q, int h, int hp, int ex1, int l, int dl, int ex2) {
            toolname = n;
            supportsQuestionMark = ( q  == 1 ? true : false );
            supportsH            = ( h  == 1 ? true : false );
            supportsHelp         = ( hp == 1 ? true : false );
            exitcodeOfHelp       = ex1;

            supportsLegacyHelp   = (  l == 1 ? true : false );
            documentsLegacyHelp  = ( dl == 1 ? true : false );
            exitcodeOfWrongFlag  = ex2;
        }
    }

    static ToolHelpSpec[] jdkTools = {
        //               name          -?   -h --help exitcode   -help -help  exitcode
        //                                            of help          docu   of wrong
        //                                                             mented flag
        new ToolHelpSpec("jabswitch",   0,   0,   0,   0,         0,    0,     0),     // /?, prints help message anyways, win only
        new ToolHelpSpec("jar",         1,   1,   1,   0,         0,    0,     1),     // -?, -h, --help
        new ToolHelpSpec("jarsigner",   1,   1,   1,   0,         1,    0,     1),     // -?, -h, --help, -help accepted but not documented.
        new ToolHelpSpec("java",        1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("javac",       1,   0,   1,   0,         1,    1,     2),     // -?,     --help -help, Documents -help, -h is already taken for "native header output directory".
        new ToolHelpSpec("javadoc",     1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("javap",       1,   1,   1,   0,         1,    1,     2),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("javaw",       1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help, win only
        new ToolHelpSpec("jcmd",        1,   1,   1,   0,         1,    0,     1),     // -?, -h, --help, -help accepted but not documented.
        new ToolHelpSpec("jdb",         1,   1,   1,   0,         1,    1,     0),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("jdeprscan",   1,   1,   1,   0,         0,    0,     1),     // -?, -h, --help
        new ToolHelpSpec("jdeps",       1,   1,   1,   0,         1,    0,     2),     // -?, -h, --help, -help accepted but not documented.
        new ToolHelpSpec("jfr",         1,   1,   1,   0,         0,    0,     2),     // -?, -h, --help
        new ToolHelpSpec("jhsdb",       0,   0,   0,   0,         0,    0,     0),     // none, prints help message anyways.
        new ToolHelpSpec("jimage",      1,   1,   1,   0,         0,    0,     2),     // -?, -h, --help
        new ToolHelpSpec("jinfo",       1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("jjs",         0,   1,   1, 100,         0,    0,   100),     //     -h, --help, return code 100
        new ToolHelpSpec("jlink",       1,   1,   1,   0,         0,    0,     2),     // -?, -h, --help
        new ToolHelpSpec("jmap",        1,   1,   1,   0,         1,    0,     1),     // -?, -h, --help, -help accepted but not documented.
        new ToolHelpSpec("jmod",        1,   1,   1,   0,         1,    0,     2),     // -?, -h, --help, -help accepted but not documented.
        new ToolHelpSpec("jps",         1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("jrunscript",  1,   1,   1,   0,         1,    1,     7),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("jshell",      1,   1,   1,   0,         1,    0,     1),     // -?, -h, --help, -help accepted but not documented.
        new ToolHelpSpec("jstack",      1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("jstat",       1,   1,   1,   0,         1,    1,     1),     // -?, -h, --help -help, Documents -help
        new ToolHelpSpec("jstatd",      1,   1,   1,   0,         0,    0,     1),     // -?, -h, --help
        new ToolHelpSpec("keytool",     1,   1,   1,   0,         1,    0,     1),     // none, prints help message anyways.
        new ToolHelpSpec("rmiregistry", 0,   0,   0,   0,         0,    0,     1),     // none, prints help message anyways.
        new ToolHelpSpec("serialver",   0,   0,   0,   0,         0,    0,     1),     // none, prints help message anyways.
        new ToolHelpSpec("jpackage",    0,   1,   1,   0,         0,    1,     1),     //     -h, --help,
    };

    // Returns corresponding object from jdkTools array.
    static ToolHelpSpec getToolHelpSpec(String tool) {
        for (ToolHelpSpec x : jdkTools) {
            if (tool.toLowerCase().equals(x.toolname) ||
                tool.toLowerCase().equals(x.toolname + ".exe"))
                return x;
        }
        return null;
    }

    // Check whether 'flag' appears in 'line' as a word of itself. It must not
    // be a substring of a word, as then similar flags might be matched.
    // E.g.: --help matches in the documentation of --help-extra.
    // This works only with english locale, as some tools have translated
    // usage messages.
    static boolean findFlagInLine(String line, String flag) {
        if (line.contains(flag) &&
            !line.contains("nknown") &&                       // Some tools say 'Unknown option "<flag>"',
            !line.contains("invalid flag") &&                 // 'invalid flag: <flag>'
            !line.contains("invalid option") &&               // or 'invalid option: <flag>'. Skip that.
            !line.contains("FileNotFoundException: -help") && // Special case for idlj.
            !line.contains("-h requires an argument") &&      // Special case for javac.
            !line.contains("port argument,")) {               // Special case for rmiregistry.
            // There might be several appearances of 'flag' in
            // 'line'. (-h as substring of --help).
            int flagLen = flag.length();
            int lineLen = line.length();
            for (int i = line.indexOf(flag); i >= 0; i = line.indexOf(flag, i+1)) {
                // There should be a space before 'flag' in 'line', or it's right at the beginning.
                if (i > 0 &&
                    line.charAt(i-1) != ' ' &&
                    line.charAt(i-1) != '[' &&  // jarsigner
                    line.charAt(i-1) != '|' &&  // jstatd
                    line.charAt(i-1) != '\t') { // jjs
                    continue;
                }
                // There should be a space or comma after 'flag' in 'line', or it's just at the end.
                int posAfter = i + flagLen;
                if (posAfter < lineLen &&
                    line.charAt(posAfter) != ' ' &&
                    line.charAt(posAfter) != ',' &&
                    line.charAt(posAfter) != '[' && // jar
                    line.charAt(posAfter) != ']' && // jarsigner
                    line.charAt(posAfter) != ')' && // jfr
                    line.charAt(posAfter) != '|' && // jstatd
                    line.charAt(posAfter) != ':' && // jps
                    line.charAt(posAfter) != '"') { // keytool
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    static TestResult runToolWithFlag(File f, String flag) {
        String x = f.getAbsolutePath();
        TestResult tr = doExec(x, flag);
        System.out.println("Testing " + f.getName());
        System.out.println("#> " + x + " " + flag);
        tr.testOutput.forEach(System.out::println);
        System.out.println("#> echo $?");
        System.out.println(tr.exitValue);

        return tr;
    }

    // Checks whether tool supports flag 'flag' and documents it
    // in the help message.
    static String testTool(File f, String flag, int exitcode) {
        String result = "";
        TestResult tr = runToolWithFlag(f, flag);

        // Check that the tool accepted the flag.
        if (exitcode == 0 && !tr.isOK()) {
            System.out.println("failed");
            result = "failed: " + f.getName() + " " + flag + " has exit code " + tr.exitValue + ".\n";
        }

        // Check there is a help message listing the flag.
        boolean foundFlag = false;
        for (String y : tr.testOutput) {
            if (!foundFlag && findFlagInLine(y, flag)) { // javac
                foundFlag = true;
                System.out.println("Found documentation of '" + flag + "': '" + y.trim() +"'");
            }
        }
        if (!foundFlag) {
            result += "failed: " + f.getName() + " does not document " +
                flag + " in help message.\n";
        }

        if (!result.isEmpty())
            System.out.println(result);

        return result;
    }

    // Test the tool supports legacy option -help, but does
    // not document it.
    static String testLegacyFlag(File f, int exitcode) {
        String result = "";
        TestResult tr = runToolWithFlag(f, "-help");

        // Check that the tool accepted the flag.
        if (exitcode == 0 && !tr.isOK()) {
            System.out.println("failed");
            result = "failed: " + f.getName() + " -help has exit code " + tr.exitValue + ".\n";
        }

        // Check there is _no_ documentation of -help.
        boolean foundFlag = false;
        for (String y : tr.testOutput) {
            if (!foundFlag && findFlagInLine(y, "-help")) {  // javac
                foundFlag = true;
                System.out.println("Found documentation of '-help': '" + y.trim() +"'");
            }
        }
        if (foundFlag) {
            result += "failed: " + f.getName() + " does document -help " +
                "in help message. This legacy flag should not be documented.\n";
        }

        if (!result.isEmpty())
            System.out.println(result);

        return result;
    }

    // Test that the tool exits with the exit code expected for
    // invalid flags. In general, one would expect this to be != 0,
    // but currently a row of tools exit with 0 in this case.
    // The output should not ask to get help with flag '-help'.
    static String testInvalidFlag(File f, String flag, int exitcode, boolean documentsLegacyHelp) {
        String result = "";
        TestResult tr = runToolWithFlag(f, flag);

        // Check that the tool did exit with the expected return code.
        if (!((exitcode == tr.exitValue) ||
              // Windows reports -1 where unix reports 255.
              (tr.exitValue < 0 && exitcode == tr.exitValue + 256))) {
            System.out.println("failed");
            result = "failed: " + f.getName() + " " + flag + " should not be " +
                     "accepted. But it has exit code " + tr.exitValue + ".\n";
        }

        if (!documentsLegacyHelp) {
            // Check there is _no_ documentation of -help.
            boolean foundFlag = false;
            for (String y : tr.testOutput) {
                if (!foundFlag && findFlagInLine(y, "-help")) {  // javac
                    foundFlag = true;
                    System.out.println("Found documentation of '-help': '" + y.trim() +"'");
                }
            }
            if (foundFlag) {
                result += "failed: " + f.getName() + " does document -help " +
                    "in error message. This legacy flag should not be documented.\n";
            }
        }

        if (!result.isEmpty())
            System.out.println(result);

        return result;
    }

    public static void main(String[] args) {
        String errorMessage = "";

        // The test analyses the help messages printed. It assumes englisch
        // help messages. Thus it only works with english locale.
        if (!isEnglishLocale()) { return; }

        for (File f : new File(JAVA_BIN).listFiles(new ToolFilter(TOOLS_NOT_TO_TEST))) {
            String toolName = f.getName();

            ToolHelpSpec tool = getToolHelpSpec(toolName);
            if (tool == null) {
                errorMessage += "Tool " + toolName + " not covered by this test. " +
                    "Add specification to jdkTools array!\n";
                continue;
            }

            // Test for help flags to be supported.
            if (tool.supportsQuestionMark == true) {
                errorMessage += testTool(f, "-?", tool.exitcodeOfHelp);
            } else {
                System.out.println("Skip " + tool.toolname + ". It does not support -?.");
            }
            if (tool.supportsH == true) {
                errorMessage += testTool(f, "-h", tool.exitcodeOfHelp);
            } else {
                System.out.println("Skip " + tool.toolname + ". It does not support -h.");
            }
            if (tool.supportsHelp == true) {
                errorMessage += testTool(f, "--help", tool.exitcodeOfHelp);
            } else {
                System.out.println("Skip " + tool.toolname + ". It does not support --help.");
            }

            // Check that the return code listing in jdkTools[] is
            // correct for an invalid flag.
            errorMessage += testInvalidFlag(f, "-asdfxgr", tool.exitcodeOfWrongFlag, tool.documentsLegacyHelp);

            // Test for legacy -help flag.
            if (!tool.documentsLegacyHelp) {
                if (tool.supportsLegacyHelp == true) {
                    errorMessage += testLegacyFlag(f, tool.exitcodeOfHelp);
                } else {
                    errorMessage += testInvalidFlag(f, "-help", tool.exitcodeOfWrongFlag, false);
                }
            }
        }

        if (errorMessage.isEmpty()) {
            System.out.println("All help string tests: PASS");
        } else {
            throw new AssertionError("HelpFlagsTest failed:\n" + errorMessage);
        }
    }
}
