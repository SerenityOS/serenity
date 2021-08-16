/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8157395 8157393 8157517 8158738 8167128 8163840 8167637 8170368 8172102 8172179 8177847
 * @summary Tests of jshell comand options, and undoing operations
 * @modules jdk.jshell/jdk.internal.jshell.tool
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build ToolCommandOptionTest ReplToolTesting
 * @run testng ToolCommandOptionTest
 */
import java.nio.file.Path;
import org.testng.annotations.Test;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class ToolCommandOptionTest extends ReplToolTesting {

    public void listTest() {
        test(
                (a) -> assertCommand(a, "int x;",
                        "x ==> 0"),
                (a) -> assertCommand(a, "/li",
                        "1 : int x;"),
                (a) -> assertCommandOutputStartsWith(a, "/lis -st",
                        "s1 : import"),
                (a) -> assertCommandOutputStartsWith(a, "/list -all",
                        "s1 : import"),
                (a) -> assertCommandOutputContains(a, "/list -all",
                        "1 : int x;"),
                (a) -> assertCommandOutputContains(a, "/list -history",
                        "int x;"),
                (a) -> assertCommandOutputContains(a, "/li -h",
                        "/lis -st"),
                (a) -> assertCommand(a, "/list -furball",
                        "|  Unknown option: -furball -- /list -furball"),
                (a) -> assertCommand(a, "/list x",
                        "1 : int x;"),
                (a) -> assertCommand(a, "/li x -start",
                        "|  Options and snippets must not both be used: /list x -start"),
                (a) -> assertCommand(a, "/l -st -al",
                        "|  Conflicting options -- /list -st -al")
        );
    }

    public void typesTest() {
        test(
                (a) -> assertCommand(a, "int x",
                        "x ==> 0"),
                (a) -> assertCommand(a, "/types x",
                        "|  This command does not accept the snippet 'x' : int x;"),
                (a) -> assertCommand(a, "class C {}",
                        "|  created class C"),
                (a) -> assertCommand(a, "/ty",
                        "|    class C"),
                (a) -> assertCommand(a, "/ty -st",
                        ""),
                (a) -> assertCommand(a, "/types -all",
                        "|    class C"),
                (a) -> assertCommand(a, "/types -furball",
                        "|  Unknown option: -furball -- /types -furball"),
                (a) -> assertCommand(a, "/types C",
                        "|    class C"),
                (a) -> assertCommand(a, "/types C -start",
                        "|  Options and snippets must not both be used: /types C -start"),
                (a) -> assertCommand(a, "/ty -st -al",
                        "|  Conflicting options -- /types -st -al")
        );
    }

    public void dropTest() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertCommand(a, "int x = 5;",
                        "x ==> 5"),
                (a) -> assertCommand(a, "x",
                        "x ==> 5"),
                (a) -> assertCommand(a, "long y;",
                        "y ==> 0"),
                (a) -> assertCommand(a, "/drop -furball",
                        "|  Unknown option: -furball -- /drop -furball"),
                (a) -> assertCommand(a, "/drop -all",
                        "|  Unknown option: -all -- /drop -all"),
                (a) -> assertCommandOutputStartsWith(a, "/drop z",
                        "|  No such snippet: z"),
                (a) -> assertCommand(a, "/drop 2",
                        ""),
                (a) -> assertCommandOutputStartsWith(a, "23qwl",
                        "|  Error:"),
                (a) -> assertCommandOutputStartsWith(a, "/drop e1",
                        "|  This command does not accept the snippet 'e1' : 23qwl"),
                (a) -> assertCommand(a, "/dr x y",
                        "|  dropped variable x\n" +
                        "|  dropped variable y"),
                (a) -> assertCommand(a, "/list",
                        "")
        );
    }

    public void setEditorTest() {
        test(
                (a) -> assertCommand(a, "/set editor -furball",
                        "|  Unknown option: -furball -- /set editor -furball"),
                (a) -> assertCommand(a, "/set editor -furball prog",
                        "|  Unknown option: -furball -- /set editor -furball prog"),
                (a) -> assertCommand(a, "/set editor -furball -mattress",
                        "|  Unknown option: -furball -mattress -- /set editor -furball -mattress"),
                (a) -> assertCommand(a, "/set editor -default prog",
                        "|  Specify -default option, -delete option, or program -- /set editor -default prog"),
                (a) -> assertCommand(a, "/set editor prog",
                        "|  Editor set to: prog"),
                (a) -> assertCommand(a, "/set editor prog -default",
                        "|  Editor set to: prog -default"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor prog -default"),
                (a) -> assertCommand(a, "/se ed prog -furball",
                        "|  Editor set to: prog -furball"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor prog -furball"),
                (a) -> assertCommand(a, "/se ed -delete",
                        "|  Editor set to: -default"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -default"),
                (a) -> assertCommand(a, "/set editor prog arg1 -furball arg3 -default arg4",
                        "|  Editor set to: prog arg1 -furball arg3 -default arg4"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor prog arg1 -furball arg3 -default arg4"),
                (a) -> assertCommand(a, "/set editor -default",
                        "|  Editor set to: -default"),
                (a) -> assertCommand(a, "/se edi -def",
                        "|  Editor set to: -default"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -default")
        );
    }

    public void retainEditorTest() {
        test(
                (a) -> assertCommand(a, "/set editor -retain -furball",
                        "|  Unknown option: -furball -- /set editor -retain -furball"),
                (a) -> assertCommand(a, "/set editor -retain -furball prog",
                        "|  Unknown option: -furball -- /set editor -retain -furball prog"),
                (a) -> assertCommand(a, "/set editor -retain -furball -mattress",
                        "|  Unknown option: -furball -mattress -- /set editor -retain -furball -mattress"),
                (a) -> assertCommand(a, "/set editor -retain -default prog",
                        "|  Specify -default option, -delete option, or program -- /set editor -retain -default prog"),
                (a) -> assertCommand(a, "/set editor -retain -wait",
                        "|  -wait applies to external editors"),
                (a) -> assertCommand(a, "/set editor -retain -default -wait",
                        "|  -wait applies to external editors"),
                (a) -> assertCommand(a, "/set editor -retain prog",
                        "|  Editor set to: prog\n" +
                        "|  Editor setting retained: prog"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain prog"),
                (a) -> assertCommand(a, "/se ed other",
                        "|  Editor set to: other"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain prog\n" +
                        "|  /set editor other"),
                (a) -> assertCommand(a, "/se ed -delete",
                        "|  Editor set to: prog"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain prog"),
                (a) -> assertCommand(a, "/set editor -retain prog -default",
                        "|  Editor set to: prog -default\n" +
                        "|  Editor setting retained: prog -default"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain prog -default"),
                (a) -> assertCommand(a, "/se ed -retain prog -furball",
                        "|  Editor set to: prog -furball\n" +
                        "|  Editor setting retained: prog -furball"),
                (a) -> assertCommand(a, "/set editor -retain prog arg1 -furball arg3 -default arg4",
                        "|  Editor set to: prog arg1 -furball arg3 -default arg4\n" +
                        "|  Editor setting retained: prog arg1 -furball arg3 -default arg4"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain prog arg1 -furball arg3 -default arg4"),
                (a) -> assertCommand(a, "/set editor -retain -default",
                        "|  Editor set to: -default\n" +
                        "|  Editor setting retained: -default"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain -default"),
                (a) -> assertCommand(a, "/se e -ret -def",
                        "|  Editor set to: -default\n" +
                        "|  Editor setting retained: -default"),
                (a) -> assertCommand(a, "/set editor -retain",
                        "|  Editor setting retained: -default")
        );
    }

    public void setEditorEnvTest() {
        setEnvVar("EDITOR", "best one");
        setEditorEnvSubtest();
        setEnvVar("EDITOR", "not this");
        setEnvVar("VISUAL", "best one");
        setEditorEnvSubtest();
        setEnvVar("VISUAL", "not this");
        setEnvVar("JSHELLEDITOR", "best one");
        setEditorEnvSubtest();
    }

    private void setEditorEnvSubtest() {
        test(
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor best one"),
                (a) -> assertCommand(a, "/set editor prog",
                        "|  Editor set to: prog"),
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor prog"),
                (a) -> assertCommand(a, "/set editor -delete",
                        "|  Editor set to: best one"),
                (a) -> assertCommand(a, "/set editor -retain stored editor",
                        "|  Editor set to: stored editor\n" +
                        "|  Editor setting retained: stored editor")
        );
        test(
                (a) -> assertCommand(a, "/set editor",
                        "|  /set editor -retain stored editor"),
                (a) -> assertCommand(a, "/set editor -delete -retain",
                        "|  Editor set to: best one")
        );
    }

    public void setStartTest() {
        Compiler compiler = new Compiler();
        Path startup = compiler.getPath("StartTest/startup.txt");
        compiler.writeToFile(startup, "int iAmHere = 1234;");
        test(
                (a) -> assertCommand(a, "/set start -furball",
                        "|  Unknown option: -furball -- /set start -furball"),
                (a) -> assertCommand(a, "/set start -furball pyle",
                        "|  Unknown option: -furball -- /set start -furball pyle"),
                (a) -> assertCommand(a, "/se st pyle -furball",
                        "|  Unknown option: -furball -- /set st pyle -furball"),
                (a) -> assertCommand(a, "/set start -furball -mattress",
                        "|  Unknown option: -furball -mattress -- /set start -furball -mattress"),
                (a) -> assertCommand(a, "/set start foo -default",
                        "|  Specify no more than one of -default, -none, or a startup file name -- /set start foo -default"),
                (a) -> assertCommand(a, "/set start frfg",
                        "|  File 'frfg' for '/set start' is not found."),
                (a) -> assertCommand(a, "/set start DEFAULT frfg",
                        "|  File 'frfg' for '/set start' is not found."),
                (a) -> assertCommand(a, "/set start -default",
                        ""),
                (a) -> assertCommand(a, "/set start",
                        "|  /set start -default"),
                (a) -> assertCommand(a, "/set start DEFAULT",
                        ""),
                (a) -> assertCommand(a, "/set start",
                        "|  /set start -default"),
                (a) -> assertCommand(a, "/set start DEFAULT PRINTING",
                        ""),
                (a) -> assertCommandOutputContains(a, "/set start",
                        "/set start DEFAULT PRINTING", "void println", "import java.util.*"),
                (a) -> assertCommand(a, "/set start " + startup.toString(),
                        ""),
                (a) -> assertCommandOutputContains(a, "/set start",
                        "|  /set start " + startup + "\n" +
                        "|  ---- " + startup + " @ ", " ----\n" +
                        "|  int iAmHere = 1234;\n"),
                (a) -> assertCommand(a, "/se sta -no",
                        ""),
                (a) -> assertCommand(a, "/set start",
                        "|  /set start -none")
        );
    }

    public void retainStartTest() {
        Compiler compiler = new Compiler();
        Path startup = compiler.getPath("StartTest/startup.txt");
        compiler.writeToFile(startup, "int iAmHere = 1234;");
        test(
                (a) -> assertCommand(a, "/set start -retain -furball",
                        "|  Unknown option: -furball -- /set start -retain -furball"),
                (a) -> assertCommand(a, "/set start -retain -furball pyle",
                        "|  Unknown option: -furball -- /set start -retain -furball pyle"),
                (a) -> assertCommand(a, "/se st -re pyle -furball",
                        "|  Unknown option: -furball -- /set st -re pyle -furball"),
                (a) -> assertCommand(a, "/set start -retain -furball -mattress",
                        "|  Unknown option: -furball -mattress -- /set start -retain -furball -mattress"),
                (a) -> assertCommand(a, "/set start -retain foo -default",
                        "|  Specify no more than one of -default, -none, or a startup file name -- /set start -retain foo -default"),
                (a) -> assertCommand(a, "/set start -retain -default foo",
                        "|  Specify no more than one of -default, -none, or a startup file name -- /set start -retain -default foo"),
                (a) -> assertCommand(a, "/set start -retain frfg",
                        "|  File 'frfg' for '/set start' is not found."),
                (a) -> assertCommand(a, "/set start -retain -default",
                        ""),
                (a) -> assertCommand(a, "/set start",
                        "|  /set start -retain -default"),
                (a) -> assertCommand(a, "/set sta -no",
                        ""),
                (a) -> assertCommand(a, "/set start",
                        "|  /set start -retain -default\n" +
                        "|  /set start -none"),
                (a) -> assertCommand(a, "/se st -ret",
                        ""),
                (a) -> assertCommand(a, "/se sta",
                        "|  /set start -retain -none"),
                (a) -> assertCommand(a, "/set start -retain " + startup.toString(),
                        ""),
                (a) -> assertCommand(a, "/set start DEFAULT PRINTING",
                        ""),
                (a) -> assertCommandOutputStartsWith(a, "/set start",
                        "|  /set start -retain " + startup.toString() + "\n" +
                        "|  /set start DEFAULT PRINTING\n" +
                        "|  ---- " + startup.toString() + " @ "),
                (a) -> assertCommandOutputContains(a, "/set start",
                        "|  ---- DEFAULT ----\n",
                        "|  ---- PRINTING ----\n",
                        "|  int iAmHere = 1234;\n",
                        "|  void println(String s)",
                        "|  import java.io.*;")
        );
    }

    public void setModeTest() {
        test(
                (a) -> assertCommandOutputContains(a, "/set mode",
                        "|  /set format verbose unresolved"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode *",
                        "|  Expected a feedback mode name: *"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -quiet",
                        "|  Missing the feedback mode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -quiet *",
                        "|  Expected a feedback mode name: *"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode amode normal thing",
                        "|  Unexpected arguments at end of command: thing"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode",
                        "|  To create a new mode either the -command or the -quiet option must be used"),
                (a) -> assertCommand(a, "/set mode mymode -command",
                        "|  Created new feedback mode: mymode"),
                (a) -> assertCommand(a, "/set mode mymode -delete",
                        ""),
                (a) -> assertCommand(a, "/set mode mymode normal -command",
                        "|  Created new feedback mode: mymode"),
                (a) -> assertCommand(a, "/set mode -del mymode",
                        ""),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -command -quiet",
                        "|  Conflicting options"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -delete -quiet",
                        "|  Conflicting options"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -command -delete",
                        "|  Conflicting options"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -d",
                        "|  No feedback mode named: mymode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode normal -c",
                        "|  Mode to be created already exists: normal"),
                (a) -> assertCommand(a, "/se mo -c mymode",
                        "|  Created new feedback mode: mymode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode",
                        "|  /set mode mymode -command"),
                (a) -> assertCommand(a, "/set feedback mymode",
                        "|  Feedback mode: mymode"),
                (a) -> assertCommand(a, "/se fe",
                        "|  /set feedback mymode\n" +
                        "|  \n" +
                        "|  Available feedback modes:\n" +
                        "|     concise\n" +
                        "|     mymode\n" +
                        "|     normal\n" +
                        "|     silent\n" +
                        "|     verbose"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -delete",
                        "|  The current feedback mode 'mymode' cannot be deleted"),
                (a) -> assertCommand(a, "/set feedback no",
                        "|  Feedback mode: normal"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -delete",
                        ""),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode",
                        "|  To create a new mode either the -command or the -quiet option must be used -- \n" +
                        "|  Does not match any current feedback mode: mymode -- /set mode mymode\n" +
                        "|  Available feedback modes:"),
                (a) -> assertCommandCheckOutput(a, "/set feedback",
                        (s) -> assertFalse(s.contains("mymode"), "Didn't delete: " + s))
        );
    }

    public void setModeSmashTest() {
        test(
                (a) -> assertCommand(a, "/set mode mymode -command",
                        "|  Created new feedback mode: mymode"),
                (a) -> assertCommand(a, "/set feedback mymode",
                        "|  Feedback mode: mymode"),
                (a) -> assertCommand(a, "/set format mymode display 'blurb'",
                        ""),
                (a) -> assertCommand(a, "45",
                        "blurb"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode normal",
                        "|  To create a new mode either the -command or the -quiet option must be used"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -command normal",
                        "|  Mode to be created already exists: mymode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode mymode -delete",
                        "|  The current feedback mode 'mymode' cannot be deleted, use '/set feedback' first"),
                (a) -> assertCommand(a, "/set feedback normal",
                        "|  Feedback mode: normal"),
                (a) -> assertCommand(a, "/set mode mymode -delete",
                        ""),
                (a) -> assertCommand(a, "/set mode mymode -command normal",
                        "|  Created new feedback mode: mymode"),
                (a) -> assertCommand(a, "/set feedback mymode",
                        "|  Feedback mode: mymode"),
                (a) -> assertCommandOutputContains(a, "45",
                        " ==> 45")
        );
    }

    public void retainModeTest() {
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain",
                        "|  Missing the feedback mode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain *",
                        "|  Expected a feedback mode name: *"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain amode normal",
                        "|  Unexpected arguments at end of command: normal"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain mymode",
                        "|  No feedback mode named: mymode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain mymode -delete",
                        "|  No feedback mode named: mymode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain -d mymode",
                        "|  No feedback mode named: mymode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain normal",
                        "|  Not valid with a predefined mode: normal"),
                (a) -> assertCommand(a, "/set mode mymode verbose -command",
                        "|  Created new feedback mode: mymode"),
                (a) -> assertCommand(a, "/set mode -retain mymode",
                        ""),
                (a) -> assertCommand(a, "/set mode mymode -delete",
                        ""),
                (a) -> assertCommand(a, "/set mode -retain mymode -delete",
                        ""),
                (a) -> assertCommand(a, "/set mode kmode normal -command",
                        "|  Created new feedback mode: kmode"),
                (a) -> assertCommand(a, "/set mode -retain kmode",
                        ""),
                (a) -> assertCommand(a, "/set mode kmode -delete",
                        ""),
                (a) -> assertCommand(a, "/set mode tmode normal -command",
                        "|  Created new feedback mode: tmode"),
                (a) -> assertCommandOutputStartsWith(a, "/set feedback -retain tmode",
                        "|  '/set feedback -retain <mode>' requires that <mode> is predefined or has been retained with '/set mode -retain'"),
                (a) -> assertCommand(a, "/set format tmode display 'YES'",
                        ""),
                (a) -> assertCommand(a, "/set feedback tmode",
                        "|  Feedback mode: tmode"),
                (a) -> assertCommand(a, "45",
                        "YES"),
                (a) -> assertCommand(a, "/set mode -retain tmode",
                        ""),
                (a) -> assertCommand(a, "/set feedback -retain tmode",
                        "|  Feedback mode: tmode"),
                (a) -> assertCommand(a, "/set format tmode display 'blurb'",
                        ""),
                (a) -> assertCommand(a, "/set format tmode display",
                        "|  /set format tmode display \"blurb\""),
                (a) -> assertCommandOutputContains(a, "/set mode tmode",
                        "|  /set format tmode display \"YES\""),
                (a) -> assertCommand(a, "45",
                        "blurb")
        );
        test(
                (a) -> assertCommand(a, "/set format tmode display",
                        "|  /set format tmode display \"YES\""),
                (a) -> assertCommandOutputContains(a, "/set mode tmode",
                        "|  /set format tmode display \"YES\""),
                (a) -> assertCommand(a, "45",
                        "YES"),
                (a) -> assertCommand(a, "/set feedback kmode",
                        "|  Feedback mode: kmode"),
                (a) -> assertCommand(a, "/set feedback",
                        "|  /set feedback -retain tmode\n" +
                        "|  /set feedback kmode\n" +
                        "|  \n" +
                        "|  Retained feedback modes:\n" +
                        "|     kmode\n" +
                        "|     tmode\n" +
                        "|  Available feedback modes:\n" +
                        "|     concise\n" +
                        "|     kmode\n" +
                        "|     normal\n" +
                        "|     silent\n" +
                        "|     tmode\n" +
                        "|     verbose"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain kmode -delete",
                        "|  The current feedback mode 'kmode' cannot be deleted"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain tmode -delete",
                        "|  The retained feedback mode 'tmode' cannot be deleted"),
                (a) -> assertCommand(a, "/set feedback -retain normal",
                        "|  Feedback mode: normal"),
                (a) -> assertCommand(a, "/set mode -retain tmode -delete",
                        ""),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain kmode -delete",
                        "")
        );
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set feedback tmode",
                        "|  Does not match any current feedback mode: tmode"),
                (a) -> assertCommandOutputStartsWith(a, "/set feedback kmode",
                        "|  Does not match any current feedback mode: kmode"),
                (a) -> assertCommandOutputStartsWith(a, "/set feedback mymode",
                        "|  Does not match any current feedback mode: mymode"),
                (a) -> assertCommandCheckOutput(a, "/set feedback",
                        (s) -> assertFalse(s.contains("mymode"), "Didn't delete mymode: " + s)),
                (a) -> assertCommandCheckOutput(a, "/set feedback",
                        (s) -> assertFalse(s.contains("kmode"), "Didn't delete kmode: " + s)),
                (a) -> assertCommandCheckOutput(a, "/set feedback",
                        (s) -> assertFalse(s.contains("tmode"), "Didn't delete tmode: " + s))
        );
    }

    public void retainModeDeleteLocalTest() {
        test(
                (a) -> assertCommand(a, "/set mode rmdlt normal -command",
                        "|  Created new feedback mode: rmdlt"),
                (a) -> assertCommand(a, "/set mode rmdlt -delete -retain ",
                        ""),
                (a) -> assertCommandCheckOutput(a, "/set feedback",
                        (s) -> {
                            assertTrue(s.contains("normal"), "Expected normal mode: " + s);
                            assertFalse(s.contains("rmdlt"), "Didn't delete rmdlt: " + s);
                        })
        );
    }

}
