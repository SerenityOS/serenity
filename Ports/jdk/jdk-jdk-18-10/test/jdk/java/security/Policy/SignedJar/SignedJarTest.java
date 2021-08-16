/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug 8048360 8242565
 * @summary test policy entry with signedBy alias
 * @library /test/lib
 * @run main/othervm SignedJarTest
 */
public class SignedJarTest {

    private static final String FS = File.separator;
    private static final String JAVA_HOME = System.getProperty("test.jdk");
    private static final String TESTCLASSES = System.getProperty("test.classes", "");
    private static final String TESTSRC = System.getProperty("test.src", "");
    private static final String KEYTOOL = JAVA_HOME + FS + "bin" + FS + "keytool";
    private static final String JAR = JAVA_HOME + FS + "bin" + FS + "jar";
    private static final String JARSIGNER = JAVA_HOME + FS + "bin" + FS + "jarsigner";
    private static final String PASSWORD = "password";
    private static final String PWDFILE = "keypass";
    private static final String POLICY1 = "SignedJarTest_1.policy";
    private static final String POLICY2 = "SignedJarTest_2.policy";
    private static final String KEYSTORE1 = "both.jks";
    private static final String KEYSTORE2 = "first.jks";
    private static final String SECPROPS = TESTSRC + FS + "java.security";

    public static void main(String args[]) throws Throwable {
        //copy PrivilegeTest.class, policy files and keystore password file into current direcotry
        Files.copy(Paths.get(TESTCLASSES, "PrivilegeTest.class"), Paths.get("PrivilegeTest.class"));
        Files.copy(Paths.get(TESTSRC, POLICY1), Paths.get(POLICY1));
        Files.copy(Paths.get(TESTSRC, POLICY2), Paths.get(POLICY2));
        Files.copy(Paths.get(TESTSRC, PWDFILE), Paths.get(PWDFILE));

        //create Jar file
        ProcessTools.executeCommand(JAR, "-cvf", "test.jar", "PrivilegeTest.class");

        //Creating first key , keystore both.jks
        ProcessTools.executeCommand(KEYTOOL,
                "-genkey",
                "-keyalg", "DSA",
                "-alias", "first",
                "-keystore", KEYSTORE1,
                "-keypass", PASSWORD,
                "-dname", "cn=First",
                "-storepass", PASSWORD
        ).shouldHaveExitValue(0);

        //Creating Second key, keystore both.jks
        ProcessTools.executeCommand(KEYTOOL,
                "-genkey",
                "-keyalg", "DSA",
                // "-storetype","JKS",
                "-alias", "second",
                "-keystore", KEYSTORE1,
                "-keypass", PASSWORD,
                "-dname", "cn=Second",
                "-storepass", PASSWORD
        ).shouldHaveExitValue(0);

        //copy both.jks to first.jks, remove second Keypair from first.jks
        Files.copy(Paths.get(KEYSTORE1), Paths.get(KEYSTORE2));
        ProcessTools.executeCommand(KEYTOOL,
                "-delete",
                "-keystore", KEYSTORE2,
                "-alias", "second",
                "-storepass", PASSWORD
        ).shouldHaveExitValue(0);

        //sign jar with first key, first.jar is only signed by first signer
        ProcessTools.executeCommand(JARSIGNER,
                "-keystore", KEYSTORE1,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", "first.jar", "test.jar",
                "first").shouldHaveExitValue(0);

        //sign jar with second key, both.jar is signed by first and second signer
        ProcessTools.executeCommand(JARSIGNER,
                "-keystore", KEYSTORE1,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-signedjar", "both.jar", "first.jar",
                "second").shouldHaveExitValue(0);

        //test case 1
        //setIO permission granted to code that was signed by first signer
        //setFactory permission granted to code that was signed by second signer
        //Keystore that contains both first and second  keypairs
        //code was singed by first signer
        //Expect AccessControlException for setFactory permission
        System.out.println("Test Case 1");
        //copy policy file into current directory
        String[] cmd = constructCMD("first.jar", POLICY1, "false", "true");
        ProcessTools.executeTestJvm(cmd).shouldHaveExitValue(0);

        //test case 2, test with both.jar
        //setIO permission granted to code that was signed by first signer
        //setFactory permission granted to code that was signed by second signer
        //Keystore that contains both first and second  keypairs
        //code was singed by first signer and second signer
        //Expect no AccessControlException
        System.out.println("Test Case 2");
        cmd = constructCMD("both.jar", POLICY1, "false", "false");
        ProcessTools.executeTestJvm(cmd).shouldHaveExitValue(0);

        //test case 3
        //setIO permission granted to code that was signed by first signer
        //setFactory permission granted to code that was signed by second signer
        //Keystore that contains only first keypairs
        //code was singed by first signer and second signer
        //Expect AccessControlException for setFactory permission
        System.out.println("Test Case 3");
        cmd = constructCMD("both.jar", POLICY2, "false", "true");
        ProcessTools.executeTestJvm(cmd).shouldHaveExitValue(0);

    }

    private static String[] constructCMD(String classpath, String policy, String arg1, String arg2) {
        String[] cmd = {
            "-classpath", classpath,
            "-Djava.security.manager",
            "-Djava.security.policy=" + policy,
            "-Djava.security.properties=" + SECPROPS,
            "PrivilegeTest",
            arg1, arg2};
        return cmd;
    }
}

class PrivilegeTest {

    private static final Permission PERM1 = new RuntimePermission("setIO");
    private static final Permission PERM2 = new RuntimePermission("setFactory");

    public static void main(String args[]) {
        boolean expectException1 = Boolean.parseBoolean(args[0]);
        boolean expectException2 = Boolean.parseBoolean(args[1]);
        test(PERM1, expectException1);
        test(PERM2, expectException2);
    }

    public static void test(Permission perm, boolean expectException) {
        boolean getException = (Boolean) AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                AccessController.checkPermission(perm);
                return (Boolean) false;
            } catch (AccessControlException ex) {
                return (Boolean) true;
            }
        });

        if (expectException ^ getException) {
            String message = "Check Permission :" + perm + "\n ExpectException = "
                    + expectException + "\n getException = " + getException;
            throw new RuntimeException(message);
        }

    }

}
