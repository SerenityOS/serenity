/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143037 8142447 8144095 8140265 8144906 8146138 8147887 8147886 8148316 8148317 8143955 8157953 8080347 8154714 8166649 8167643 8170162 8172102 8165405 8174796 8174797 8175304 8167554 8180508 8166232 8196133 8199912 8211694 8223688 8254196
 * @summary Tests for Basic tests for REPL tool
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting TestingInputStream Compiler
 * @run testng/timeout=600 ToolBasicTest
 * @key intermittent
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

@Test
public class ToolBasicTest extends ReplToolTesting {

    public void elideStartUpFromList() {
        test(
                (a) -> assertCommandOutputContains(a, "123", "==> 123"),
                (a) -> assertCommandCheckOutput(a, "/list", (s) -> {
                    int cnt;
                    try (Scanner scanner = new Scanner(s)) {
                        cnt = 0;
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            if (!line.trim().isEmpty()) {
                                ++cnt;
                            }
                        }
                    }
                    assertEquals(cnt, 1, "Expected only one listed line");
                })
        );
    }

    public void elideStartUpFromSave() throws IOException {
        Compiler compiler = new Compiler();
        Path path = compiler.getPath("myfile");
        test(
                (a) -> assertCommandOutputContains(a, "123", "==> 123"),
                (a) -> assertCommand(a, "/save " + path.toString(), "")
        );
        try (Stream<String> lines = Files.lines(path)) {
            assertEquals(lines.count(), 1, "Expected only one saved line");
        }
    }

    public void testInterrupt() {
        ReplTest interrupt = (a) -> assertCommand(a, "\u0003", "");
        for (String s : new String[] { "", "\u0003" }) {
            test(false, new String[]{"--no-startup"},
                    (a) -> assertCommand(a, "int a = 2 +" + s, ""),
                    interrupt,
                    (a) -> assertCommand(a, "int a\u0003", ""),
                    (a) -> assertCommand(a, "int a = 2 + 2\u0003", ""),
                    (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                    (a) -> evaluateExpression(a, "int", "2", "2"),
                    (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                    (a) -> assertCommand(a, "void f() {", ""),
                    (a) -> assertCommand(a, "int q = 10;" + s, ""),
                    interrupt,
                    (a) -> assertCommand(a, "void f() {}\u0003", ""),
                    (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                    (a) -> assertMethod(a, "int f() { return 0; }", "()int", "f"),
                    (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                    (a) -> assertCommand(a, "class A {" + s, ""),
                    interrupt,
                    (a) -> assertCommand(a, "class A {}\u0003", ""),
                    (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                    (a) -> assertClass(a, "interface A {}", "interface", "A"),
                    (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                    (a) -> assertCommand(a, "import java.util.stream." + s, ""),
                    interrupt,
                    (a) -> assertCommand(a, "import java.util.stream.\u0003", ""),
                    (a) -> assertCommandCheckOutput(a, "/imports", assertImports()),
                    (a) -> assertImport(a, "import java.util.stream.Stream", "", "java.util.stream.Stream"),
                    (a) -> assertCommandCheckOutput(a, "/imports", assertImports())
            );
        }
    }

    public void testCtrlD() {
        test(false, new String[]{"--no-startup"},
                a -> {
                    if (!a) {
                        closeCommandInput();
                    } else {
                        throw new IllegalStateException();
                    }
                }
        );
    }

    private final Object lock = new Object();
    private PrintWriter out;
    private boolean isStopped;
    private Thread t;
    private void assertStop(boolean after, String cmd, String output) {
        if (!after) {
            isStopped = false;
            StringWriter writer = new StringWriter();
            out = new PrintWriter(writer);
            setCommandInput(cmd + "\n");
            t = new Thread(() -> {
                try {
                    // no chance to know whether cmd is being evaluated
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
                int i = 1;
                int n = 30;
                synchronized (lock) {
                    do {
                        setCommandInput("\u0003");
                        if (!isStopped) {
                            out.println("Not stopped. Try again: " + i);
                            try {
                                lock.wait(1000);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    } while (i++ < n && !isStopped);
                    if (!isStopped) {
                        System.err.println(writer.toString());
                        fail("Evaluation was not stopped: '" + cmd + "'");
                    }
                }
            });
            t.start();
        } else {
            synchronized (lock)  {
                out.println("Evaluation was stopped successfully: '" + cmd + "'");
                isStopped = true;
                lock.notify();
            }
            try {
                t.join();
                t = null;
            } catch (InterruptedException ignored) {
            }
            assertOutput(getCommandOutput(), "", "command");
            assertOutput(getCommandErrorOutput(), "", "command error");
            assertOutput(getUserOutput().trim(), output, "user");
            assertOutput(getUserErrorOutput(), "", "user error");
        }
    }

    public void testStop() {
        test(
                (a) -> assertStop(a, "while (true) {}", ""),
                (a) -> assertStop(a, "while (true) { try { Thread.sleep(100); } catch (InterruptedException ex) { } }", "")
        );
    }

    public void testRerun() {
        test(false, new String[] {"--no-startup"},
                (a) -> assertCommand(a, "/0", "|  No snippet with ID: 0"),
                (a) -> assertCommand(a, "/5", "|  No snippet with ID: 5")
        );
        String[] codes = new String[] {
                "int a = 0;", // var
                "class A {}", // class
                "void f() {}", // method
                "bool b;", // active failed
                "void g() { h(); }", // active corralled
        };
        List<ReplTest> tests = new ArrayList<>();
        for (String s : codes) {
            tests.add((a) -> assertCommand(a, s, null));
        }
        // Test /1 through /5 -- assure references are correct
        for (int i = 0; i < codes.length; ++i) {
            final int finalI = i;
            Consumer<String> check = (s) -> {
                String[] ss = s.split("\n");
                assertEquals(ss[0], codes[finalI]);
                assertTrue(ss.length > 1, s);
            };
            tests.add((a) -> assertCommandCheckOutput(a, "/" + (finalI + 1), check));
        }
        // Test /-1 ... note that the snippets added by history must be stepped over
        for (int i = 0; i < codes.length; ++i) {
            final int finalI = i;
            Consumer<String> check = (s) -> {
                String[] ss = s.split("\n");
                assertEquals(ss[0], codes[codes.length - finalI - 1]);
                assertTrue(ss.length > 1, s);
            };
            tests.add((a) -> assertCommandCheckOutput(a, "/-" + (2 * finalI + 1), check));
        }
        tests.add((a) -> assertCommandCheckOutput(a, "/!", assertStartsWith("int a = 0;")));
        test(false, new String[]{"--no-startup"},
                tests.toArray(new ReplTest[tests.size()]));
    }

    public void test8142447() {
        Function<String, BiFunction<String, Integer, ReplTest>> assertRerun = cmd -> (code, assertionCount) ->
                (a) -> assertCommandCheckOutput(a, cmd, s -> {
                            String[] ss = s.split("\n");
                            assertEquals(ss[0], code);
                            loadVariable(a, "int", "assertionCount", Integer.toString(assertionCount), Integer.toString(assertionCount));
                        });
        ReplTest assertVariables = (a) -> assertCommandCheckOutput(a, "/v", assertVariables());

        Compiler compiler = new Compiler();
        Path startup = compiler.getPath("StartupFileOption/startup.txt");
        compiler.writeToFile(startup, "int assertionCount = 0;\n" + // id: s1
                "void add(int n) { assertionCount += n; }");
        test(new String[]{"--startup", startup.toString()},
                (a) -> assertCommand(a, "add(1)", ""), // id: 1
                (a) -> assertCommandCheckOutput(a, "add(ONE)", s -> assertEquals(s.split("\n")[0], "|  Error:")), // id: e1
                (a) -> assertVariable(a, "int", "ONE", "1", "1"),
                assertRerun.apply("/1").apply("add(1)", 2), assertVariables,
                assertRerun.apply("/e1").apply("add(ONE)", 3), assertVariables,
                assertRerun.apply("/s1").apply("int assertionCount = 0;", 0), assertVariables
        );

        test(false, new String[] {"--no-startup"},
                (a) -> assertCommand(a, "/s1", "|  No snippet with ID: s1"),
                (a) -> assertCommand(a, "/1", "|  No snippet with ID: 1"),
                (a) -> assertCommand(a, "/e1", "|  No snippet with ID: e1")
        );
    }

    public void testClasspathDirectory() {
        Compiler compiler = new Compiler();
        Path outDir = Paths.get("testClasspathDirectory");
        compiler.compile(outDir, "package pkg; public class A { public String toString() { return \"A\"; } }");
        Path classpath = compiler.getPath(outDir);
        test(
                (a) -> assertCommand(a, "/env --class-path " + classpath,
                        "|  Setting new options and restoring state."),
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "A")
        );
        test(new String[] { "--class-path", classpath.toString() },
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "A")
        );
    }

    public void testEnvInStartUp() {
        Compiler compiler = new Compiler();
        Path outDir = Paths.get("testClasspathDirectory");
        compiler.compile(outDir, "package pkg; public class A { public String toString() { return \"A\"; } }");
        Path classpath = compiler.getPath(outDir);
        Path sup = compiler.getPath("startup.jsh");
        compiler.writeToFile(sup,
                "int xxx;\n" +
                "/env -class-path " + classpath + "\n" +
                "int aaa = 735;\n"
        );
        test(
                (a) -> assertCommand(a, "/set start -retain " + sup, ""),
                (a) -> assertCommand(a, "/reset",
                        "|  Resetting state."),
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "A"),
                (a) -> assertCommand(a, "aaa", "aaa ==> 735")
        );
        test(
                (a) -> assertCommandOutputContains(a, "/env", "--class-path"),
                (a) -> assertCommandOutputContains(a, "xxx", "cannot find symbol", "variable xxx"),
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "A"),
                (a) -> assertCommand(a, "aaa", "aaa ==> 735")
        );
    }

    private String makeSimpleJar() {
        Compiler compiler = new Compiler();
        Path outDir = Paths.get("testClasspathJar");
        compiler.compile(outDir, "package pkg; public class A { public String toString() { return \"A\"; } }");
        String jarName = "test.jar";
        compiler.jar(outDir, jarName, "pkg/A.class");
        return compiler.getPath(outDir).resolve(jarName).toString();
    }

    public void testClasspathJar() {
        String jarPath = makeSimpleJar();
        test(
                (a) -> assertCommand(a, "/env --class-path " + jarPath,
                        "|  Setting new options and restoring state."),
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "A")
        );
        test(new String[] { "--class-path", jarPath },
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "A")
        );
    }

    public void testClasspathUserHomeExpansion() {
        String jarPath = makeSimpleJar();
        String tilde = "~" + File.separator;
        test(
                (a) -> assertCommand(a, "/env --class-path " + tilde + "forblato",
                        "|  File '" + Paths.get(System.getProperty("user.home"), "forblato").toString()
                                + "' for '--class-path' is not found."),
                (a) -> assertCommand(a, "/env --class-path " + jarPath + File.pathSeparator
                                                            + tilde + "forblato",
                        "|  File '" + Paths.get(System.getProperty("user.home"), "forblato").toString()
                                + "' for '--class-path' is not found.")
        );
    }

    public void testBadClasspath() {
        String jarPath = makeSimpleJar();
        Compiler compiler = new Compiler();
        Path t1 = compiler.getPath("whatever/thing.zip");
        compiler.writeToFile(t1, "");
        Path t2 = compiler.getPath("whatever/thing.jmod");
        compiler.writeToFile(t2, "");
        test(
                (a) -> assertCommand(a, "/env --class-path " + t1.toString(),
                        "|  Invalid '--class-path' argument: " + t1.toString()),
                (a) -> assertCommand(a, "/env --class-path " + jarPath + File.pathSeparator + t1.toString(),
                        "|  Invalid '--class-path' argument: " + t1.toString()),
                (a) -> assertCommand(a, "/env --class-path " + t2.toString(),
                        "|  Invalid '--class-path' argument: " + t2.toString())
        );
    }

    private String makeBadSourceJar() {
        Compiler compiler = new Compiler();
        Path outDir = Paths.get("testClasspathJar");
        Path src = compiler.getPath(outDir.resolve("pkg/A.java"));
        compiler.writeToFile(src, "package pkg; /** \u0086 */public class A { public String toString() { return \"A\"; } }");
        String jarName = "test.jar";
        compiler.jar(outDir, jarName, "pkg/A.java");
        return compiler.getPath(outDir).resolve(jarName).toString();
    }

    public void testBadSourceJarClasspath() {
        String jarPath = makeBadSourceJar();
        test(
                (a) -> assertCommand(a, "/env --class-path " + jarPath,
                        "|  Setting new options and restoring state."),
                (a) -> assertCommandOutputStartsWith(a, "new pkg.A();",
                        "|  Error:\n"
                        + "|  cannot find symbol\n"
                        + "|    symbol:   class A")
        );
        test(new String[]{"--class-path", jarPath},
                (a) -> assertCommandOutputStartsWith(a, "new pkg.A();",
                        "|  Error:\n"
                        + "|  cannot find symbol\n"
                        + "|    symbol:   class A")
        );
    }

    public void testModulePath() {
        Compiler compiler = new Compiler();
        Path modsDir = Paths.get("mods");
        Path outDir = Paths.get("mods", "org.astro");
        compiler.compile(outDir, "package org.astro; public class World { public static String name() { return \"world\"; } }");
        compiler.compile(outDir, "module org.astro { exports org.astro; }");
        Path modsPath = compiler.getPath(modsDir);
        test(new String[] { "--module-path", modsPath.toString(), "--add-modules", "org.astro" },
                (a) -> assertCommand(a, "import org.astro.World;", ""),
                (a) -> evaluateExpression(a, "String",
                        "String.format(\"Greetings %s!\", World.name());",
                        "\"Greetings world!\"")
        );
    }

    public void testModulePathUserHomeExpansion() {
        String tilde = "~" + File.separatorChar;
        test(
                (a) -> assertCommand(a, "/env --module-path " + tilde + "snardugol",
                        "|  File '" + Paths.get(System.getProperty("user.home"), "snardugol").toString()
                                + "' for '--module-path' is not found.")
        );
    }

    public void testBadModulePath() {
        Compiler compiler = new Compiler();
        Path t1 = compiler.getPath("whatever/thing.zip");
        compiler.writeToFile(t1, "");
        test(
                (a) -> assertCommand(a, "/env --module-path " + t1.toString(),
                        "|  Invalid '--module-path' argument: " + t1.toString())
        );
    }

    public void testStartupFileOption() {
        Compiler compiler = new Compiler();
        Path startup = compiler.getPath("StartupFileOption/startup.txt");
        compiler.writeToFile(startup, "class A { public String toString() { return \"A\"; } }");
        test(new String[]{"--startup", startup.toString()},
                (a) -> evaluateExpression(a, "A", "new A()", "A")
        );
        test(new String[]{"--no-startup"},
                (a) -> assertCommandCheckOutput(a, "Pattern.compile(\"x+\")", assertStartsWith("|  Error:\n|  cannot find symbol"))
        );
        test(
                (a) -> assertCommand(a, "Pattern.compile(\"x+\")", "$1 ==> x+", "", null, "", "")
        );
    }

    public void testLoadingFromArgs() {
        Compiler compiler = new Compiler();
        Path path = compiler.getPath("loading.repl");
        compiler.writeToFile(path, "int a = 10; double x = 20; double a = 10;");
        test(new String[] { path.toString() },
                (a) -> assertCommand(a, "x", "x ==> 20.0"),
                (a) -> assertCommand(a, "a", "a ==> 10.0")
        );
    }

    public void testReset() {
        test(
                (a) -> assertReset(a, "/res"),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertVariable(a, "int", "x"),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertMethod(a, "void f() { }", "()void", "f"),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertClass(a, "class A { }", "class", "A"),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertImport(a, "import java.util.stream.*;", "", "java.util.stream.*"),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports()),
                (a) -> assertReset(a, "/reset"),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
    }

    public void testOpen() {
        Compiler compiler = new Compiler();
        Path path = compiler.getPath("testOpen.repl");
        compiler.writeToFile(path,
                "int a = 10;\ndouble x = 20;\ndouble a = 10;\n" +
                        "class A { public String toString() { return \"A\"; } }\nimport java.util.stream.*;");
        for (String s : new String[]{"/o", "/open"}) {
            test(
                    (a) -> assertCommand(a, s + " " + path.toString(), ""),
                    (a) -> assertCommand(a, "a", "a ==> 10.0"),
                    (a) -> evaluateExpression(a, "A", "new A();", "A"),
                    (a) -> evaluateExpression(a, "long", "Stream.of(\"A\").count();", "1"),
                    (a) -> {
                        loadVariable(a, "double", "x", "20.0", "20.0");
                        loadVariable(a, "double", "a", "10.0", "10.0");
                        loadVariable(a, "A", "$7", "new A();", "A");
                        loadVariable(a, "long", "$8", "Stream.of(\"A\").count();", "1");
                        loadClass(a, "class A { public String toString() { return \"A\"; } }",
                                "class", "A");
                        loadImport(a, "import java.util.stream.*;", "", "java.util.stream.*");
                        assertCommandCheckOutput(a, "/types", assertClasses());
                    },
                    (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                    (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                    (a) -> assertCommandCheckOutput(a, "/imports", assertImports())
            );
            Path unknown = compiler.getPath("UNKNOWN.repl");
            test(
                    (a) -> assertCommand(a, s + " " + unknown,
                            "|  File '" + unknown + "' for '/open' is not found.")
            );
        }
    }

    public void testOpenLocalFileUrl() {
        Compiler compiler = new Compiler();
        Path path = compiler.getPath("testOpen.repl");
        compiler.writeToFile(path, "int a = 10;int b = 20;int c = a + b;\n");
        for (String s : new String[]{"/o", "/open"}) {
            test(
                    (a) -> assertCommand(a, s + " " + path.toUri(), ""),
                    (a) -> assertCommand(a, "a", "a ==> 10"),
                    (a) -> assertCommand(a, "b", "b ==> 20"),
                    (a) -> assertCommand(a, "c", "c ==> 30")
            );
        }
    }

    public void testOpenFileOverHttp() throws IOException {
        var script = "int a = 10;int b = 20;int c = a + b;";

        var localhostAddress = new InetSocketAddress(InetAddress.getLoopbackAddress().getHostAddress(), 0);
        var httpServer = HttpServer.create(localhostAddress, 0);
        try {
            httpServer.createContext("/script", exchange -> {
                exchange.sendResponseHeaders(200, script.length());
                try (var output = exchange.getResponseBody()) {
                    output.write(script.getBytes());
                }
            });
            httpServer.setExecutor(null);
            httpServer.start();

            var urlAddress = "http:/" + httpServer.getAddress().toString() + "/script";
            for (String s : new String[]{"/o", "/open"}) {
                test(
                        (a) -> assertCommand(a, s + " " + urlAddress, ""),
                        (a) -> assertCommand(a, "a", "a ==> 10"),
                        (a) -> assertCommand(a, "b", "b ==> 20"),
                        (a) -> assertCommand(a, "c", "c ==> 30")
                );
            }
        } finally {
            httpServer.stop(0);
        }
    }

    public void testOpenResource() {
        test(
                (a) -> assertCommand(a, "/open PRINTING", ""),
                (a) -> assertCommandOutputContains(a, "/list",
                        "void println", "System.out.printf"),
                (a) -> assertCommand(a, "printf(\"%4.2f\", Math.PI)",
                        "", "", null, "3.14", "")
        );
    }

    public void testSave() throws IOException {
        Compiler compiler = new Compiler();
        Path path = compiler.getPath("testSave.repl");
        {
            List<String> list = Arrays.asList(
                    "int a;",
                    "class A { public String toString() { return \"A\"; } }"
            );
            test(
                    (a) -> assertVariable(a, "int", "a"),
                    (a) -> assertCommand(a, "()", null, null, null, "", ""),
                    (a) -> assertClass(a, "class A { public String toString() { return \"A\"; } }", "class", "A"),
                    (a) -> assertCommand(a, "/save " + path.toString(), "")
            );
            assertEquals(Files.readAllLines(path), list);
        }
        {
            List<String> output = new ArrayList<>();
            test(
                    (a) -> assertCommand(a, "int a;", null),
                    (a) -> assertCommand(a, "()", null, null, null, "", ""),
                    (a) -> assertClass(a, "class A { public String toString() { return \"A\"; } }", "class", "A"),
                    (a) -> assertCommandCheckOutput(a, "/list -all", (out) ->
                                    output.addAll(Stream.of(out.split("\n"))
                            .filter(str -> !str.isEmpty())
                            .map(str -> str.substring(str.indexOf(':') + 2))
                            .filter(str -> !str.startsWith("/"))
                            .collect(Collectors.toList()))),
                    (a) -> assertCommand(a, "/save -all " + path.toString(), "")
            );
            assertEquals(Files.readAllLines(path), output);
        }
        {
            List<String> output = new ArrayList<>();
            test(
                    (a) -> assertCommand(a, "int a;", null),
                    (a) -> assertCommand(a, "int b;", null),
                    (a) -> assertCommand(a, "int c;", null),
                    (a) -> assertClass(a, "class A { public String toString() { return \"A\"; } }", "class", "A"),
                    (a) -> assertCommandCheckOutput(a, "/list b c a A", (out) ->
                                    output.addAll(Stream.of(out.split("\n"))
                            .filter(str -> !str.isEmpty())
                            .map(str -> str.substring(str.indexOf(':') + 2))
                            .filter(str -> !str.startsWith("/"))
                            .collect(Collectors.toList()))),
                    (a) -> assertCommand(a, "/save 2-3 1 4 " + path.toString(), "")
            );
            assertEquals(Files.readAllLines(path), output);
        }
        {
            List<String> output = new ArrayList<>();
            test(
                    (a) -> assertVariable(a, "int", "a"),
                    (a) -> assertCommand(a, "()", null, null, null, "", ""),
                    (a) -> assertClass(a, "class A { public String toString() { return \"A\"; } }", "class", "A"),
                    (a) -> assertCommandCheckOutput(a, "/history", (out) ->
                                output.addAll(Stream.of(out.split("\n"))
                            .filter(str -> !str.isEmpty())
                            .collect(Collectors.toList()))),
                    (a) -> assertCommand(a, "/save -history " + path.toString(), "")
            );
            output.add("/save -history " + path.toString());
            assertEquals(Files.readAllLines(path), output);
        }
    }

    public void testStartRetain() {
        Compiler compiler = new Compiler();
        Path startUpFile = compiler.getPath("startUp.txt");
        test(
                (a) -> assertVariable(a, "int", "a"),
                (a) -> assertVariable(a, "double", "b", "10", "10.0"),
                (a) -> assertMethod(a, "void f() {}", "()V", "f"),
                (a) -> assertImport(a, "import java.util.stream.*;", "", "java.util.stream.*"),
                (a) -> assertCommand(a, "/save " + startUpFile.toString(), null),
                (a) -> assertCommand(a, "/set start -retain " + startUpFile.toString(), null)
        );
        Path unknown = compiler.getPath("UNKNOWN");
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set start -retain " + unknown.toString(),
                        "|  File '" + unknown + "' for '/set start' is not found.")
        );
        test(false, new String[0],
                (a) -> {
                    loadVariable(a, "int", "a");
                    loadVariable(a, "double", "b", "10.0", "10.0");
                    loadMethod(a, "void f() {}", "()void", "f");
                    loadImport(a, "import java.util.stream.*;", "", "java.util.stream.*");
                    assertCommandCheckOutput(a, "/types", assertClasses());
                },
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
    }

    public void testStartSave() throws IOException {
        Compiler compiler = new Compiler();
        Path startSave = compiler.getPath("startSave.txt");
        test(a -> assertCommand(a, "/save -start " + startSave.toString(), null));
        List<String> lines = Files.lines(startSave)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        assertEquals(lines, START_UP);
    }

    public void testConstrainedUpdates() {
        test(
                a -> assertClass(a, "class XYZZY { }", "class", "XYZZY"),
                a -> assertVariable(a, "XYZZY", "xyzzy"),
                a -> assertCommandCheckOutput(a, "import java.util.stream.*",
                        (out) -> assertTrue(out.trim().isEmpty(), "Expected no output, got: " + out))
        );
    }

    public void testRemoteExit() {
        test(
                a -> assertVariable(a, "int", "x"),
                a -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                a -> assertCommandOutputContains(a, "System.exit(5);", "terminated"),
                a -> assertCommandCheckOutput(a, "/vars", s ->
                        assertTrue(s.trim().isEmpty(), s)),
                a -> assertMethod(a, "void f() { }", "()void", "f"),
                a -> assertCommandCheckOutput(a, "/methods", assertMethods())
        );
    }

    public void testFeedbackNegative() {
        test(a -> assertCommandCheckOutput(a, "/set feedback aaaa",
                assertStartsWith("|  Does not match any current feedback mode")));
    }

    public void testFeedbackSilent() {
        for (String off : new String[]{"s", "silent"}) {
            test(
                    a -> assertCommand(a, "/set feedback " + off, ""),
                    a -> assertCommand(a, "int a", ""),
                    a -> assertCommand(a, "void f() {}", ""),
                    a -> assertCommandCheckOutput(a, "aaaa", assertStartsWith("|  Error:"))
            );
        }
    }

    public void testFeedbackNormal() {
        Compiler compiler = new Compiler();
        Path testNormalFile = compiler.getPath("testConciseNormal");
        String[] sources = new String[] {"int a", "void f() {}", "class A {}", "a = 10"};
        String[] sources2 = new String[] {"int a //again", "void f() {int y = 4;}", "class A {} //again", "a = 10"};
        String[] output = new String[] {
                "a ==> 0",
                "|  created method f()",
                "|  created class A",
                "a ==> 10"
        };
        compiler.writeToFile(testNormalFile, sources2);
        for (String feedback : new String[]{"/set fe", "/set feedback"}) {
            for (String feedbackState : new String[]{"n", "normal"}) {
                test(
                        a -> assertCommand(a, feedback + " " + feedbackState, "|  Feedback mode: normal"),
                        a -> assertCommand(a, sources[0], output[0]),
                        a -> assertCommand(a, sources[1], output[1]),
                        a -> assertCommand(a, sources[2], output[2]),
                        a -> assertCommand(a, sources[3], output[3]),
                        a -> assertCommand(a, "/o " + testNormalFile.toString(), "")
                );
            }
        }
    }

    public void testVarsWithNotActive() {
        test(
                a -> assertVariable(a, "Blath", "x"),
                a -> assertCommandOutputContains(a, "/var -all", "(not-active)")
        );
    }

    public void testHistoryReference() {
        test(false, new String[]{"--no-startup"},
                a -> assertCommand(a, "System.err.println(99)", "", "", null, "", "99\n"),
                a -> assertCommand(a, "/exit", "")
        );
        test(false, new String[]{"--no-startup"},
                a -> assertCommand(a, "System.err.println(1)", "", "", null, "", "1\n"),
                a -> assertCommand(a, "System.err.println(2)", "", "", null, "", "2\n"),
                a -> assertCommand(a, "/-2", "System.err.println(1)", "", null, "", "1\n"),
                a -> assertCommand(a, "/history",
                                                    "/debug 0\n" +
                                                    "System.err.println(1)\n" +
                                                    "System.err.println(2)\n" +
                                                    "System.err.println(1)\n" +
                                                    "/history\n"),
                a -> assertCommand(a, "/history -all",
                                                    "/debug 0\n" +
                                                    "System.err.println(99)\n" +
                                                    "/exit\n" +
                                                    "/debug 0\n" +
                                                    "System.err.println(1)\n" +
                                                    "System.err.println(2)\n" +
                                                    "System.err.println(1)\n" +
                                                    "/history\n" +
                                                    "/history -all\n"),
                a -> assertCommand(a, "/-2", "System.err.println(2)", "", null, "", "2\n"),
                a -> assertCommand(a, "/!", "System.err.println(2)", "", null, "", "2\n"),
                a -> assertCommand(a, "/2", "System.err.println(2)", "", null, "", "2\n"),
                a -> assertCommand(a, "/1", "System.err.println(1)", "", null, "", "1\n")
        );
    }

    public void testRerunIdRange() {
        Compiler compiler = new Compiler();
        Path startup = compiler.getPath("rangeStartup");
        String[] startupSources = new String[] {
            "boolean go = false",
            "void println(String s) { if (go) System.out.println(s); }",
            "void println(int i) { if (go) System.out.println(i); }",
            "println(\"s4\")",
            "println(\"s5\")",
            "println(\"s6\")"
        };
        String[] sources = new String[] {
            "frog",
            "go = true",
            "println(2)",
            "println(3)",
            "println(4)",
            "querty"
        };
        compiler.writeToFile(startup, startupSources);
        test(false, new String[]{"--startup", startup.toString()},
                a -> assertCommandOutputStartsWith(a, sources[0], "|  Error:"),
                a -> assertCommand(a, sources[1], "go ==> true", "", null, "", ""),
                a -> assertCommand(a, sources[2], "", "", null, "2\n", ""),
                a -> assertCommand(a, sources[3], "", "", null, "3\n", ""),
                a -> assertCommand(a, sources[4], "", "", null, "4\n", ""),
                a -> assertCommandOutputStartsWith(a, sources[5], "|  Error:"),
                a -> assertCommand(a, "/3", "println(3)", "", null, "3\n", ""),
                a -> assertCommand(a, "/s4", "println(\"s4\")", "", null, "s4\n", ""),
                a -> assertCommandOutputStartsWith(a, "/e1", "frog\n|  Error:"),
                a -> assertCommand(a, "/2-4",
                        "println(2)\nprintln(3)\nprintln(4)",
                        "", null, "2\n3\n4\n", ""),
                a -> assertCommand(a, "/s4-s6",
                        startupSources[3] + "\n" +startupSources[4] + "\n" +startupSources[5],
                        "", null, "s4\ns5\ns6\n", ""),
                a -> assertCommand(a, "/s4-4", null,
                        "", null, "s4\ns5\ns6\n2\n3\n4\n", ""),
                a -> assertCommandCheckOutput(a, "/e1-e2",
                        s -> {
                            assertTrue(s.trim().startsWith("frog\n|  Error:"),
                                    "Output: \'" + s + "' does not start with: " + "|  Error:");
                            assertTrue(s.trim().lastIndexOf("|  Error:") > 10,
                                    "Output: \'" + s + "' does not have second: " + "|  Error:");
                        }),
                a -> assertCommand(a, "/4  s4 2",
                        "println(4)\nprintln(\"s4\")\nprintln(2)",
                        "", null, "4\ns4\n2\n", ""),
                a -> assertCommand(a, "/s5 2-4 3",
                        "println(\"s5\")\nprintln(2)\nprintln(3)\nprintln(4)\nprintln(3)",
                        "", null, "s5\n2\n3\n4\n3\n", ""),
                a -> assertCommand(a, "/2 ff", "|  No such snippet: ff"),
                a -> assertCommand(a, "/4-2", "|  End of snippet range less than start: 4 - 2"),
                a -> assertCommand(a, "/s5-s3", "|  End of snippet range less than start: s5 - s3"),
                a -> assertCommand(a, "/4-s5", "|  End of snippet range less than start: 4 - s5")
        );
    }

    @Test(enabled = false) // TODO 8158197
    public void testHeadlessEditPad() {
        String prevHeadless = System.getProperty("java.awt.headless");
        try {
            System.setProperty("java.awt.headless", "true");
            test(
                (a) -> assertCommandOutputStartsWith(a, "/edit printf", "|  Cannot launch editor -- unexpected exception:")
            );
        } finally {
            System.setProperty("java.awt.headless", prevHeadless==null? "false" : prevHeadless);
        }
    }

    public void testAddExports() {
        test(false, new String[]{"--no-startup"},
                a -> assertCommandOutputStartsWith(a, "import jdk.internal.misc.VM;", "|  Error:")
        );
        test(false, new String[]{"--no-startup",
                        "-R--add-exports", "-Rjava.base/jdk.internal.misc=ALL-UNNAMED",
                        "-C--add-exports", "-Cjava.base/jdk.internal.misc=ALL-UNNAMED"},
                a -> assertImport(a, "import jdk.internal.misc.VM;", "", "jdk.internal.misc.VM"),
                a -> assertCommand(a, "System.err.println(VM.isBooted())", "", "", null, "", "true\n")
        );
        test(false, new String[]{"--no-startup", "--add-exports", "java.base/jdk.internal.misc"},
                a -> assertImport(a, "import jdk.internal.misc.VM;", "", "jdk.internal.misc.VM"),
                a -> assertCommand(a, "System.err.println(VM.isBooted())", "", "", null, "", "true\n")
        );
    }

    public void testRedeclareVariableNoInit() {
        test(
                a -> assertCommand(a, "Integer a;", "a ==> null"),
                a -> assertCommand(a, "a instanceof Integer;", "$2 ==> false"),
                a -> assertCommand(a, "a = 1;", "a ==> 1"),
                a -> assertCommand(a, "Integer a;", "a ==> null"),
                a -> assertCommand(a, "a instanceof Integer;", "$5 ==> false"),
                a -> assertCommand(a, "a", "a ==> null")
        );
     }

    public void testWarningUnchecked() { //8223688
        test(false, new String[]{"--no-startup"},
                a -> assertCommand(a, "abstract class A<T> { A(T t){} }", "|  created class A"),
                a -> assertCommandCheckOutput(a, "new A(\"\") {}", s -> {
                            assertStartsWith("|  Warning:");
                            assertTrue(s.contains("unchecked call"));
                            assertFalse(s.contains("Exception"));
                        })
        );
    }

    public void testIndent() { //8223688
        prefsMap.remove("INDENT");
        test(false, new String[]{"--no-startup"},
                a -> assertCommand(a, "/set indent", "|  /set indent 4"),
                a -> assertCommand(a, "/set indent 2", "|  Indent level set to: 2"),
                a -> assertCommand(a, "/set indent", "|  /set indent 2"),
                a -> assertCommand(a, "/set indent broken", "|  Invalid indent level: broken"),
                a -> assertCommandOutputContains(a, "/set", "|  /set indent 2")
        );
    }

    public void testSystemExitStartUp() {
        Compiler compiler = new Compiler();
        Path startup = compiler.getPath("SystemExitStartUp/startup.txt");
        compiler.writeToFile(startup, "int i1 = 0;\n" +
                                      "System.exit(0);\n" +
                                      "int i2 = 0;\n");
        test(Locale.ROOT, true, new String[]{"--startup", startup.toString()},
                "State engine terminated.",
                (a) -> assertCommand(a, "i2", "i2 ==> 0"),
                (a) -> assertCommandOutputContains(a, "i1", "Error:", "variable i1")
        );
    }
}
