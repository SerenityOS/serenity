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
 * @bug 6868579
 * @summary RFE: jarsigner to support reading password from environment variable
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PassType {

    static OutputAnalyzer jarsignerWithEnv(String cmd) throws Throwable {
        ProcessBuilder pb = SecurityTools.getProcessBuilder(
                "jarsigner", List.of(cmd.trim().split("\\s+")));
        pb.environment().put("PASSENV", "test12");
        return ProcessTools.executeCommand(pb);
    }

    static OutputAnalyzer keytoolWithEnv(String cmd) throws Throwable {
        ProcessBuilder pb = SecurityTools.getProcessBuilder(
                "keytool", List.of(cmd.trim().split("\\s+")));
        pb.environment().put("PASSENV", "test12");
        return ProcessTools.executeCommand(pb);
    }

    public static void main(String[] args) throws Throwable {

        SecurityTools.keytool("-keystore ks -validity 300 -keyalg rsa "
                + "-alias a -dname CN=a -keyalg rsa -genkey "
                + "-storepass test12 -keypass test12")
                .shouldHaveExitValue(0);
        keytoolWithEnv("-keystore ks -validity 300 -keyalg rsa "
                + "-alias b -dname CN=b -keyalg rsa -genkey "
                + "-storepass:env PASSENV -keypass:env PASSENV")
                .shouldHaveExitValue(0);
        Files.write(Path.of("passfile"), List.of("test12"));
        SecurityTools.keytool("-keystore ks -validity 300 -keyalg rsa "
                + "-alias c -dname CN=c -keyalg rsa -genkey "
                + "-storepass:file passfile -keypass:file passfile")
                .shouldHaveExitValue(0);

        Files.write(Path.of("A"), List.of("A"));
        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("A"));

        // Sign
        SecurityTools.jarsigner("-keystore ks -storepass test12 a.jar a")
                .shouldHaveExitValue(0);
        jarsignerWithEnv("-keystore ks -storepass:env PASSENV a.jar b")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-keystore ks -storepass:file passfile a.jar c")
                .shouldHaveExitValue(0);

        // Verify
        SecurityTools.jarsigner("-keystore ks -storepass test12 "
                + "-verify -debug -strict a.jar")
                .shouldHaveExitValue(0);
        jarsignerWithEnv("-keystore ks -storepass:env PASSENV "
                + "-verify -debug -strict a.jar")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-keystore ks -storepass:file passfile "
                + "-verify -debug -strict a.jar")
                .shouldHaveExitValue(0);
    }
}
