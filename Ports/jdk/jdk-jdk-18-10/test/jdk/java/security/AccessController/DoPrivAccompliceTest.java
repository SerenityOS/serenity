/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.helpers.ClassFileInstaller;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * @test
 * @bug 8048362
 * @summary Tests the doPrivileged with accomplice Generate two jars
 * (DoPrivTest.jar and DoPrivAccomplice.jar) and grant permission to
 * DoPrivAccmplice.jar for reading user.name property from a PrivilagedAction.
 * Run DoPrivTest.jar and try to access user.name property using
 * DoPrivAccmplice.jar.
 *
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main/othervm DoPrivAccompliceTest
 */

public class DoPrivAccompliceTest {
    private static final String ACTION_SOURCE = DoPrivAccomplice.class.getName();
    private static final String TEST_SOURCE = DoPrivTest.class.getName();

    private static void createPolicyFile(Path jarFile, Path policy) {
        String codebase = jarFile.toFile().toURI().toString();
        String quotes = "\"";
        StringBuilder policyFile = new StringBuilder();
        policyFile.append("grant codeBase ")
                  .append(quotes).append(codebase).append(quotes)
                  .append("{\n")
                  .append("permission java.util.PropertyPermission ")
                  .append(quotes).append("user.name").append(quotes)
                  .append(",")
                  .append(quotes).append("read").append(quotes)
                  .append(";\n};");
        try (FileWriter writer = new FileWriter(policy.toFile())) {
            writer.write(policyFile.toString());
        } catch (IOException e) {
            throw new Error("Error while creating policy file " + policy, e);
        }
    }

    public static void main(String[] args) throws Exception {
        // copy class files to pwd
        ClassFileInstaller.main(ACTION_SOURCE, TEST_SOURCE);
        Path pwd = Paths.get(".");
        Path jarFile1 = pwd.resolve(ACTION_SOURCE + ".jar").toAbsolutePath();
        Path jarFile2 = pwd.resolve(TEST_SOURCE + ".jar").toAbsolutePath();
        Path policy = pwd.resolve("java.policy").toAbsolutePath();

        JarUtils.createJar(jarFile1.toString(), ACTION_SOURCE + ".class");
        System.out.println("Created jar file " + jarFile1);
        JarUtils.createJar(jarFile2.toString(), TEST_SOURCE + ".class");
        System.out.println("Created jar file " + jarFile2);


        String pathSepartor = System.getProperty("path.separator");
        String[] commands = {
                "-Djava.security.manager",
                "-Djava.security.policy=" + policy,
                "-classpath", jarFile1 + pathSepartor + jarFile2,
                TEST_SOURCE
        };

        String userName = System.getProperty("user.name");

        createPolicyFile(jarFile1, policy);
        System.out.println("Created policy for " + jarFile1);
        ProcessTools.executeTestJava(commands)
                    .shouldHaveExitValue(0)
                    .shouldContain(userName)
                    .stderrShouldBeEmptyIgnoreWarnings();

        createPolicyFile(jarFile2, policy);
        System.out.println("Created policy for " + jarFile2);
        ProcessTools.executeTestJava(commands)
                    .shouldNotHaveExitValue(0)
                    .shouldNotContain(userName)
                    .stderrShouldContain("java.security.AccessControlException");

        System.out.println("Test PASSES");
    }
}
