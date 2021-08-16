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
 * @bug 8044755
 * @summary Add a test for algorithm constraints check in jarsigner
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Path;

public class WeakSize {

    static OutputAnalyzer kt(String cmd) throws Exception {
        // The sigalg used is MD2withRSA, which is obsolete.
        return SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa -sigalg MD2withRSA " + cmd);
    }

    static void gencert(String owner, String cmd) throws Exception {
        kt("-certreq -alias " + owner + " -file tmp.req");
        kt("-gencert -infile tmp.req -outfile tmp.cert " + cmd);
        kt("-import -alias " + owner + " -file tmp.cert");
    }

    public static void main(String[] args) throws Exception {

        kt("-genkeypair -alias ca -dname CN=CA -ext bc");
        kt("-genkeypair -alias signer -dname CN=Signer");
        gencert("signer", "-alias ca -ext ku=dS -rfc");

        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("ks"));

        // We always trust a TrustedCertificateEntry
        SecurityTools.jarsigner("-keystore ks -storepass changeit "
                + "-strict -debug a.jar ca")
                .shouldNotContain("chain is invalid");

        // An end-entity cert must follow algorithm constraints
        SecurityTools.jarsigner("-keystore ks -storepass changeit "
                + "-strict -debug a.jar signer")
                .shouldContain("chain is invalid");
    }
}
