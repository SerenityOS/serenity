/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8192920 8204588 8210275
 * @summary Test source mode
 * @modules jdk.compiler jdk.jlink
 * @run main SourceMode
 */


import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.spi.ToolProvider;

public class SourceMode extends TestHelper {

    public static void main(String... args) throws Exception {
        new SourceMode().run(args);
    }

    // To reduce the chance of creating shebang lines that are too long,
    // use a shorter path for a java command if the standard path is too long.
    private final Path shebangJavaCmd;

    // Whether or not to automatically skip the shebang tests
    private final boolean skipShebangTest;

    private final PrintStream log;

    private static final String thisVersion = System.getProperty("java.specification.version");

    SourceMode() throws Exception {
        log = System.err;

        if (isWindows) {
            // Skip shebang tests on Windows, because that requires Cygwin.
            skipShebangTest = true;
            shebangJavaCmd = null;
        } else {
            // Try to ensure the path to the Java launcher is reasonably short,
            // to work around the mostly undocumented limit of 120 characters
            // for a shebang line.
            // The value of 120 is the typical kernel compile-time buffer limit.
            // The following limit of 80 allows room for arguments to be placed
            // after the path to the launcher on the shebang line.
            Path cmd = Paths.get(javaCmd);
            if (cmd.toString().length() < 80) {
                shebangJavaCmd = cmd;
            } else {
                // Create a small image in the current directory, such that
                // the path for the launcher is just "tmpJDK/bin/java".
                Path tmpJDK = Paths.get("tmpJDK");
                ToolProvider jlink = ToolProvider.findFirst("jlink")
                    .orElseThrow(() -> new Exception("cannot find jlink"));
                jlink.run(System.out, System.err,
                    "--add-modules", "jdk.compiler,jdk.zipfs", "--output", tmpJDK.toString());
                shebangJavaCmd = tmpJDK.resolve("bin").resolve("java");
            }
            log.println("Using java command: " + shebangJavaCmd);
            skipShebangTest = false;
        }
    }

