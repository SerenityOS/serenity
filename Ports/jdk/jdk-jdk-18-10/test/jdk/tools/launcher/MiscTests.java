/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6856415 8154212 8154470
 * @summary Miscellaneous tests, Exceptions
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile -XDignore.symbol.file MiscTests.java
 * @run main MiscTests
 */


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiscTests extends TestHelper {

    /**
     * Test with class path set on the command line via -Djava.class.path
     */
    static void testWithClassPathSetViaProperty() throws IOException {
        final String mainClass = "Foo";

        File source = new File(mainClass + ".java");

        List<String> scratch = new ArrayList<>();
        scratch.add("public class Foo {");
        scratch.add("public static void main(String... args) {");
        scratch.add("}");
        scratch.add("}");
        createFile(source, scratch);

        compile(mainClass + ".java");

        String dir = new File(mainClass + ".class").getAbsoluteFile().getParent();
        TestResult tr = doExec(javaCmd, "-Djava.class.path=" + dir, mainClass);
        for (String s : tr.testOutput) {
            System.out.println(s);
        }
    }

    /**
     * 6856415: Checks to ensure that proper exceptions are thrown by java
     */
    static void test6856415() throws IOException {

        final String mainClass = "Foo6856415";

        List<String> scratch = new ArrayList<>();
        scratch.add("public class Foo6856415 {");
        scratch.add("public static void main(String... args) {");
        scratch.add("java.security.Provider p = new sun.security.pkcs11.SunPKCS11();");
        scratch.add("java.security.Security.insertProviderAt(p, 1);");
        scratch.add("}");
        scratch.add("}");
        createFile(new File(mainClass + ".java"), scratch);

        compile(mainClass + ".java",
                "--add-modules=jdk.crypto.cryptoki",
                "--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED");

        File testJar = new File("Foo.jar");
        testJar.delete();
        String jarArgs[] = {
            (debug) ? "cvfe" : "cfe",
            testJar.getAbsolutePath(),
            mainClass,
            mainClass + ".class"
        };
        createJar(jarArgs);

        TestResult tr = doExec(javaCmd,
                "-Djava.security.manager", "-jar", testJar.getName(), "foo.bak");
        if (!tr.contains("java.security.AccessControlException:" +
                " access denied (\"java.lang.RuntimePermission\"" +
                " \"accessClassInPackage.sun.security.pkcs11\")")) {
            System.out.println(tr);
        }
    }

    static void testJLDEnv() {
        final Map<String, String> envToSet = new HashMap<>();
        envToSet.put("_JAVA_LAUNCHER_DEBUG", "true");
        for (String cmd : new String[] { javaCmd, javacCmd }) {
            TestResult tr = doExec(envToSet, cmd, "-version");
            tr.checkPositive();
            String javargs = cmd.equals(javacCmd) ? "on" : "off";
            String progname = cmd.equals(javacCmd) ? "javac" : "java";
            if (!tr.isOK()
                || !tr.matches("\\s*debug:on$")
                || !tr.matches("\\s*javargs:" + javargs + "$")
                || !tr.matches("\\s*program name:" + progname + "$")) {
                System.out.println(tr);
            }
        }
    }

    public static void main(String... args) throws IOException {
        testWithClassPathSetViaProperty();
        test6856415();
        testJLDEnv();
        if (testExitValue != 0) {
            throw new Error(testExitValue + " tests failed");
        }
    }
}
