/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5030233 6214916 6356475 6571029 6684582 6742159 4459600 6758881 6753938
 *      6894719 6968053 7151434 7146424 8007333 8077822 8143640 8132379 8218547
 * @summary Argument parsing validation.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile -XDignore.symbol.file Arrrghs.java
 * @run main/othervm Arrrghs
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Arrrghs extends TestHelper {
    private Arrrghs(){}
    /**
     * This class provides various tests for arguments processing.
     *
     * History: these set of tests  were part of Arrrghs.sh. The MKS shell
     * implementations were notoriously buggy. Implementing these tests purely
     * in Java is not only portable but also robust.
     *
     */

    // the pattern we hope to see in the output
    static final Pattern ArgPattern = Pattern.compile("\\s*argv\\[[0-9]*\\].*=.*");

    void checkArgumentParsing(String inArgs, String... expArgs) throws IOException {
        List<String> scratchpad = new ArrayList<>();
        scratchpad.add("set " + JLDEBUG_KEY + "=true");
        // GAK, -version needs to be added so that windows can flush its stderr
        // exiting the process prematurely can terminate the stderr.
        scratchpad.add(javaCmd + " -version " + inArgs);
        File batFile = new File("atest.bat");
        createAFile(batFile, scratchpad);

        TestResult tr = doExec(batFile.getName());

        ArrayList<String> expList = new ArrayList<>();
        expList.add(javaCmd);
        expList.add("-version");
        expList.addAll(Arrays.asList(expArgs));

        List<String> gotList = new ArrayList<>();
        for (String x : tr.testOutput) {
            Matcher m = ArgPattern.matcher(x);
            if (m.matches()) {
                String a[] = x.split("=");
                gotList.add(a[a.length - 1].trim());
            }
        }
        if (!gotList.equals(expList)) {
            System.out.println(tr);
            System.out.println("Expected args:");
            System.out.println(expList);
            System.out.println("Obtained args:");
            System.out.println(gotList);
            throw new RuntimeException("Error: args do not match");
        }
        System.out.println("\'" + inArgs + "\'" + " - Test passed");
    }

    /*
     * This tests general quoting and are specific to Windows, *nixes
     * need not worry about this, these have been tested with Windows
     * implementation and those that are known to work are used against
     * the java implementation. Note that the ProcessBuilder gets in the
     * way when testing some of these arguments, therefore we need to
     * create and execute a .bat file containing the arguments.
     */
    @Test
    void testArgumentParsing() throws IOException {
        if (!isWindows)
            return;
        // no quotes
        checkArgumentParsing("a b c d", "a", "b", "c", "d");

        // single quotes
        checkArgumentParsing("\"a b c d\"", "a b c d");

        //double quotes
        checkArgumentParsing("\"\"a b c d\"\"", "a", "b", "c", "d");

        // triple quotes
        checkArgumentParsing("\"\"\"a b c d\"\"\"", "\"a b c d\"");

        // a literal within single quotes
        checkArgumentParsing("\"a\"b c d\"e\"", "ab", "c", "de");

        // a literal within double quotes
        checkArgumentParsing("\"\"a\"b c d\"e\"\"", "ab c de");

        // a literal quote
        checkArgumentParsing("a\\\"b", "a\"b");

        // double back-slash
        checkArgumentParsing("\"a b c d\\\\\"", "a b c d\\");

        // triple back-slash
        checkArgumentParsing("a\\\\\\\"b", "a\\\"b");

        // dangling quote
        checkArgumentParsing("\"a b c\"\"", "a b c\"");

        // expansions of white space separators
        checkArgumentParsing("a b", "a", "b");
        checkArgumentParsing("a\tb", "a", "b");
        checkArgumentParsing("a \t b", "a", "b");

        checkArgumentParsing("\"C:\\TEST A\\\\\"", "C:\\TEST A\\");
        checkArgumentParsing("\"\"C:\\TEST A\\\\\"\"", "C:\\TEST", "A\\");

        // MS Windows tests
        // triple back-slash
        checkArgumentParsing("a\\\\\\d", "a\\\\\\d");

        // triple back-slash in quotes
        checkArgumentParsing("\"a\\\\\\d\"", "a\\\\\\d");

        // slashes separating characters
        checkArgumentParsing("X\\Y\\Z", "X\\Y\\Z");
        checkArgumentParsing("\\X\\Y\\Z", "\\X\\Y\\Z");

        // literals within dangling quotes, etc.
        checkArgumentParsing("\"a b c\" d e", "a b c", "d", "e");
        checkArgumentParsing("\"ab\\\"c\"  \"\\\\\"  d", "ab\"c", "\\", "d");
        checkArgumentParsing("a\\\\\\c d\"e f\"g h", "a\\\\\\c", "de fg", "h");
        checkArgumentParsing("a\\\\\\\"b c d", "a\\\"b", "c", "d");
        checkArgumentParsing("a\\\\\\\\\"g c\" d e", "a\\\\g c", "d", "e");

        // treatment of back-slashes
        checkArgumentParsing("*\\", "*\\");
        checkArgumentParsing("*/", "*/");
        checkArgumentParsing(".\\*", ".\\*");
        checkArgumentParsing("./*", "./*");
        checkArgumentParsing("..\\..\\*", "..\\..\\*");
        checkArgumentParsing("../../*", "../../*");
        checkArgumentParsing("..\\..\\", "..\\..\\");
        checkArgumentParsing("../../", "../../");
        checkArgumentParsing("a b\\ c", "a", "b\\", "c");
        // 2 back-slashes
        checkArgumentParsing("\\\\?", "\\\\?");
        // 3 back-slashes
        checkArgumentParsing("\\\\\\?", "\\\\\\?");
        // 4 back-slashes
        checkArgumentParsing("\\\\\\\\?", "\\\\\\\\?");
        // 5 back-slashes
        checkArgumentParsing("\\\\\\\\\\?", "\\\\\\\\\\?");
        // 6 back-slashes
        checkArgumentParsing("\\\\\\\\\\\\?", "\\\\\\\\\\\\?");

        // more treatment of  mixed slashes
        checkArgumentParsing("f1/ f3\\ f4/", "f1/", "f3\\", "f4/");
        checkArgumentParsing("f1/ f2\' ' f3/ f4/", "f1/", "f2\'", "'", "f3/", "f4/");

        checkArgumentParsing("a\\*\\b", "a\\*\\b");
    }

    private void initEmptyDir(File emptyDir) throws IOException {
        if (emptyDir.exists()) {
            recursiveDelete(emptyDir);
        }
        emptyDir.mkdir();
    }

    private void initDirWithJavaFiles(File libDir) throws IOException {

        if (libDir.exists()) {
            recursiveDelete(libDir);
        }
        libDir.mkdirs();
        ArrayList<String> scratchpad = new ArrayList<>();
        scratchpad.add("package lib;");
        scratchpad.add("public class Fbo {");
        scratchpad.add("public static void main(String... args){Foo.f();}");
        scratchpad.add("public static void f(){}");
        scratchpad.add("}");
        createFile(new File(libDir, "Fbo.java"), scratchpad);

        scratchpad.clear();
        scratchpad.add("package lib;");
        scratchpad.add("public class Foo {");
        scratchpad.add("public static void main(String... args){");
        scratchpad.add("for (String x : args) {");
        scratchpad.add("System.out.println(x);");
        scratchpad.add("}");
        scratchpad.add("Fbo.f();");
        scratchpad.add("}");
        scratchpad.add("public static void f(){}");
        scratchpad.add("}");
        createFile(new File(libDir, "Foo.java"), scratchpad);
    }

    void checkArgumentWildcard(String inArgs, String... expArgs) throws IOException {
        String[] in = {inArgs};
        checkArgumentWildcard(in, expArgs);

        // now add arbitrary arguments before and after
        String[] outInArgs = { "-Q", inArgs, "-R"};

        String[] outExpArgs = new String[expArgs.length + 2];
        outExpArgs[0] = "-Q";
        System.arraycopy(expArgs, 0, outExpArgs, 1, expArgs.length);
        outExpArgs[expArgs.length + 1] = "-R";
        checkArgumentWildcard(outInArgs, outExpArgs);
    }

    void checkArgumentWildcard(String[] inArgs, String[] expArgs) throws IOException {
        ArrayList<String> argList = new ArrayList<>();
        argList.add(javaCmd);
        argList.add("-cp");
        argList.add("lib" + File.separator + "*");
        argList.add("lib.Foo");
        argList.addAll(Arrays.asList(inArgs));
        String[] cmds = new String[argList.size()];
        argList.toArray(cmds);
        TestResult tr = doExec(cmds);
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("Error: classpath single entry wildcard entry");
        }

        ArrayList<String> expList = new ArrayList<>();
        expList.addAll(Arrays.asList(expArgs));

        List<String> gotList = new ArrayList<>();
        for (String x : tr.testOutput) {
            gotList.add(x.trim());
        }
        if (!gotList.equals(expList)) {
            System.out.println(tr);
            System.out.println("Expected args:");
            System.out.println(expList);
            System.out.println("Obtained args:");
            System.out.println(gotList);
            throw new RuntimeException("Error: args do not match");
        }
        System.out.print("\'");
        for (String x : inArgs) {
            System.out.print(x + " ");
        }
        System.out.println("\'" + " - Test passed");
    }

    /*
     * These tests are not expected to work on *nixes, and are ignored.
     */
    @Test
    void testWildCardArgumentProcessing() throws IOException {
        if (!isWindows)
            return;
        File cwd = new File(".");
        File libDir = new File(cwd, "lib");
        initDirWithJavaFiles(libDir);
        initEmptyDir(new File(cwd, "empty"));

        // test if javac (the command) can compile *.java
        TestResult tr = doExec(javacCmd, libDir.getName() + File.separator + "*.java");
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("Error: compiling java wildcards");
        }

        // test if javac (the command) can compile *.java with a vmoption
        tr = doExec(javacCmd, "-cp", ".",
                    "-J-showversion", "-J-Dsomeproperty=foo",
                    libDir.getName() + File.separator + "*.java");
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("Error: compiling java wildcards with vmoptions");
        }


        // use the jar cmd to create jars using the ? wildcard
        File jarFoo = new File(libDir, "Foo.jar");
        tr = doExec(jarCmd, "cvf", jarFoo.getAbsolutePath(), "lib" + File.separator + "F?o.class");
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("Error: creating jar with wildcards");
        }

        // now the litmus test!, this should work
        checkArgumentWildcard("a", "a");

        // test for basic expansion
        checkArgumentWildcard("lib\\F*java", "lib\\Fbo.java", "lib\\Foo.java");

        // basic expansion in quotes
        checkArgumentWildcard("\"lib\\F*java\"", "lib\\F*java");

        checkArgumentWildcard("lib\\**", "lib\\Fbo.class", "lib\\Fbo.java",
                              "lib\\Foo.class", "lib\\Foo.jar", "lib\\Foo.java");

        checkArgumentWildcard("lib\\*?", "lib\\Fbo.class", "lib\\Fbo.java",
                              "lib\\Foo.class", "lib\\Foo.jar", "lib\\Foo.java");

        checkArgumentWildcard("lib\\?*", "lib\\Fbo.class", "lib\\Fbo.java",
                "lib\\Foo.class", "lib\\Foo.jar", "lib\\Foo.java");

        checkArgumentWildcard("lib\\?", "lib\\?");

        // test for basic expansion
        checkArgumentWildcard("lib\\*java", "lib\\Fbo.java", "lib\\Foo.java");

        // basic expansion in quotes
        checkArgumentWildcard("\"lib\\*.java\"", "lib\\*.java");

        // suffix expansion
        checkArgumentWildcard("lib\\*.class", "lib\\Fbo.class", "lib\\Foo.class");

        // suffix expansion in quotes
        checkArgumentWildcard("\"lib\\*.class\"", "lib\\*.class");

        // check for ? expansion now
        checkArgumentWildcard("lib\\F?o.java", "lib\\Fbo.java", "lib\\Foo.java");

        // check ? in quotes
        checkArgumentWildcard("\"lib\\F?o.java\"", "lib\\F?o.java");

        // check ? as suffixes
        checkArgumentWildcard("lib\\F?o.????", "lib\\Fbo.java", "lib\\Foo.java");

        // check ? in a leading role
        checkArgumentWildcard("lib\\???.java", "lib\\Fbo.java", "lib\\Foo.java");
        checkArgumentWildcard("\"lib\\???.java\"", "lib\\???.java");

        // check ? prefixed with -
        checkArgumentWildcard("-?", "-?");

        // check * prefixed with -
        checkArgumentWildcard("-*", "-*");

        // check on empty directory
        checkArgumentWildcard("empty\\*", "empty\\*");
        checkArgumentWildcard("empty\\**", "empty\\**");
        checkArgumentWildcard("empty\\?", "empty\\?");
        checkArgumentWildcard("empty\\??", "empty\\??");
        checkArgumentWildcard("empty\\*?", "empty\\*?");
        checkArgumentWildcard("empty\\?*", "empty\\?*");

        // 8132379: java should not filter out -J options for application
        String[] args = { "-J-one", "-Jtwo", "lib\\???.java", "-J-Dsomething",
           "a", "-J-Dlast.arg" };
        String[] expected = { "-J-one", "-Jtwo", "lib\\Fbo.java",
           "lib\\Foo.java", "-J-Dsomething", "a", "-J-Dlast.arg" };
        checkArgumentWildcard(args, expected);
    }

    void doArgumentCheck(String inArgs, String... expArgs) {
        Map<String, String> env = new HashMap<>();
        env.put(JLDEBUG_KEY, "true");
        TestResult tr = doExec(env, javaCmd, inArgs);
        System.out.println(tr);
        int sindex = tr.testOutput.indexOf("Command line args:");
        if (sindex < 0) {
            System.out.println(tr);
            throw new RuntimeException("Error: no output");
        }
        sindex++; // skip over the tag
        List<String> gotList = new ArrayList<>();
        for (String x : tr.testOutput.subList(sindex, sindex + expArgs.length)) {
            String a[] = x.split("=");
            gotList.add(a[a.length - 1].trim());
        }
        List<String> expList = Arrays.asList(expArgs);
        if (!gotList.equals(expList)) {
            System.out.println(tr);
            System.out.println("Expected args:");
            System.out.println(expList);
            System.out.println("Obtained args:");
            System.out.println(gotList);
            throw new RuntimeException("Error: args do not match");
        }
    }


    /*
     * These tests are usually run on non-existent targets to check error results
     */
    @Test
    void testBasicErrorMessages() {
        // Tests for 5030233
        TestResult tr = doExec(javaCmd, "-cp");
        tr.checkNegative();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        tr = doExec(javaCmd, "-classpath");
        tr.checkNegative();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        tr = doExec(javaCmd, "-jar");
        tr.checkNegative();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        tr = doExec(javacCmd, "-cp");
        tr.checkNegative();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        // Test for 6356475 "REGRESSION:"java -X" from cmdline fails"
        tr = doExec(javaCmd, "-X");
        tr.checkPositive();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        tr = doExec(javaCmd, "-help");
        tr.checkPositive();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        // 6753938, test for non-negative exit value for an incorrectly formed
        // command line,  '% java'
        tr = doExec(javaCmd);
        tr.checkNegative();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        // 6753938, test for non-negative exit value for an incorrectly formed
        // command line,  '% java -Xcomp'
        tr = doExec(javaCmd, "-Xcomp");
        tr.checkNegative();
        tr.isNotZeroOutput();
        if (!tr.testStatus)
            System.out.println(tr);

        // 7151434, test for non-negative exit value for an incorrectly formed
        // command line, '% java -jar -W', note the bogus -W
        tr = doExec(javaCmd, "-jar", "-W");
        tr.checkNegative();
        tr.contains("Unrecognized option: -W");
        if (!tr.testStatus)
            System.out.println(tr);
    }

    /*
     * Tests -jar command on a jar file with "long" (> 260 chars) full path on Windows
     */
    @Test
    void testLongPathJarFile() throws IOException {
        if (!isWindows) {
            return;
        }
        // put the jar file to a location with long path
        String longPathPart = "longpathtest_longpathtest/";
        String longPathStr = longPathPart.repeat(15);
        Path longPath = Paths.get(longPathStr);
        Path jarPath = Files.createDirectories(longPath).resolve("elp.jar");
        File elp = jarPath.toFile();
        createJar(elp, new File("Foo"), "public static void main(String[] args){ System.out.println(\"Hello from ELP\"); }");
        System.out.println("execute " + elp.getAbsolutePath());
        TestResult tr = doExec(javaCmd, "-jar", elp.getAbsolutePath());
        tr.checkPositive();
        tr.contains("Hello from ELP");
    }

    /*
     * Tests various dispositions of the main method, these tests are limited
     * to English locales as they check for error messages that are localized.
     */
    @Test
    void testMainMethod() throws FileNotFoundException {
        if (!isEnglishLocale()) {
            return;
        }

        TestResult tr;

        // a missing class
        createJar("MIA", new File("some.jar"), new File("Foo"),
                (String[])null);
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.contains("Error: Could not find or load main class MIA");
        if (!tr.testStatus)
            System.out.println(tr);
        // use classpath to check
        tr = doExec(javaCmd, "-cp", "some.jar", "MIA");
        tr.contains("Error: Could not find or load main class MIA");
        if (!tr.testStatus)
            System.out.println(tr);

        // incorrect method access
        createJar(new File("some.jar"), new File("Foo"),
                "private static void main(String[] args){}");
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.contains("Error: Main method not found in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);
        // use classpath to check
        tr = doExec(javaCmd, "-cp", "some.jar", "Foo");
        tr.contains("Error: Main method not found in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);

        // incorrect return type
        createJar(new File("some.jar"), new File("Foo"),
                "public static int main(String[] args){return 1;}");
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.contains("Error: Main method must return a value of type void in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);
        // use classpath to check
        tr = doExec(javaCmd, "-cp", "some.jar", "Foo");
        tr.contains("Error: Main method must return a value of type void in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);

        // incorrect parameter type
        createJar(new File("some.jar"), new File("Foo"),
                "public static void main(Object[] args){}");
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.contains("Error: Main method not found in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);
        // use classpath to check
        tr = doExec(javaCmd, "-cp", "some.jar", "Foo");
        tr.contains("Error: Main method not found in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);

        // incorrect method type - non-static
         createJar(new File("some.jar"), new File("Foo"),
                "public void main(String[] args){}");
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.contains("Error: Main method is not static in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);
        // use classpath to check
        tr = doExec(javaCmd, "-cp", "some.jar", "Foo");
        tr.contains("Error: Main method is not static in class Foo");
        if (!tr.testStatus)
            System.out.println(tr);

        // amongst a potpourri of kindred main methods, is the right one chosen ?
        createJar(new File("some.jar"), new File("Foo"),
            "void main(Object[] args){}",
            "int  main(Float[] args){return 1;}",
            "private void main() {}",
            "private static void main(int x) {}",
            "public int main(int argc, String[] argv) {return 1;}",
            "public static void main(String[] args) {System.out.println(\"THE_CHOSEN_ONE\");}");
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.contains("THE_CHOSEN_ONE");
        if (!tr.testStatus)
            System.out.println(tr);
        // use classpath to check
        tr = doExec(javaCmd, "-cp", "some.jar", "Foo");
        tr.contains("THE_CHOSEN_ONE");
        if (!tr.testStatus)
            System.out.println(tr);

        // test for extraneous whitespace in the Main-Class attribute
        createJar(" Foo ", new File("some.jar"), new File("Foo"),
                "public static void main(String... args){}");
        tr = doExec(javaCmd, "-jar", "some.jar");
        tr.checkPositive();
        if (!tr.testStatus)
            System.out.println(tr);
    }
    /*
     * tests 6968053, ie. we turn on the -Xdiag (for now) flag and check if
     * the suppressed stack traces are exposed, ignore these tests for localized
     * locales, limiting to English only.
     */
    @Test
    void testDiagOptions() throws FileNotFoundException {
        if (!isEnglishLocale()) { // only english version
            return;
        }
        TestResult tr;
        // a missing class
        createJar("MIA", new File("some.jar"), new File("Foo"),
                (String[])null);
        tr = doExec(javaCmd, "-Xdiag", "-jar", "some.jar");
        tr.contains("Error: Could not find or load main class MIA");
        tr.contains("java.lang.ClassNotFoundException: MIA");
        if (!tr.testStatus)
            System.out.println(tr);

        // use classpath to check
        tr = doExec(javaCmd,  "-Xdiag", "-cp", "some.jar", "MIA");
        tr.contains("Error: Could not find or load main class MIA");
        tr.contains("java.lang.ClassNotFoundException: MIA");
        if (!tr.testStatus)
            System.out.println(tr);

        // a missing class on the classpath
        tr = doExec(javaCmd, "-Xdiag", "NonExistentClass");
        tr.contains("Error: Could not find or load main class NonExistentClass");
        tr.contains("java.lang.ClassNotFoundException: NonExistentClass");
        if (!tr.testStatus)
            System.out.println(tr);
    }

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws Exception {
        if (debug) {
            System.out.println("Starting Arrrghs tests");
        }
        Arrrghs a = new Arrrghs();
        a.run(args);
        if (testExitValue > 0) {
            System.out.println("Total of " + testExitValue + " failed");
            System.exit(1);
        } else {
            System.out.println("All tests pass");
        }
    }
}
