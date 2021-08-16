/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8196433
 * @summary use the new error diagnostic approach at javac.Main
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @run main/othervm OptionSmokeTest
 */

import java.util.Locale;

import java.util.List;
import java.util.stream.Collectors;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.code.Source;

import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;

public class OptionSmokeTest extends TestRunner {
    ToolBox tb = new ToolBox();

    public OptionSmokeTest() {
        super(System.err);
        Locale.setDefault(Locale.US);
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws Exception {
        return tb.findJavaFiles(paths);
    }

    public static void main(String... args) throws Exception {
        new OptionSmokeTest().runTests();
    }

    @Test
    public void optionA1(Path base) throws Exception {
        doTest(base, "error: -A requires an argument; use '-Akey' or '-Akey=value'", "-A");
    }

    @Test
    public void optionA2(Path base) throws Exception {
        doTest(base,
                "error: key in annotation processor option '-A1e=2' is not a dot-separated sequence of identifiers",
                "-A1e=2");
    }

    @Test
    public void noFlag(Path base) throws Exception {
        doTest(base, "error: invalid flag: -noFlag", "-noFlag");
    }

    @Test
    public void profileAndBSP(Path base) throws Exception {
        doTest(base, "error: profile and bootclasspath options cannot be used together",
                String.format("-profile compact1 -bootclasspath . -target %s -source %s", Source.DEFAULT.name, Source.DEFAULT.name));
    }

    @Test
    public void invalidProfile(Path base) throws Exception {
        doTest(base, "error: invalid profile: noProfile", "-profile noProfile");
    }

    @Test
    public void invalidTarget(Path base) throws Exception {
        doTest(base, "error: invalid target release: 999999", "-target 999999");
    }

    @Test
    public void optionNotAvailableWithTarget(Path base) throws Exception {
        doTest(base, String.format("error: option -profile not allowed with target %s", Source.DEFAULT.name),
                String.format("-profile compact1 -target %s", Source.DEFAULT.name));
    }

    @Test
    public void optionTooMany(Path base) throws Exception {
        doTest(base, "error: option --default-module-for-created-files can only be specified once",
                "--default-module-for-created-files=m1x --default-module-for-created-files=m1x");
    }

    @Test
    public void noSrcFiles(Path base) throws Exception {
        doTestNoSource(base, "error: no source files", String.format("-target %s", Source.DEFAULT.name));
    }

    @Test
    public void requiresArg(Path base) throws Exception {
        doTestNoSource(base, "error: --target requires an argument", "-target");
    }

    @Test
    public void invalidSource(Path base) throws Exception {
        doTestNoSource(base, "error: invalid source release: 999999", "-source 999999");
    }

    @Test
    public void sourceAndModuleSourceCantBeTogether(Path base) throws Exception {
        doTest(base, "error: cannot specify both --source-path and --module-source-path",
                "--source-path . --module-source-path .");
    }

    @Test
    public void sourceAndTargetMismatch(Path base) throws Exception {
        doTest(base, String.format("warning: source release %s requires target release %s", Source.DEFAULT.name, Source.DEFAULT.name),
                String.format("-source %s -target %s", Source.DEFAULT.name, Source.MIN.name));
    }

    @Test
    public void targetConflictsWithDefaultSource(Path base) throws Exception {
        doTest(base, String.format("warning: target release %s conflicts with default source release %s", Source.MIN.name, Source.DEFAULT.name),
                String.format("-target %s", Source.MIN.name));
    }

    @Test
    public void profileNotValidForTarget(Path base) throws Exception {
        doTest(base, String.format("warning: profile compact2 is not valid for target release %s", Source.MIN.name),
                String.format("-profile compact2 -target %s -source %s", Source.MIN.name, Source.MIN.name));
    }

    @Test
    public void fileNotFound(Path base) throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .files("notExistent/T.java")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        Assert.check(log.startsWith(String.format("error: file not found: notExistent%sT.java", fileSeparator)),
                String.format("real value of log:%s", log));
    }

    static final String fileSeparator = System.getProperty("file.separator");

