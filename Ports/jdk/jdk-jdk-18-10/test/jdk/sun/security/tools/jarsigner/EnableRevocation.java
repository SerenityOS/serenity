/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242060
 * @summary Add a test to enable revocation check in jarsigner
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Path;

public class EnableRevocation {

    static OutputAnalyzer kt(String cmd, String ks) throws Exception {
        return SecurityTools.keytool("-storepass changeit " + cmd +
                " -keystore " + ks);
    }

    static void gencert(String owner, String cmd) throws Exception {
        kt("-certreq -alias " + owner + " -file tmp.req", "ks");
        kt("-gencert -infile tmp.req -outfile tmp.cert " + cmd, "ks");
        kt("-importcert -alias " + owner + " -file tmp.cert", "ks");
    }

    public static void main(String[] args) throws Exception {

        kt("-genkeypair -keyalg rsa -alias ca -dname CN=CA -ext bc:c", "ks");
        kt("-genkeypair -keyalg rsa -alias ca1 -dname CN=CA1", "ks");
        kt("-genkeypair -keyalg rsa -alias e1 -dname CN=E1", "ks");

        gencert("ca1", "-alias ca -ext san=dns:ca1 -ext bc:c " +
                "-ext crldp=URI:http://localhost:7000/crl.pem " +
                "-ext aia=ocsp:URI:http://localhost:7200");

        gencert("e1", "-alias ca1 -ext san=dns:e1 " +
                "-ext crldp=URI:http://localhost:7000/crl.pem " +
                "-ext aia=ocsp:URI:http://localhost:7100");

        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("ks"));

        //Signing with -revCheck option
        SecurityTools.jarsigner("-keystore ks -storepass changeit " +
                "-signedjar signeda.jar " +
                "-sigalg SHA256withRSA " +
                " a.jar e1 -revCheck")
                .shouldContain("Contacting OCSP server at")
                .shouldContain("Downloading CRL from")
                .shouldHaveExitValue(0);

        kt("-exportcert -alias ca -rfc -file cacert", "ks");
        kt("-importcert -noprompt -file cacert", "caks");

        // Verifying with -revCheck option
        SecurityTools.jarsigner("-verify -certs signeda.jar " +
                "-keystore caks -storepass changeit -verbose -debug -revCheck")
                .shouldContain("Contacting OCSP server at")
                .shouldContain("Downloading CRL from")
                .shouldHaveExitValue(0);

        // Verifying with -revCheck and -strict options
        SecurityTools.jarsigner("-verify -certs signeda.jar " +
                "-keystore caks -storepass changeit " +
                "-strict -verbose -debug -revCheck")
                .shouldContain("Contacting OCSP server at")
                .shouldContain("Downloading CRL from")
                .shouldHaveExitValue(4);
    }
}
