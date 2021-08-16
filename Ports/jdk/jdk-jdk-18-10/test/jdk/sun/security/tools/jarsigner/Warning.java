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

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @test
 * @bug 8024302 8026037 8130132
 * @summary warnings, errors and -strict
 * @library /lib/testlibrary /test/lib
 * @build jdk.test.lib.util.JarUtils
 * @run main Warning
 */
public class Warning {

    public static void main(String[] args) throws Throwable {

        Files.deleteIfExists(Paths.get("ks"));

        newCert("ca", "-validity 365000", "-ext bc:c");

        recreateJar();

        newCert("a");
        run("jarsigner", "a.jar a")
                .shouldContain("is self-signed");
        run("jarsigner", "a.jar a -strict")
                .shouldContain("is self-signed")
                .shouldHaveExitValue(4);
        // Trusted entry can be self-signed without a warning
        run("jarsigner", "-verify a.jar")
                .shouldNotContain("is self-signed")
                .shouldNotContain("not signed by alias in this keystore");
        run("keytool", "-delete -alias a");
        // otherwise a warning will be shown
        run("jarsigner", "-verify a.jar")
                .shouldContain("is self-signed")
                .shouldContain("not signed by alias in this keystore");

        recreateJar();

        newCert("b");
        issueCert("b");
        run("jarsigner", "a.jar b")
                .shouldNotContain("is self-signed");
        run("jarsigner", "-verify a.jar")
                .shouldNotContain("is self-signed");

        run("jarsigner", "a.jar b -digestalg MD5")
                .shouldContain("-digestalg option is considered a security risk and is disabled.");
        run("jarsigner", "a.jar b -digestalg MD5 -strict")
                .shouldHaveExitValue(4)
                .shouldContain("-digestalg option is considered a security risk and is disabled.");
        run("jarsigner", "a.jar b -sigalg MD5withRSA")
                .shouldContain("-sigalg option is considered a security risk and is disabled.");

        issueCert("b", "-sigalg MD5withRSA");
        run("jarsigner", "a.jar b")
                .shouldMatch("chain is invalid. Reason:.*MD5withRSA");

        recreateJar();

        newCert("c", "-keysize 512");
        issueCert("c");
        run("jarsigner", "a.jar c")
                .shouldContain("chain is invalid. " +
                        "Reason: Algorithm constraints check failed");

        recreateJar();

        newCert("s1"); issueCert("s1", "-startdate 2000/01/01 -validity 36525");
        run("jarsigner", "a.jar s1")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp").shouldContain("2100-01-01")
                .shouldNotContain("with signer errors");
        run("jarsigner", "a.jar s1 -strict")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp").shouldContain("2100-01-01")
                .shouldNotContain("with signer errors");
        run("jarsigner", "a.jar s1 -verify")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp").shouldContain("2100-01-01")
                .shouldNotContain("with signer errors");
        run("jarsigner", "a.jar s1 -verify -strict")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp").shouldContain("2100-01-01")
                .shouldNotContain("with signer errors");

        recreateJar();

        newCert("s2"); issueCert("s2", "-validity 100");
        run("jarsigner", "a.jar s2")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp")
                .shouldContain("will expire")
                .shouldNotContain("with signer errors");
        run("jarsigner", "a.jar s2 -strict")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp")
                .shouldContain("will expire")
                .shouldNotContain("with signer errors");
        run("jarsigner", "a.jar s2 -verify")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp")
                .shouldContain("will expire")
                .shouldNotContain("with signer errors");
        run("jarsigner", "a.jar s2 -verify -strict")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("Error:")
                .shouldContain("timestamp")
                .shouldContain("will expire")
                .shouldNotContain("with signer errors");

        recreateJar();

        newCert("s3"); issueCert("s3", "-startdate -200d -validity 100");
        run("jarsigner", "a.jar s3")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldContain("has expired")
                .shouldNotContain("with signer errors")
                .shouldNotContain("Error:");
        run("jarsigner", "a.jar s3 -strict")
                .shouldHaveExitValue(4)
                .shouldContain("with signer errors")
                .shouldMatch("(?s).*Error:.*has expired.*Warning:.*");
        run("jarsigner", "a.jar s3 -verify")
                .shouldHaveExitValue(0)
                .shouldContain("Warning:")
                .shouldNotContain("with signer errors")
                .shouldNotContain("Error:");
        run("jarsigner", "a.jar s3 -verify -strict")
                .shouldHaveExitValue(4)
                .shouldContain("with signer errors")
                .shouldMatch("(?s).*Error:.*has expired.*Warning:.*");
    }

    // Creates a new jar without signature
    static void recreateJar() throws Exception {
        JarUtils.createJar("a.jar", "ks");
    }

    // Creates a self-signed cert for alias with zero or more -genkey options
    static void newCert(String alias, String... more) throws Exception {
        String args = "-genkeypair -alias " + alias + " -dname CN=" + alias;
        for (String s: more) {
            args += " " + s;
        }
        run("keytool", args).shouldHaveExitValue(0);
    }

    // Asks ca to issue a cert to alias with zero or more -gencert options
    static void issueCert(String alias, String...more) throws Exception {
        String req = run("keytool", "-certreq -alias " + alias)
                .shouldHaveExitValue(0).getStdout();
        String args = "-gencert -alias ca -rfc";
        for (String s: more) {
            args += " " + s;
        }
        String cert = run("keytool", args, req)
                .shouldHaveExitValue(0).getStdout();
        run("keytool", "-import -alias " + alias, cert).shouldHaveExitValue(0);
    }

    // Runs a java tool with command line arguments
    static OutputAnalyzer run(String command, String args)
            throws Exception {
        return run(command, args, null);
    }

    // Runs a java tool with command line arguments and an optional input block
    static OutputAnalyzer run(String command, String args, String input)
            throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK(command);
        launcher.addVMArg("-Duser.language=en").addVMArg("-Duser.country=US");
        switch (command) {
            case "keytool":
                for (String s: new String[] {
                        "-keystore", "ks", "-storepass", "changeit",
                        "-storetype", "jks",
                        "-keypass", "changeit", "-keyalg", "rsa", "-debug"}) {
                    launcher.addToolArg(s);
                }
                break;
            case "jarsigner":
                for (String s: new String[] {
                        "-keystore", "ks", "-storepass", "changeit",
                        "-storetype", "jks"}) {
                    launcher.addToolArg(s);
                }
                break;
        }
        for (String arg: args.split(" ")) {
            launcher.addToolArg(arg);
        }
        String[] cmd = launcher.getCommand();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        OutputAnalyzer out = ProcessTools.executeProcess(pb, input);
        System.out.println("======================");
        System.out.println(Arrays.toString(cmd));
        String msg = " stdout: [" + out.getStdout() + "];\n"
                + " stderr: [" + out.getStderr() + "]\n"
                + " exitValue = " + out.getExitValue() + "\n";
        System.out.println(msg);
        return out;
    }
}