    @Test
    public void notADirectory(Path base) throws Exception {
        doTest(base, String.format("error: not a directory: notADirectory%ssrc%sDummy.java", fileSeparator, fileSeparator),
                String.format("-d notADirectory%ssrc%sDummy.java", fileSeparator, fileSeparator));
    }

    @Test
    public void notAFile(Path base) throws Exception {
        // looks like a java file, it is a directory
        Path dir = base.resolve("dir.java");
        tb.createDirectories(dir);
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .spaceSeparatedOptions("-XDsourcefile " + dir)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        Assert.check(log.startsWith(String.format("error: not a file: notAFile%sdir.java", fileSeparator)));
    }

    @Test
    public void badValueForOption(Path base) throws Exception {
        doTestNoSource(base, "error: bad value for --patch-module option: \'notExistent\'",
                "--patch-module notExistent");
    }

    @Test
    public void patchModuleMoreThanOnce(Path base) throws Exception {
        doTestNoSource(base, "error: --patch-module specified more than once for m",
                "--patch-module m=. --patch-module m=.");
    }

    @Test
    public void unmatchedQuoteInEnvVar(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy {}");
        List<String> log = new JavacTask(tb, Task.Mode.EXEC)
                .envVar("JDK_JAVAC_OPTIONS",
                        String.format("--add-exports jdk.compiler%scom.sun.tools.javac.jvm=\"ALL-UNNAMED", fileSeparator))
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.STDERR);
        log = log.stream().filter(s->!s.matches("^Picked up .*JAVA.*OPTIONS:.*")).collect(Collectors.toList());
        List<String> expected = List.of(
                "error: unmatched quote in environment variable JDK_JAVAC_OPTIONS",
                "Usage: javac <options> <source files>",
                "use --help for a list of possible options"
        );
        tb.checkEqual(log, expected);
    }

    @Test
    public void optionCantBeUsedWithRelease(Path base) throws Exception {
        doTestNoSource(base, "error: option --source cannot be used together with --release",
                String.format("--release %s -source %s", Source.DEFAULT.name, Source.DEFAULT.name));
    }

    @Test
    public void releaseVersionNotSupported(Path base) throws Exception {
        doTestNoSource(base, "error: release version 99999999 not supported",
                "--release 99999999");
    }

    // taken from former test: tools/javac/options/release/ReleaseOptionClashes
    @Test
    public void releaseAndBootclasspath(Path base) throws Exception {
        doTestNoSource(base, "error: option --boot-class-path cannot be used together with --release",
                String.format("--release %s -bootclasspath any", Source.DEFAULT.name));
        doTestNoSource(base, "error: option -Xbootclasspath: cannot be used together with --release",
                String.format("--release %s -Xbootclasspath:any", Source.DEFAULT.name));
        doTestNoSource(base, "error: option -Xbootclasspath/p: cannot be used together with --release",
                String.format("--release %s -Xbootclasspath/p:any", Source.DEFAULT.name));
        doTestNoSource(base, "error: option -endorseddirs cannot be used together with --release",
                String.format("--release %s -endorseddirs any", Source.DEFAULT.name));
        doTestNoSource(base, "error: option -extdirs cannot be used together with --release",
                String.format("--release %s -extdirs any", Source.DEFAULT.name));
        doTestNoSource(base, "error: option --source cannot be used together with --release",
                String.format("--release %s -source %s", Source.MIN.name, Source.DEFAULT.name));
        doTestNoSource(base, "error: option --target cannot be used together with --release",
                String.format("--release %s -target %s", Source.MIN.name, Source.DEFAULT.name));
        doTestNoSource(base, "error: option --system cannot be used together with --release",
                String.format("--release %s --system none", Source.DEFAULT.name));
        doTestNoSource(base, "error: option --upgrade-module-path cannot be used together with --release",
                String.format("--release %s --upgrade-module-path any", Source.DEFAULT.name));
    }

    void doTest(Path base, String output, String options) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .spaceSeparatedOptions(options)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        Assert.check(log.startsWith(output), String.format("expected:\n%s\nfound:\n%s", output, log));
    }

    void doTestNoSource(Path base, String output, String options) throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .spaceSeparatedOptions(options)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        Assert.check(log.startsWith(output), String.format("expected:\n%s\nfound:\n%s", output, log));
    }
}
