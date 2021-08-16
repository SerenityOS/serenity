/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6584033
 * @summary Stackoverflow error with security manager, signed jars and debug.
 * @library /test/lib
 * @build TimeZoneDatePermissionCheck
 * @run main TimeZoneDatePermissionCheckRun
 */

import java.io.File;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

public class TimeZoneDatePermissionCheckRun {
    private static String storePath = Utils.TEST_CLASSES + File.separator
            + "timezonedatetest.store";
    private static String jarPath = Utils.TEST_CLASSES + File.separator
            + "timezonedatetest.jar";

    public static void main(String[] args) throws Throwable {
        try {
            //create a test keystore and dummy cert. Note that we use the COMPILEJAVA
            //as this test is a TimeZone test, it doesn't test keytool
            runJavaCmd("keytool",
                    List.of("-genkeypair", "-alias", "testcert", "-keystore",
                            storePath, "-storepass", "testpass", "-validity",
                            "360", "-keyalg", "rsa", "-dname",
                            "cn=Mark Wildebeest, ou=FreeSoft, o=Red Hat, c=NL",
                            "-keypass", "testpass"));

            //create a jar file to sign with the test class in it.
            runJavaCmd("jar", List.of("cf", jarPath, "-C", Utils.TEST_CLASSES,
                    "TimeZoneDatePermissionCheck.class"));

            //sign it
            runJavaCmd("jarsigner",
                    List.of("-keystore", storePath, "-storepass", "testpass",
                            jarPath, "testcert"));

            //run it with the security manager on, plus accesscontroller debugging
            //will go into infinite recursion trying to get enough permissions for
            //printing Date of failing certificate unless fix is applied.
            JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
            launcher.addToolArg("-Djava.security.manager")
                    .addToolArg("-Djava.security.debug=access,failure,policy")
                    .addToolArg("-ea")
                    .addToolArg("-esa")
                    .addToolArg("-cp")
                    .addToolArg(jarPath)
                    .addToolArg("TimeZoneDatePermissionCheck");

            int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                    .getExitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Unexpected exit code: " + exitCode);
            }
        } finally {
            //clean up the test files
            File storeFile = new File(storePath);
            if (storeFile.exists()) {
                storeFile.delete();
            }
            File jarFile = new File(jarPath);
            if (jarFile.exists()) {
                jarFile.delete();
            }
        }
    }

    private static void runJavaCmd(String cmd, List<String> javaParam)
            throws Throwable{
        JDKToolLauncher launcher = JDKToolLauncher.create(cmd);
        for (String para: javaParam) {
            launcher.addToolArg(para);
        }

        System.out.println(launcher.getCommand());
        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Unexpected exit code: " + exitCode);
        }
    }
}
