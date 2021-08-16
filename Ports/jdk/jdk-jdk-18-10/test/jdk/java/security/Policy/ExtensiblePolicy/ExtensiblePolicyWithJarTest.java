/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import TVJar.TVPermission;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

/**
 * @test
 * @bug 8050402
 * @summary Check policy is extensible with user defined permissions
 * @library /test/lib
 * @build jdk.test.lib.util.JarUtils
 * @compile TVJar/TVPermission.java
 * @run main ExtensiblePolicyWithJarTest
 */
public class ExtensiblePolicyWithJarTest {

    public static void main(String args[]) throws Throwable {
        final String FS = File.separator;
        final String PS = File.pathSeparator;
        final String POL = "ExtensiblePolicyTest3.policy";
        final String JAVA_HOME = System.getProperty("test.jdk");
        final String KEYTOOL = JAVA_HOME + FS + "bin" + FS + "keytool";
        final String JARSIGNER = JAVA_HOME + FS + "bin" + FS + "jarsigner";
        final String KEYSTORE = "epkeystore";
        final String PASSWORD = "password";
        final String ALIAS = "duke2";
        final String CLASSPATH = System.getProperty("test.class.path", "");
        final String TESTCLASSES = System.getProperty("test.classes", "");
        final String TVPERMJAR = "tvPerm.jar";
        final String PATHTOJAR = System.getProperty("user.dir", "")
                                + FS + TVPERMJAR;

        // create jar file for TVpermission
        new File("TVJar").mkdir();
        Files.copy(Paths.get(TESTCLASSES + FS + "TVJar", "TVPermission.class"),
                Paths.get("TVJar", "TVPermission.class"));
        Files.copy(Paths.get(TESTCLASSES + FS + "TVJar",
                "TVPermissionCollection.class"),
                Paths.get("TVJar", "TVPermissionCollection.class"));
        JarUtils.createJar(TVPERMJAR, "TVJar/TVPermission.class",
                "TVJar/TVPermissionCollection.class");

        // create key pair for jar signing
        ProcessTools.executeCommand(KEYTOOL,
                "-genkey",
                "-keyalg", "DSA",
                "-alias", ALIAS,
                "-keystore", KEYSTORE,
                "-storetype", "JKS",
                "-keypass", PASSWORD,
                "-dname", "cn=Blah",
                "-storepass", PASSWORD
        ).shouldHaveExitValue(0);
        // sign jar
        ProcessTools.executeCommand(JARSIGNER,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                TVPERMJAR,
                ALIAS).shouldHaveExitValue(0);
        // add jar file to classpath
        String cp = PATHTOJAR + PS + CLASSPATH;

        // policy file grants permission signed by duke2 to watch TVChanel 5
        try {
            String[] cmd = {
            "-classpath", cp,
            "-Djava.security.manager",
            "-Djava.security.policy=" + POL,
            "ExtensiblePolicyTest_orig$TestMain"};
            ProcessTools.executeTestJvm(cmd).shouldHaveExitValue(0);
        } catch (Exception ex) {
            System.out.println("ExtensiblePolicyWithJarTest Failed");
        }

    }

    public static class TestMain {
        public static void main(String args[]) {
            TVPermission perm = new TVPermission("channel:5", "watch");
            try {
                AccessController.checkPermission(perm);
            } catch (SecurityException se) {
                throw new RuntimeException(se);
            }
        }
    }

}
