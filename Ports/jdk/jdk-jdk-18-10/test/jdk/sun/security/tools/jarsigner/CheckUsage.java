/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7004168
 * @summary jarsigner -verify checks for KeyUsage codesigning ext on all certs
 *  instead of just signing cert
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CheckUsage {

    static OutputAnalyzer keytool(String cmd) throws Exception {
        return SecurityTools.keytool("-keypass changeit -storepass changeit "
                + "-keyalg rsa " + cmd);
    }

    public static void main(String[] args) throws Exception {
        Files.write(Path.of("x"), List.of("x"));
        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("x"));

        // ################### 3 Keystores #######################

        // Keystore js.jks: including CA and Publisher
        // CA contains a non-empty KeyUsage
        keytool("-keystore js.jks -genkeypair -alias ca -dname CN=CA "
                + "-ext KU=kCS -ext bc -validity 365");
        keytool("-keystore js.jks -genkeypair -alias pub -dname CN=Publisher");

        // Publisher contains the correct KeyUsage
        keytool("-keystore js.jks -certreq -alias pub -file pub.req");
        keytool("-keystore js.jks -gencert -alias ca -ext KU=dig -validity 365 "
                + "-infile pub.req -outfile pub.cert");
        keytool("-keystore js.jks -importcert -alias pub -file pub.cert");

        // Keystore trust.jks: including CA only
        keytool("-keystore js.jks -exportcert -alias ca -file ca.cert");
        keytool("-keystore trust.jks -importcert -alias ca -noprompt -file ca.cert");

        // Keystore unrelated.jks: unrelated
        keytool("-keystore unrelated.jks -genkeypair -alias nothing "
                + "-dname CN=Nothing -validity 365");

        // ################### 4 Tests #######################

        // Test 1: Sign should be OK

        SecurityTools.jarsigner("-keystore js.jks -storepass changeit a.jar pub")
                .shouldHaveExitValue(0);

        // Test 2: Verify should be OK

        SecurityTools.jarsigner("-keystore trust.jks -storepass changeit "
                + "-strict -verify a.jar")
                .shouldHaveExitValue(0);

        // Test 3: When no keystore is specified, the error is only
        // "chain invalid"

        SecurityTools.jarsigner("-strict -verify a.jar")
                .shouldHaveExitValue(4);

        // Test 4: When unrelated keystore is specified, the error is
        // "chain invalid" and "not alias in keystore"

        SecurityTools.jarsigner("-keystore unrelated.jks -storepass changeit "
                + "-strict -verify a.jar")
                .shouldHaveExitValue(36);
    }
}