    // java Simple.java 1 2 3
    @Test
    void testSimpleJava() throws IOException {
        starting("testSimpleJava");
        Path file = getSimpleFile("Simple.java", false);
        TestResult tr = doExec(javaCmd, file.toString(), "1", "2", "3");
        if (!tr.isOK())
            error(tr, "Bad exit code: " + tr.exitValue);
        if (!tr.contains("[1, 2, 3]"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java --source N simple 1 2 3
    @Test
    void testSimple() throws IOException {
        starting("testSimple");
        Path file = getSimpleFile("simple", false);
        TestResult tr = doExec(javaCmd, "--source", thisVersion, file.toString(), "1", "2", "3");
        if (!tr.isOK())
            error(tr, "Bad exit code: " + tr.exitValue);
        if (!tr.contains("[1, 2, 3]"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // execSimple 1 2 3
    @Test
    void testExecSimple() throws IOException {
        starting("testExecSimple");
        if (skipShebangTest) {
            log.println("SKIPPED");
            return;
        }
        Path file = setExecutable(getSimpleFile("execSimple", true));
        TestResult tr = doExec(file.toAbsolutePath().toString(), "1", "2", "3");
        if (!tr.isOK())
            error(tr, "Bad exit code: " + tr.exitValue);
        if (!tr.contains("[1, 2, 3]"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java @simpleJava.at  (contains Simple.java 1 2 3)
    @Test
    void testSimpleJavaAtFile() throws IOException {
        starting("testSimpleJavaAtFile");
        Path file = getSimpleFile("Simple.java", false);
        Path atFile = Paths.get("simpleJava.at");
        createFile(atFile, List.of(file + " 1 2 3"));
        TestResult tr = doExec(javaCmd, "@" + atFile);
        if (!tr.isOK())
            error(tr, "Bad exit code: " + tr.exitValue);
        if (!tr.contains("[1, 2, 3]"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java @simple.at  (contains --source N simple 1 2 3)
    @Test
    void testSimpleAtFile() throws IOException {
        starting("testSimpleAtFile");
        Path file = getSimpleFile("simple", false);
        Path atFile = Paths.get("simple.at");
        createFile(atFile, List.of("--source " + thisVersion + " " + file + " 1 2 3"));
        TestResult tr = doExec(javaCmd, "@" + atFile);
        if (!tr.isOK())
            error(tr, "Bad exit code: " + tr.exitValue);
        if (!tr.contains("[1, 2, 3]"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java -cp classes Main.java 1 2 3
    @Test
    void testClasspath() throws IOException {
        starting("testClasspath");
        Path base = Files.createDirectories(Paths.get("testClasspath"));
        Path otherJava = base.resolve("Other.java");
        createFile(otherJava, List.of(
            "public class Other {",
            "  public static String join(String[] args) {",
            "    return String.join(\"-\", args);",
            "  }",
            "}"
        ));
        Path classes = Files.createDirectories(base.resolve("classes"));
        Path mainJava = base.resolve("Main.java");
        createFile(mainJava, List.of(
            "class Main {",
            "  public static void main(String[] args) {",
            "    System.out.println(Other.join(args));",
            "  }}"
        ));
        compile("-d", classes.toString(), otherJava.toString());
        TestResult tr = doExec(javaCmd, "-cp", classes.toString(),
                mainJava.toString(), "1", "2", "3");
        if (!tr.isOK())
            error(tr, "Bad exit code: " + tr.exitValue);
        if (!tr.contains("1-2-3"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java --add-exports=... Export.java --help
    @Test
    void testAddExports() throws IOException {
        starting("testAddExports");
        Path exportJava = Paths.get("Export.java");
        createFile(exportJava, List.of(
            "public class Export {",
            "  public static void main(String[] args) {",
            "    new com.sun.tools.javac.main.Main(\"demo\").compile(args);",
            "  }",
            "}"
        ));
        // verify access fails without --add-exports
        TestResult tr1 = doExec(javaCmd, exportJava.toString(), "--help");
        if (tr1.isOK())
            error(tr1, "Compilation succeeded unexpectedly");
        show(tr1);
        // verify access succeeds with --add-exports
        TestResult tr2 = doExec(javaCmd,
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            exportJava.toString(), "--help");
        if (!tr2.isOK())
            error(tr2, "Bad exit code: " + tr2.exitValue);
        if (!(tr2.contains("demo") && tr2.contains("Usage")))
            error(tr2, "Expected output not found");
        show(tr2);
    }

    // java -cp ... HelloWorld.java  (for a class "java" in package "HelloWorld")
    @Test
    void testClassNamedJava() throws IOException {
        starting("testClassNamedJava");
        Path base = Files.createDirectories(Paths.get("testClassNamedJava"));
        Path src = Files.createDirectories(base.resolve("src"));
        Path srcfile = src.resolve("java.java");
        createFile(srcfile, List.of(
                "package HelloWorld;",
                "class java {",
                "    public static void main(String... args) {",
                "        System.out.println(HelloWorld.java.class.getName());",
                "    }",
                "}"
        ));
        Path classes = base.resolve("classes");
        compile("-d", classes.toString(), srcfile.toString());
        TestResult tr =
            doExec(javaCmd, "-cp", classes.toString(), "HelloWorld.java");
        if (!tr.isOK())
            error(tr, "Command failed");
        if (!tr.contains("HelloWorld.java"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java --source N -cp ... HelloWorld
    @Test
    void testSourceClasspath() throws IOException {
        starting("testSourceClasspath");
        Path base = Files.createDirectories(Paths.get("testSourceClasspath"));
        Path src = Files.createDirectories(base.resolve("src"));
        Path srcfile = src.resolve("java.java");
        createFile(srcfile, List.of(
                "class HelloWorld {",
                "    public static void main(String... args) {",
                "        System.out.println(\"Hello World\");",
                "    }",
                "}"
        ));
        Path classes = base.resolve("classes");
        compile("-d", classes.toString(), srcfile.toString());
        TestResult tr =
            doExec(javaCmd, "--source", thisVersion, "-cp", classes.toString(), "HelloWorld");
        if (tr.isOK())
            error(tr, "Command succeeded unexpectedly");
        if (!tr.contains("file not found: HelloWorld"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java --source
    @Test
    void testSourceNoArg() throws IOException {
        starting("testSourceNoArg");
        TestResult tr = doExec(javaCmd, "--source");
        if (tr.isOK())
            error(tr, "Command succeeded unexpectedly");
        if (!tr.contains("--source requires source version"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java --source N -jar simple.jar
    @Test
    void testSourceJarConflict() throws IOException {
        starting("testSourceJarConflict");
        Path base = Files.createDirectories(Paths.get("testSourceJarConflict"));
        Path file = getSimpleFile("Simple.java", false);
        Path classes = Files.createDirectories(base.resolve("classes"));
        compile("-d", classes.toString(), file.toString());
        Path simpleJar = base.resolve("simple.jar");
        createJar("cf", simpleJar.toString(), "-C", classes.toString(), ".");
        TestResult tr =
            doExec(javaCmd, "--source", thisVersion, "-jar", simpleJar.toString());
        if (tr.isOK())
            error(tr, "Command succeeded unexpectedly");
        if (!tr.contains("Option -jar is not allowed with --source"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // java --source N -m jdk.compiler
    @Test
    void testSourceModuleConflict() throws IOException {
        starting("testSourceModuleConflict");
        TestResult tr = doExec(javaCmd, "--source", thisVersion, "-m", "jdk.compiler");
        if (tr.isOK())
            error(tr, "Command succeeded unexpectedly");
        if (!tr.contains("Option -m is not allowed with --source"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // #!.../java --source N -version
    @Test
    void testTerminalOptionInShebang() throws IOException {
        starting("testTerminalOptionInShebang");
        if (skipShebangTest || isAIX || isMacOSX) {
            // On MacOSX, we cannot distinguish between terminal options on the
            // shebang line and those on the command line.
            // On Solaris, all options after the first on the shebang line are
            // ignored. Similar on AIX.
            log.println("SKIPPED");
            return;
        }
        Path base = Files.createDirectories(
            Paths.get("testTerminalOptionInShebang"));
        Path bad = base.resolve("bad");
        createFile(bad, List.of(
            "#!" + shebangJavaCmd + " --source " + thisVersion + " -version"));
        setExecutable(bad);
        TestResult tr = doExec(bad.toString());
        if (!tr.contains("Option -version is not allowed in this context"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // #!.../java --source N @bad.at  (contains -version)
    @Test
    void testTerminalOptionInShebangAtFile() throws IOException {
        starting("testTerminalOptionInShebangAtFile");
        if (skipShebangTest || isAIX || isMacOSX) {
            // On MacOSX, we cannot distinguish between terminal options in a
            // shebang @-file and those on the command line.
            // On Solaris, all options after the first on the shebang line are
            // ignored. Similar on AIX.
            log.println("SKIPPED");
            return;
        }
        // Use a short directory name, to avoid line length limitations
        Path base = Files.createDirectories(Paths.get("testBadAtFile"));
        Path bad_at = base.resolve("bad.at");
        createFile(bad_at, List.of("-version"));
        Path bad = base.resolve("bad");
        createFile(bad, List.of(
            "#!" + shebangJavaCmd + " --source " + thisVersion + " @" + bad_at));
        setExecutable(bad);
        TestResult tr = doExec(bad.toString());
        if (!tr.contains("Option -version in @testBadAtFile/bad.at is "
                + "not allowed in this context"))
            error(tr, "Expected output not found");
        show(tr);
    }

    // #!.../java --source N HelloWorld
    @Test
    void testMainClassInShebang() throws IOException {
        starting("testMainClassInShebang");
        if (skipShebangTest || isAIX || isMacOSX) {
            // On MacOSX, we cannot distinguish between a main class on the
            // shebang line and one on the command line.
            // On Solaris, all options after the first on the shebang line are
            // ignored. Similar on AIX.
            log.println("SKIPPED");
            return;
        }
        Path base = Files.createDirectories(Paths.get("testMainClassInShebang"));
        Path bad = base.resolve("bad");
        createFile(bad, List.of(
            "#!" + shebangJavaCmd + " --source " + thisVersion + " HelloWorld"));
        setExecutable(bad);
        TestResult tr = doExec(bad.toString());
        if (!tr.contains("Cannot specify main class in this context"))
            error(tr, "Expected output not found");
        show(tr);
    }

    //--------------------------------------------------------------------------

    private void starting(String label) {
        System.out.println();
        System.out.println("*** Starting: " + label + " (stdout)");

        System.err.println();
        System.err.println("*** Starting: " + label + " (stderr)");
    }

    private void show(TestResult tr) {
        log.println("*** Test Output:");
        for (String line: tr.testOutput) {
            log.println(line);
        }
        log.println("*** End Of Test Output:");
    }

    private Map<String,String> getLauncherDebugEnv() {
        return Map.of("_JAVA_LAUNCHER_DEBUG", "1");
    }

    private Path getSimpleFile(String name, boolean shebang) throws IOException {
        Path file = Paths.get(name);
        if (!Files.exists(file)) {
            createFile(file, List.of(
                (shebang ? "#!" + shebangJavaCmd + " --source=" + thisVersion: ""),
                "public class Simple {",
                "  public static void main(String[] args) {",
                "    System.out.println(java.util.Arrays.toString(args));",
                "  }}"));
        }
        return file;
    }

    private void createFile(Path file, List<String> lines) throws IOException {
        lines.stream()
            .filter(line -> line.length() > 128)
            .forEach(line -> {
                    log.println("*** Warning: long line ("
                                        + line.length()
                                        + " chars) in file " + file);
                    log.println("*** " + line);
                });
        log.println("*** File: " + file);
        lines.stream().forEach(log::println);
        log.println("*** End Of File");
        createFile(file.toFile(), lines);
    }

    private Path setExecutable(Path file) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(file, perms);
        return file;
    }

    private void error(TestResult tr, String message) {
        show(tr);
        throw new RuntimeException(message);
    }
}
