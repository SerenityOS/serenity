/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8153716 8143955 8151754 8150382 8153920 8156910 8131024 8160089 8153897 8167128 8154513 8170015 8170368 8172102 8172103  8165405 8173073 8173848 8174041 8173916 8174028 8174262 8174797 8177079 8180508 8177466 8172154 8192979 8191842 8198573 8198801 8210596 8210959 8215099 8199623 8236715 8239536 8247456 8246774 8238173
 * @summary Simple jshell tool tests
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @build KullaTesting TestingInputStream
 * @run testng ToolSimpleTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ToolSimpleTest extends ReplToolTesting {

    @Test
    public void testRemaining() {
        test(
                (a) -> assertCommand(a, "int z; z =", "z ==> 0"),
                (a) -> assertCommand(a, "5", "z ==> 5"),
                (a) -> assertCommand(a, "/*nada*/; int q =", ""),
                (a) -> assertCommand(a, "77", "q ==> 77"),
                (a) -> assertCommand(a, "//comment;", ""),
                (a) -> assertCommand(a, "int v;", "v ==> 0"),
                (a) -> assertCommand(a, "int v; int c",
                        "v ==> 0\n" +
                        "c ==> 0")
        );
    }

    @Test
    public void testOpenComment() {
        test(
                (a) -> assertCommand(a, "int z = /* blah", ""),
                (a) -> assertCommand(a, "baz */ 5", "z ==> 5"),
                (a) -> assertCommand(a, "/** hoge ", ""),
                (a) -> assertCommand(a, "baz **/", ""),
                (a) -> assertCommand(a, "int v", "v ==> 0")
        );
    }

    @Test
    public void testSwitchExpression() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertCommand(a, "enum Day {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }", "|  created enum Day"),
                (a) -> assertCommand(a, "Day day = Day.FRIDAY;", "day ==> FRIDAY"),
                (a) -> assertCommand(a, "switch (day) {", ""),
                (a) -> assertCommand(a, "case MONDAY, FRIDAY, SUNDAY -> 6;", ""),
                (a) -> assertCommand(a, "case TUESDAY -> 7;", ""),
                (a) -> assertCommand(a, "case THURSDAY, SATURDAY -> 8;", ""),
                (a) -> assertCommand(a, "case WEDNESDAY -> 9;", ""),
                (a) -> assertCommandOutputContains(a, "}", " ==> 6")
                );
    }

    @Test
    public void testSwitchExpressionCompletion() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertCommand(a, "enum Day {MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }", "|  created enum Day"),
                (a) -> assertCommand(a, "Day day = Day.FRIDAY;", "day ==> FRIDAY"),
                (a) -> assertCommand(a, "switch (day) {", ""),
                (a) -> assertCommand(a, "case MONDAY, FRIDAY, SUNDAY -> 6;", ""),
                (a) -> assertCommand(a, "case TUESDAY -> 7;", ""),
                (a) -> assertCommand(a, "case THURSDAY, SATURDAY -> 8;", ""),
                (a) -> assertCommand(a, "case WEDNESDAY -> 9;", ""),
                (a) -> assertCommand(a, "} +", ""),
                (a) -> assertCommandOutputContains(a, "1000", " ==> 1006")
                );
    }

    @Test
    public void testLessThan() {
        test(
                (a) -> assertCommand(a, "45", "$1 ==> 45"),
                (a) -> assertCommand(a, "72", "$2 ==> 72"),
                (a) -> assertCommand(a, "$1 < $2", "$3 ==> true"),
                (a) -> assertCommand(a, "int a, b", "a ==> 0\n" +
                        "b ==> 0"),
                (a) -> assertCommand(a, "a < b", "$6 ==> false")
        );
    }

    @Test
    public void testChainedThrow() {
        test(
                (a) -> assertCommand(a, "void p() throws Exception { ((String) null).toString(); }",
                        "|  created method p()"),
                (a) -> assertCommand(a, "void n() throws Exception { try { p(); } catch (Exception ex) { throw new IOException(\"bar\", ex); }}",
                        "|  created method n()"),
                (a) -> assertCommand(a, "void m() { try { n(); } catch (Exception ex) { throw new RuntimeException(\"foo\", ex); }}",
                        "|  created method m()"),
                (a) -> assertCommand(a, "m()",
                          "|  Exception java.lang.RuntimeException: foo\n"
                        + "|        at m (#3:1)\n"
                        + "|        at (#4:1)\n"
                        + "|  Caused by: java.io.IOException: bar\n"
                        + "|        at n (#2:1)\n"
                        + "|        ...\n"
                        + "|  Caused by: java.lang.NullPointerException: Cannot invoke \"String.toString()\" because \"null\" is null\n"
                        + "|        at p (#1:1)\n"
                        + "|        ..."),
                (a) -> assertCommand(a, "/drop p",
                        "|  dropped method p()"),
                (a) -> assertCommand(a, "m()",
                        "|  attempted to call method n() which cannot be invoked until method p() is declared")
        );
    }

    @Test
    public void testThrowWithPercent() {
        test(
                (a) -> assertCommandCheckOutput(a,
                        "URI u = new URI(\"http\", null, \"h\", -1, \"a\" + (char)0x04, null, null);", (s) ->
                                assertTrue(s.contains("URISyntaxException") && !s.contains("JShellTool"),
                                        "Output: '" + s + "'")),
                (a) -> assertCommandCheckOutput(a,
                        "throw new Exception(\"%z\")", (s) ->
                                assertTrue(s.contains("java.lang.Exception") && !s.contains("UnknownFormatConversionException"),
                                        "Output: '" + s + "'"))
        );
    }

    @Test
    public void oneLineOfError() {
        test(
                (a) -> assertCommand(a, "12+", null),
                (a) -> assertCommandCheckOutput(a, "  true", (s) ->
                        assertTrue(s.contains("12+") && !s.contains("true"), "Output: '" + s + "'"))
        );
    }

    @Test
    public void defineVariables() {
        test(
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertVariable(a, "int", "a"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertVariable(a, "double", "a", "1", "1.0"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> evaluateExpression(a, "double", "2 * a", "2.0"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables())
        );
    }

    @Test
    public void defineMethods() {
        test(
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertMethod(a, "int f() { return 0; }", "()int", "f"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertMethod(a, "void f(int a) { g(); }", "(int)void", "f"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                (a) -> assertMethod(a, "void g() {}", "()void", "g"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods())
        );
    }

    @Test
    public void defineTypes() {
        test(
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "class A { }", "class", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "interface A { }", "interface", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "enum A { }", "enum", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "@interface A { }", "@interface", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses())
        );
    }

    @Test
    public void defineImports() {
        test(
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports()),
                (a) -> assertImport(a, "import java.util.stream.Stream;", "", "java.util.stream.Stream"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports()),
                (a) -> assertImport(a, "import java.util.stream.*;", "", "java.util.stream.*"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports()),
                (a) -> assertImport(a, "import static java.lang.Math.PI;", "static", "java.lang.Math.PI"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports()),
                (a) -> assertImport(a, "import static java.lang.Math.*;", "static", "java.lang.Math.*"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
    }

    @Test
    public void defineVar() {
        test(
                (a) -> assertCommand(a, "int x = 72", "x ==> 72"),
                (a) -> assertCommand(a, "x", "x ==> 72"),
                (a) -> assertCommand(a, "/vars", "|    int x = 72")
        );
    }

    @Test
    public void defineUnresolvedVar() {
        test(
                (a) -> assertCommand(a, "undefined x",
                        "|  created variable x, however, it cannot be referenced until class undefined is declared"),
                (a) -> assertCommand(a, "/vars", "|    undefined x = (not-active)")
        );
    }

    @Test
    public void testUnresolved() {
        test(
                (a) -> assertCommand(a, "int f() { return g() + x + new A().a; }",
                        "|  created method f(), however, it cannot be invoked until method g(), variable x, and class A are declared"),
                (a) -> assertCommand(a, "f()",
                        "|  attempted to call method f() which cannot be invoked until method g(), variable x, and class A are declared"),
                (a) -> assertCommandOutputStartsWith(a, "int g() { return x; }",
                        "|  created method g(), however, it cannot be invoked until variable x is declared"),
                (a) -> assertCommand(a, "g()", "|  attempted to call method g() which cannot be invoked until variable x is declared")
        );
    }

    @Test
    public void testAbstractMethod() {
        test(
                (a) -> assertCommand(a, "abstract int f(int x);",
                        "|  created method f(int), however, it cannot be invoked until method f(int) is declared"),
                (a) -> assertCommand(a, "f(13)",
                        "|  attempted to call method f(int) which cannot be invoked until method f(int) is declared"),
                (a) -> assertCommand(a, " abstract void m(Blah b);",
                        "|  created method m(Blah), however, it cannot be referenced until class Blah, and method m(Blah) are declared")
        );
    }

    // 8199623
    @Test
    public void testTwoForkedDrop() {
        test(
                (a) -> assertCommand(a, "void p() throws Exception { ((String) null).toString(); }",
                        "|  created method p()"),
                (a) -> assertCommand(a, "void n() throws Exception { try { p(); } catch (Exception ex) { throw new IOException(\"bar\", ex); }} ",
                        "|  created method n()"),
                (a) -> assertCommand(a, "void m() { try { n(); } catch (Exception ex) { throw new RuntimeException(\"foo\", ex); }}",
                        "|  created method m()"),
                (a) -> assertCommand(a, "void c() throws Throwable { p(); }",
                        "|  created method c()"),
                (a) -> assertCommand(a, "/drop p",
                        "|  dropped method p()"),
                (a) -> assertCommand(a, "m()",
                        "|  attempted to call method n() which cannot be invoked until method p() is declared"),
                (a) -> assertCommand(a, "/meth n",
                        "|    void n()\n" +
                        "|       which cannot be invoked until method p() is declared"),
                (a) -> assertCommand(a, "/meth m",
                        "|    void m()"),
                (a) -> assertCommand(a, "/meth c",
                        "|    void c()\n" +
                                "|       which cannot be invoked until method p() is declared")
        );
    }

    @Test
    public void testUnknownCommand() {
        test((a) -> assertCommand(a, "/unknown",
                "|  Invalid command: /unknown\n" +
                "|  Type /help for help."));
    }

    @Test
    public void testEmptyClassPath() {
        test(after -> assertCommand(after, "/env --class-path", "|  Argument to class-path missing."));
    }

    @Test
    public void testInvalidClassPath() {
        test(
                a -> assertCommand(a, "/env --class-path snurgefusal",
                        "|  File 'snurgefusal' for '--class-path' is not found."),
                a -> assertCommand(a, "/env --class-path ?",
                        "|  File '?' for '--class-path' is not found.")
        );
    }

    @Test
    public void testNoArgument() {
        test(
                (a) -> assertCommand(a, "/save",
                        "|  '/save' requires a filename argument."),
                (a) -> assertCommand(a, "/open",
                        "|  '/open' requires a filename argument."),
                (a) -> assertCommandOutputStartsWith(a, "/drop",
                        "|  In the /drop argument, please specify an import, variable, method, or class to drop.")
        );
    }

    @Test
    public void testDebug() {
        test(
                (a) -> assertCommand(a, "/deb", "|  Debugging on"),
                (a) -> assertCommand(a, "/debug", "|  Debugging off"),
                (a) -> assertCommand(a, "/debug", "|  Debugging on"),
                (a) -> assertCommand(a, "/deb", "|  Debugging off")
        );
    }

    @Test
    public void testDrop() {
        test(false, new String[]{"--no-startup"},
                a -> assertVariable(a, "int", "a"),
                a -> dropVariable(a, "/drop 1", "int a = 0", "|  dropped variable a"),
                a -> assertMethod(a, "int b() { return 0; }", "()int", "b"),
                a -> dropMethod(a, "/drop 2", "int b()", "|  dropped method b()"),
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> dropClass(a, "/drop 3", "class A", "|  dropped class A"),
                a -> assertImport(a, "import java.util.stream.*;", "", "java.util.stream.*"),
                a -> dropImport(a, "/drop 4", "import java.util.stream.*", ""),
                a -> assertCommand(a, "for (int i = 0; i < 10; ++i) {}", ""),
                a -> assertCommand(a, "/drop 5", ""),
                a -> assertCommand(a, "/list", ""),
                a -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                a -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                a -> assertCommandCheckOutput(a, "/types", assertClasses()),
                a -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
        test(false, new String[]{"--no-startup"},
                a -> assertVariable(a, "int", "a"),
                a -> dropVariable(a, "/drop a", "int a = 0", "|  dropped variable a"),
                a -> assertMethod(a, "int b() { return 0; }", "()int", "b"),
                a -> dropMethod(a, "/drop b", "int b()", "|  dropped method b()"),
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> dropClass(a, "/drop A", "class A", "|  dropped class A"),
                a -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                a -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                a -> assertCommandCheckOutput(a, "/types", assertClasses()),
                a -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
    }

    @Test
    public void testDropRange() {
        test(false, new String[]{"--no-startup"},
                a -> assertVariable(a, "int", "a"),
                a -> assertMethod(a, "int b() { return 0; }", "()int", "b"),
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> assertImport(a, "import java.util.stream.*;", "", "java.util.stream.*"),
                a -> assertCommand(a, "for (int i = 0; i < 10; ++i) {}", ""),
                a -> assertCommand(a, "/drop 3-5 b 1",
                        "|  dropped class A\n" +
                        "|  dropped method b()\n" +
                        "|  dropped variable a\n"),
                a -> assertCommand(a, "/list", "")
        );
    }

    @Test
    public void testDropNegative() {
        test(false, new String[]{"--no-startup"},
                a -> assertCommandOutputStartsWith(a, "/drop 0", "|  No snippet with ID: 0"),
                a -> assertCommandOutputStartsWith(a, "/drop a", "|  No such snippet: a"),
                a -> assertCommandCheckOutput(a, "/drop",
                        assertStartsWith("|  In the /drop argument, please specify an import, variable, method, or class to drop.")),
                a -> assertVariable(a, "int", "a"),
                a -> assertCommand(a, "a", "a ==> 0"),
                a -> assertCommand(a, "/drop 2", ""),
                a -> assertCommand(a, "/drop 2",
                        "|  This command does not accept the snippet '2' : a\n" +
                        "|  See /types, /methods, /vars, or /list")
        );
    }

    @Test
    public void testAmbiguousDrop() {
        test(
                a -> assertVariable(a, "int", "a"),
                a -> assertMethod(a, "int a() { return 0; }", "()int", "a"),
                a -> assertClass(a, "class a {}", "class", "a"),
                a -> assertCommand(a, "/drop a",
                        "|  dropped variable a\n" +
                        "|  dropped method a()\n" +
                        "|  dropped class a")
        );
        test(
                a -> assertMethod(a, "int a() { return 0; }", "()int", "a"),
                a -> assertMethod(a, "double a(int a) { return 0; }", "(int)double", "a"),
                a -> assertMethod(a, "double a(double a) { return 0; }", "(double)double", "a"),
                a -> assertCommand(a, "/drop a",
                        "|  dropped method a()\n" +
                        "|  dropped method a(int)\n" +
                        "|  dropped method a(double)\n")
        );
    }

    @Test
    public void testApplicationOfPost() {
        test(
                (a) -> assertCommand(a, "/set mode t normal -command", "|  Created new feedback mode: t"),
                (a) -> assertCommand(a, "/set feedback t", "|  Feedback mode: t"),
                (a) -> assertCommand(a, "/set format t post \"$%n\"", ""),
                (a) -> assertCommand(a, "/set prompt t \"+\" \"-\"", ""),
                (a) -> assertCommand(a, "/set prompt t", "|  /set prompt t \"+\" \"-\"$")
        );
    }

    @Test
    public void testHelpLength() {
        Consumer<String> testOutput = (s) -> {
            List<String> ss = Stream.of(s.split("\n"))
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toList());
            assertTrue(ss.size() >= 10, "Help does not print enough lines:" + s);
        };
        test(
                (a) -> assertCommandCheckOutput(a, "/?", testOutput),
                (a) -> assertCommandCheckOutput(a, "/help", testOutput),
                (a) -> assertCommandCheckOutput(a, "/help /list", testOutput)
        );
    }

    @Test
    public void testHelp() {
        test(
                (a) -> assertHelp(a, "/?", "/list", "/help", "/exit", "intro"),
                (a) -> assertHelp(a, "/help", "/list", "/help", "/exit", "intro"),
                (a) -> assertHelp(a, "/help short", "shortcuts", "Tab"),
                (a) -> assertHelp(a, "/help keys", "line", "Shift", "imports", "history"),
                (a) -> assertHelp(a, "/? /li", "/list -all", "snippets"),
                (a) -> assertHelp(a, "/help /set prompt", "optionally contain '%s'", "quoted"),
                (a) -> assertHelp(a, "/help /help", "/help <command>"),
                (a) -> assertHelp(a, "/help li", "/list -start"),
                (a) -> assertHelp(a, "/help fe", "/set feedback -retain")
        );
    }

    @Test
    public void testHelpStart() {
        test(
                (a) -> assertCommandCheckOutput(a, "/help /exit",
                        s -> assertTrue(s.replaceAll("\\r\\n?", "\n").startsWith(
                                "|  \n" +
                                "|                                   /exit\n" +
                                "|                                   =====\n" +
                                "|  "
                        ))
                )
        );
    }

    @Test
    public void testHelpFormat() {
        test(
                (a) -> assertCommandCheckOutput(a, "/help", s -> {
                    String[] lines = s.split("\\R");
                    assertTrue(lines.length > 20,
                            "Too few lines of /help output: " + lines.length
                          + "\n" + s);
                    for (int i = 0; i < lines.length; ++i) {
                        String l = lines[i];
                        assertTrue(l.startsWith("| "),
                                "Expected /help line to start with | :\n" + l);
                        assertTrue(l.length() <= 80,
                                "/help line too long: " + l.length() + "\n" + l);
                    }
                 })
        );
    }

    @Test
    public void testConfusedUserPseudoCommands() {
        test(
                (a) -> assertHelp(a, "/-<n>", "last snippet", "digits"),
                (a) -> assertHelp(a, "/<id>", "last snippet", "digits")
        );
    }

    private void assertHelp(boolean a, String command, String... find) {
        assertCommandCheckOutput(a, command, s -> {
            for (String f : find) {
                assertTrue(s.contains(f),
                        "Expected output of " + command + " to contain: " + f
                      + "\n" + s);
            }
        });
    }

    // Check that each line of output contains the corresponding string from the list
    private void checkLineToList(String in, List<String> match) {
        String trimmed = in.trim();
        String[] res = trimmed.isEmpty()
                ? new String[0]
                : trimmed.split("\n");
        assertEquals(res.length, match.size(), "Got: " + Arrays.asList(res));
        for (int i = 0; i < match.size(); ++i) {
            assertTrue(res[i].contains(match.get(i)));
        }
    }

    @Test
    public void testListArgs() {
        String arg = "qqqq";
        List<String> startVarList = new ArrayList<>(START_UP);
        startVarList.add("int aardvark");
        startVarList.add("int weevil");
        test(
                a -> assertCommandCheckOutput(a, "/list -all",
                        s -> checkLineToList(s, START_UP)),
                a -> assertCommandOutputStartsWith(a, "/list " + arg,
                        "|  No such snippet: " + arg),
                a -> assertVariable(a, "int", "aardvark"),
                a -> assertVariable(a, "int", "weevil"),
                a -> assertCommandOutputContains(a, "/list aardvark", "aardvark"),
                a -> assertCommandCheckOutput(a, "/list -start",
                        s -> checkLineToList(s, START_UP)),
                a -> assertCommandCheckOutput(a, "/list -all",
                        s -> checkLineToList(s, startVarList)),
                a -> assertCommandOutputStartsWith(a, "/list s3",
                        "s3 : import"),
                a -> assertCommandCheckOutput(a, "/list 1-2 s3",
                        s -> {
                            assertTrue(Pattern.matches(".*aardvark.*\\R.*weevil.*\\R.*s3.*import.*", s.trim()),
                                    "No match: " + s);
                        }),
                a -> assertCommandOutputStartsWith(a, "/list " + arg,
                        "|  No such snippet: " + arg)
        );
    }

    @Test
    public void testVarsArgs() {
        String arg = "qqqq";
        List<String> startVarList = new ArrayList<>();
        test(
                a -> assertCommandCheckOutput(a, "/vars -all",
                        s -> checkLineToList(s, startVarList)),
                a -> assertCommand(a, "/vars " + arg,
                        "|  No such snippet: " + arg),
                a -> assertVariable(a, "int", "aardvark"),
                a -> assertMethod(a, "int f() { return 0; }", "()int", "f"),
                a -> assertVariable(a, "int", "a"),
                a -> assertVariable(a, "double", "a", "1", "1.0"),
                a -> assertCommandOutputStartsWith(a, "/vars aardvark",
                        "|    int aardvark = 0"),
                a -> assertCommandCheckOutput(a, "/vars -start",
                        s -> checkLineToList(s, startVarList)),
                a -> assertCommandOutputStartsWith(a, "/vars -all",
                        "|    int aardvark = 0\n|    int a = "),
                a -> assertCommandOutputStartsWith(a, "/vars 1-4",
                        "|    int aardvark = 0\n|    int a = "),
                a -> assertCommandOutputStartsWith(a, "/vars f",
                        "|  This command does not accept the snippet 'f'"),
                a -> assertCommand(a, "/var " + arg,
                        "|  No such snippet: " + arg)
        );
    }

    @Test
    public void testMethodsArgs() {
        String arg = "qqqq";
        List<String> printingMethodList = new ArrayList<>(PRINTING_CMD_METHOD);
        test(new String[]{"--startup", "PRINTING"},
                a -> assertCommandCheckOutput(a, "/methods -all",
                        s -> checkLineToList(s, printingMethodList)),
                a -> assertCommandCheckOutput(a, "/methods -start",
                        s -> checkLineToList(s, printingMethodList)),
                a -> assertCommandCheckOutput(a, "/methods print println printf",
                        s -> checkLineToList(s, printingMethodList)),
                a -> assertCommandCheckOutput(a, "/methods println",
                        s -> assertEquals(s.trim().split("\n").length, 10)),
                a -> assertCommandCheckOutput(a, "/methods",
                        s -> checkLineToList(s, printingMethodList)),
                a -> assertCommandOutputStartsWith(a, "/methods " + arg,
                        "|  No such snippet: " + arg),
                a -> assertMethod(a, "int f() { return 0; }", "()int", "f"),
                a -> assertVariable(a, "int", "aardvark"),
                a -> assertMethod(a, "void f(int a) { g(); }", "(int)void", "f"),
                a -> assertMethod(a, "void g() {}", "()void", "g"),
                a -> assertCommandOutputStartsWith(a, "/methods " + arg,
                        "|  No such snippet: " + arg),
                a -> assertCommandOutputStartsWith(a, "/methods aardvark",
                        "|  This command does not accept the snippet 'aardvark' : int aardvark"),
                a -> assertCommandCheckOutput(a, "/methods -start",
                        s -> checkLineToList(s, printingMethodList)),
                a -> assertCommandCheckOutput(a, "/methods print println printf",
                        s -> checkLineToList(s, printingMethodList)),
                a -> assertCommandOutputStartsWith(a, "/methods g",
                        "|    void g()"),
                a -> assertCommandOutputStartsWith(a, "/methods f",
                        "|    int f()\n" +
                        "|    void f(int)")
        );
    }

    @Test
    public void testMethodsWithErrors() {
        test(new String[]{"--no-startup"},
                a -> assertCommand(a, "double m(int x) { return x; }",
                        "|  created method m(int)"),
                a -> assertCommand(a, "GARBAGE junk() { return TRASH; }",
                        "|  created method junk(), however, it cannot be referenced until class GARBAGE, and variable TRASH are declared"),
                a -> assertCommand(a, "int w = 5;",
                        "w ==> 5"),
                a -> assertCommand(a, "int tyer() { return w; }",
                        "|  created method tyer()"),
                a -> assertCommand(a, "String w = \"hi\";",
                        "w ==> \"hi\""),
                a -> assertCommand(a, "/methods",
                        "|    double m(int)\n" +
                        "|    GARBAGE junk()\n" +
                        "|       which cannot be referenced until class GARBAGE, and variable TRASH are declared\n" +
                        "|    int tyer()\n" +
                        "|       which cannot be invoked until this error is corrected: \n" +
                        "|          incompatible types: java.lang.String cannot be converted to int\n" +
                        "|          int tyer() { return w; }\n" +
                        "|                              ^\n")
        );
    }

    @Test
    public void testTypesWithErrors() {
        test(new String[]{"--no-startup"},
                a -> assertCommand(a, "class C extends NONE { int x; }",
                        "|  created class C, however, it cannot be referenced until class NONE is declared"),
                a -> assertCommand(a, "class D { void m() { System.out.println(nada); } }",
                        "|  created class D, however, it cannot be instantiated or its methods invoked until variable nada is declared"),
                a -> assertCommand(a, "/types",
                        "|    class C\n" +
                        "|       which cannot be referenced until class NONE is declared\n" +
                        "|    class D\n" +
                        "|       which cannot be instantiated or its methods invoked until variable nada is declared\n")
        );
    }

    @Test
    public void testTypesArgs() {
        String arg = "qqqq";
        List<String> startTypeList = new ArrayList<>();
        test(
                a -> assertCommandCheckOutput(a, "/types -all",
                        s -> checkLineToList(s, startTypeList)),
                a -> assertCommandCheckOutput(a, "/types -start",
                        s -> checkLineToList(s, startTypeList)),
                a -> assertCommandOutputStartsWith(a, "/types " + arg,
                        "|  No such snippet: " + arg),
                a -> assertVariable(a, "int", "aardvark"),
                (a) -> assertClass(a, "class A { }", "class", "A"),
                (a) -> assertClass(a, "interface A { }", "interface", "A"),
                a -> assertCommandOutputStartsWith(a, "/types -all",
                        "|    class A\n" +
                        "|    interface A"),
                (a) -> assertClass(a, "enum E { }", "enum", "E"),
                (a) -> assertClass(a, "@interface B { }", "@interface", "B"),
                a -> assertCommand(a, "/types aardvark",
                        "|  This command does not accept the snippet 'aardvark' : int aardvark;"),
                a -> assertCommandOutputStartsWith(a, "/types A",
                        "|    interface A"),
                a -> assertCommandOutputStartsWith(a, "/types E",
                        "|    enum E"),
                a -> assertCommandOutputStartsWith(a, "/types B",
                        "|    @interface B"),
                a -> assertCommandOutputStartsWith(a, "/types " + arg,
                        "|  No such snippet: " + arg),
                a -> assertCommandCheckOutput(a, "/types -start",
                        s -> checkLineToList(s, startTypeList))
        );
    }

    @Test
    public void testBlankLinesInSnippetContinuation() {
        test(Locale.ROOT, false, new String[]{"--no-startup"}, "",
                a -> assertCommand(a, "class C {",
                        ""),
                a -> assertCommand(a, "",
                        ""),
                a -> assertCommand(a, "",
                        ""),
                a -> assertCommand(a, "  int x;",
                        ""),
                a -> assertCommand(a, "",
                        ""),
                a -> assertCommand(a, "",
                        ""),
                a -> assertCommand(a, "}",
                        "|  created class C"),
                a -> assertCommand(a, "/list",
                        "\n" +
                        "   1 : class C {\n" +
                        "       \n" +
                        "       \n" +
                        "         int x;\n" +
                        "       \n" +
                        "       \n" +
                        "       }")
        );
    }

    @Test
    public void testCompoundStart() {
        test(new String[]{"--startup", "DEFAULT", "--startup", "PRINTING"},
                (a) -> assertCommand(a, "printf(\"%4.2f\", Math.PI)",
                        "", "", null, "3.14", "")
        );
    }

    @Test
    public void testJavaSeStart() {
        test(new String[]{"--startup", "JAVASE"},
                (a) -> assertCommand(a, "ZoneOffsetTransitionRule.TimeDefinition.WALL",
                        "$1 ==> WALL")
        );
    }

    @Test
    public void testJavaSeSetStart() {
        test(
                (a) -> assertCommand(a, "/set sta JAVASE", ""),
                (a) -> assertCommand(a, "/reset", "|  Resetting state."),
                (a) -> assertCommandCheckOutput(a, "/li -a",
                            s -> assertTrue(s.split("import ").length > 160,
                            "not enough imports for JAVASE:\n" + s))
        );
    }

    @Test
    public void defineClasses() {
        test(
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "class A { }", "class", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "interface A { }", "interface", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "enum A { }", "enum", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses()),
                (a) -> assertClass(a, "@interface A { }", "@interface", "A"),
                (a) -> assertCommandCheckOutput(a, "/list", assertList()),
                (a) -> assertCommandCheckOutput(a, "/types", assertClasses())
        );
    }

    @Test
    public void testCommandPrefix() {
        test(a -> assertCommandCheckOutput(a, "/s",
                      assertStartsWith("|  Command: '/s' is ambiguous: /save, /set")),
             a -> assertCommand(a, "int var", "var ==> 0"),
             a -> assertCommandCheckOutput(a, "/va",
                      assertStartsWith("|    int var = 0")),
             a -> assertCommandCheckOutput(a, "/save",
                      assertStartsWith("|  '/save' requires a filename argument.")));
    }

    @Test
    public void testOptionQ() {
        test(Locale.ROOT, false, new String[]{"-q", "--no-startup"}, "",
                (a) -> assertCommand(a, "1+1", "$1 ==> 2"),
                (a) -> assertCommand(a, "int x = 5", "")
        );
    }

    @Test
    public void testOptionS() {
        test(Locale.ROOT, false, new String[]{"-s", "--no-startup"}, "",
                (a) -> assertCommand(a, "1+1", "")
        );
    }

    @Test
    public void testOptionV() {
        test(new String[]{"-v", "--no-startup"},
                (a) -> assertCommand(a, "1+1",
                        "$1 ==> 2\n" +
                        "|  created scratch variable $1 : int")
        );
    }

    @Test
    public void testOptionFeedback() {
        test(Locale.ROOT, false, new String[]{"--feedback", "concise", "--no-startup"}, "",
                (a) -> assertCommand(a, "1+1", "$1 ==> 2"),
                (a) -> assertCommand(a, "int x = 5", "")
        );
    }

    @Test
    public void testCompoundOptions() {
        Consumer<String> confirmNoStartup = s -> {
                    assertEquals(0, Stream.of(s.split("\n"))
                            .filter(l -> !l.isEmpty())
                            .count(), "Expected no lines: " + s);
                };
        test(Locale.ROOT, false, new String[]{"-nq"}, "",
                (a) -> assertCommandCheckOutput(a, "/list -all", confirmNoStartup),
                (a) -> assertCommand(a, "1+1", "$1 ==> 2"),
                (a) -> assertCommand(a, "int x = 5", "")
        );
        test(Locale.ROOT, false, new String[]{"-qn"}, "",
                (a) -> assertCommandCheckOutput(a, "/list -all", confirmNoStartup),
                (a) -> assertCommand(a, "1+1", "$1 ==> 2"),
                (a) -> assertCommand(a, "int x = 5", "")
        );
        test(Locale.ROOT, false, new String[]{"-ns"}, "",
                (a) -> assertCommandCheckOutput(a, "/list -all", confirmNoStartup),
                (a) -> assertCommand(a, "1+1", "")
        );
    }

    @Test
    public void testOptionR() {
        test(new String[]{"-R-Dthe.sound=blorp", "--no-startup"},
                (a) -> assertCommand(a, "System.getProperty(\"the.sound\")",
                        "$1 ==> \"blorp\"")
        );
    }

    @Test
    public void testWrapSourceHandlerDiagCrash() {
        test(new String[]{"--add-exports", "jdk.javadoc/ALL-UNNAMED"},
                (a) -> assertCommand(a, "1+1", "$1 ==> 2")
         );
    }

    @Test
    public void test8156910() {
        test(
                (a) -> assertCommandOutputContains(a, "System.out.println(\"%5d\", 10);", "%5d"),
                (a) -> assertCommandOutputContains(a, "1234", "==> 1234")
        );
    }

    @Test
    public void testIntersection() {
        test(
                (a) -> assertCommandOutputContains(a, "<Z extends Runnable&CharSequence> Z get1() { return null; }", "get1()"),
                (a) -> assertCommandOutputContains(a, "var g1 = get1()", "g1"),
                (a) -> assertCommand(a, "/vars g1", "|    CharSequence&Runnable g1 = null"),
                (a) -> assertCommandOutputContains(a, "<Z extends Number&CharSequence> Z get2() { return null; }", "get2()"),
                (a) -> assertCommandOutputContains(a, "var g2 = get2()", "g2"),
                (a) -> assertCommand(a, "/vars g2", "|    Number&CharSequence g2 = null")
        );
    }

    @Test
    public void testAnonymous() {
        test(
                (a) -> assertCommandOutputContains(a, "var r1 = new Object() {}", "r1"),
                (a) -> assertCommandOutputContains(a, "/vars r1", "|    <anonymous class extending Object> r1 = "),
                (a) -> assertCommandOutputContains(a, "var r2 = new Runnable() { public void run() { } }", "r2"),
                (a) -> assertCommandOutputContains(a, "/vars r2", "|    <anonymous class implementing Runnable> r2 = "),
                (a) -> assertCommandOutputContains(a, "import java.util.stream.*;", ""),
                (a) -> assertCommandOutputContains(a, "var list = Stream.of(1, 2, 3).map(j -> new Object() { int i = j; }).collect(Collectors.toList());",
                                                      "list"),
                (a) -> assertCommandOutputContains(a, "/vars list", "|    List<<anonymous class extending Object>> list = ")
        );
    }

    // This is mainly interesting in the TestLocalSimpleTest case (8198573)
    @Test
    public void testUpdateFalsePositive() {
        test(
                a -> assertClass(a, "class A { int a() { int error = 0; return error; } }", "class", "A"),
                a -> assertVariable(a, "A", "a", "new A()", "A@.+"),
                a -> assertVariable(a, "int", "error", "4711", "4711"),
                a -> assertCommandOutputContains(a, "a", "A@")
        );
    }

    @Test
    public void testRecords() {
        test(new String[] {},
                (a) -> assertCommandOutputContains(a, "record R(int i) { public int g() { return j; } }",
                        "|  created record R, however, it cannot be instantiated or its methods invoked until variable j is declared"),
                (a) -> assertCommandOutputContains(a, "new R(0)",
                        "|  attempted to use record R which cannot be instantiated or its methods invoked until variable j is declared")
        );
    }

    @Test
    public void testImportChange() {
        for (String feedback : new String[] {"verbose", "normal"}) {
            test(
                    (a) -> assertCommandOutputContains(a, "/set feedback " + feedback, "|  Feedback mode: " + feedback),
                    (a) -> assertCommand(a, "import java.util.*", ""),
                    (a) -> assertCommandOutputContains(a, "var v1 = List.of(1);", "v1 ==> [1]"),
                    (a) -> assertCommandOutputContains(a, "import java.awt.List;",
                            "|    update replaced variable v1 which cannot be referenced until this error is corrected:"),
                    (a) -> assertCommandOutputContains(a, "var b = java.util.List.of(\"bb\")",
                            "b ==> [bb]"),
                    (a) -> assertCommandOutputContains(a, "b", "b ==> [bb]")
            );
        }
    }

    @Test
    public void testSwitchStatementExpressionDisambiguation() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertCommand(a, "switch (0) { default -> 0; }", "$1 ==> 0"),
                (a) -> assertCommand(a, "int i;", "i ==> 0"),
                (a) -> assertCommand(a, "switch (0) { case 0 -> i = 1; }", ""),
                (a) -> assertCommand(a, "i", "i ==> 1"),
                (a) -> assertCommandOutputStartsWith(a, "switch (0) { default -> throw new IllegalStateException(); }", "|  Exception java.lang.IllegalStateException")
                );
        test(false, new String[]{"--no-startup", "-C-source", "-C8"},
                (a) -> assertCommand(a, "int i;", "i ==> 0"),
                (a) -> assertCommand(a, "switch (0) { default: i = 1; }", ""),
                (a) -> assertCommand(a, "i", "i ==> 1")
                );
    }
}
