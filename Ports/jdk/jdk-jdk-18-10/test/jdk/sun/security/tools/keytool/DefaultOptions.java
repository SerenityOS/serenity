/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023197
 * @summary Pre-configured command line options for keytool and jarsigner
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DefaultOptions {

    public static void main(String[] args) throws Throwable {

        Files.write(Path.of("kt.conf"), List.of(
                "# A Pre-configured options file",
                "keytool.all = -storepass:env PASS -keypass:env PASS "
                        + "-keystore ${user.dir}/ks -debug",
                "keytool.genkey = -keyalg ec -ext bc",
                "keytool.delete = -keystore nothing"));

        // kt.conf is read
        keytool("-conf kt.conf -genkeypair -dname CN=A -alias a")
                .shouldHaveExitValue(0);
        keytool("-conf kt.conf -list -alias a -v")
                .shouldHaveExitValue(0)
                .shouldMatch("Signature algorithm name.*ECDSA")
                .shouldContain("BasicConstraints");

        // kt.conf is read, and dup multi-valued options processed as expected
        keytool("-conf kt.conf -genkeypair -dname CN=B -alias b -ext ku=ds")
                .shouldHaveExitValue(0);
        keytool("-conf kt.conf -list -alias b -v")
                .shouldHaveExitValue(0)
                .shouldContain("BasicConstraints")
                .shouldContain("DigitalSignature");

        // Single-valued option in command section override all
        keytool("-conf kt.conf -delete -alias a")
                .shouldNotHaveExitValue(0);

        // Single-valued option on command line overrides again
        keytool("-conf kt.conf -delete -alias b -keystore ks")
                .shouldHaveExitValue(0);

        // Error cases

        // File does not exist
        keytool("-conf no-such-file -help -list")
                .shouldNotHaveExitValue(0);

        // Cannot have both standard name (-genkeypair) and legacy name (-genkey)
        Files.write(Path.of("bad.conf"), List.of(
                "keytool.all = -storepass:env PASS -keypass:env PASS -keystore ks",
                "keytool.genkeypair = -keyalg rsa",
                "keytool.genkey = -keyalg ec"));

        keytool("-conf bad.conf -genkeypair -alias me -dname cn=me")
                .shouldNotHaveExitValue(0);

        // Unknown options are rejected by tool
        Files.write(Path.of("bad.conf"), List.of(
                "keytool.all=-unknown"));

        keytool("-conf bad.conf -help -list").shouldNotHaveExitValue(0);

        // System property must be present
        Files.write(Path.of("bad.conf"), List.of(
                "keytool.all = -keystore ${no.such.prop}"));

        keytool("-conf bad.conf -help -list").shouldNotHaveExitValue(0);
    }

    // Run keytool with one environment variable PASS=changeit
    static OutputAnalyzer keytool(String cmd) throws Throwable {
        ProcessBuilder pb = SecurityTools.getProcessBuilder(
                "keytool", List.of(cmd.trim().split("\\s+")));
        pb.environment().put("PASS", "changeit");
        return ProcessTools.executeCommand(pb);
    }
}
