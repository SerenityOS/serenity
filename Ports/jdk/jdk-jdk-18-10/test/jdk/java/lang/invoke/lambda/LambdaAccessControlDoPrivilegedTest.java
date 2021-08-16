/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003881
 * @summary tests DoPrivileged action (implemented as lambda expressions) by
 * inserting them into the BootClassPath.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile -XDignore.symbol.file LambdaAccessControlDoPrivilegedTest.java LUtils.java
 * @run main/othervm -Djava.security.manager=allow LambdaAccessControlDoPrivilegedTest
 */
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LambdaAccessControlDoPrivilegedTest extends LUtils {
    public static void main(String... args) {
        final List<String> scratch = new ArrayList();
        scratch.clear();
        scratch.add("import java.security.*;");
        scratch.add("public class DoPriv {");
        scratch.add("public static void main(String... args) {");
        scratch.add("String prop = AccessController.doPrivileged((PrivilegedAction<String>) () -> {");
        scratch.add("return System.getProperty(\"user.home\");");
        scratch.add("});");
        scratch.add("}");
        scratch.add("}");
        File doprivJava = new File("DoPriv.java");
        File doprivClass = getClassFile(doprivJava);
        createFile(doprivJava, scratch);

        scratch.clear();
        scratch.add("public class Bar {");
        scratch.add("public static void main(String... args) {");
        scratch.add("System.setSecurityManager(new SecurityManager());");
        scratch.add("DoPriv.main();");
        scratch.add("}");
        scratch.add("}");

        File barJava = new File("Bar.java");
        File barClass = getClassFile(barJava);
        createFile(barJava, scratch);

        String[] javacArgs = {barJava.getName(), doprivJava.getName()};
        compile(javacArgs);
        File jarFile = new File("foo.jar");
        String[] jargs = {"cvf", jarFile.getName(), doprivClass.getName()};
        TestResult tr = doExec(JAR_CMD.getAbsolutePath(),
                                "cvf", jarFile.getName(),
                                doprivClass.getName());
        if (tr.exitValue != 0){
            throw new RuntimeException(tr.toString());
        }
        doprivJava.delete();
        doprivClass.delete();
        tr = doExec(JAVA_CMD.getAbsolutePath(),
                    "-Xbootclasspath/a:foo.jar",
                    "-cp", ".",
                    "-Djava.security.manager=allow",
                    "Bar");
        tr.assertZero("testDoPrivileged fails");
        barJava.delete();
        barClass.delete();
        jarFile.delete();
    }
}
