/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4909889
 * @summary KeyTool accepts any input that user make as long as we can make some
 *          sense out of it, but when comes to present the info the user, it
 *          promotes a standard look.
 * @author Andrew Fan
 * @library /test/lib
 * @run main/timeout=240 StandardAlgName
 */

import jdk.test.lib.SecurityTools;

public class StandardAlgName {
    public static void main(String[] args) throws Exception {
        // CA
        SecurityTools.keytool("-genkey", "-v", "-alias", "pkcs12testCA",
                "-keyalg", "RsA", "-keysize", "2048",
                "-sigalg", "ShA1wItHRSA",
                "-dname", "cn=PKCS12 Test CA, ou = Security SQE, o = JavaSoft, c = US",
                "-validity", "3650",
                "-keypass", "storepass", "-keystore", "keystoreCA.jceks.data",
                "-storepass", "storepass", "-storetype", "jceKS")
                .shouldHaveExitValue(0)
                .shouldNotContain("RsA")
                .shouldNotContain("ShA1wItHRSA")
                .shouldContain("RSA")
                .shouldContain("SHA1withRSA");

        // Lead
        SecurityTools.keytool("-genkey", "-v", "-alias", "pkcs12testLead",
                "-keyalg", "rSA", "-keysize", "1024",
                "-sigalg", "mD5withRSA",
                "-dname", "cn=PKCS12 Test Lead, ou=Security SQE, o=JavaSoft, c=US",
                "-validity", "3650",
                "-keypass", "storepass", "-keystore", "keystoreLead.jceks.data",
                "-storepass", "storepass", "-storetype", "jCeks")
                .shouldHaveExitValue(0)
                .shouldNotContain("rSA")
                .shouldNotContain("mD5withRSA")
                .shouldContain("RSA")
                .shouldContain("MD5withRSA");

        // End User 1
        SecurityTools.keytool("-genkey", "-v", "-alias", "pkcs12testEndUser1",
                "-keyalg", "RSa", "-keysize", "1024",
                "-sigalg", "sHa1wIThRSA",
                "-dname", "cn=PKCS12 Test End User 1, ou=Security SQE, o=JavaSoft, c=US",
                "-validity", "3650",
                "-keypass", "storepass", "-keystore", "keystoreEndUser1.jceks.data",
                "-storepass", "storepass", "-storetype", "Jceks")
                .shouldHaveExitValue(0)
                .shouldNotContain("RSa")
                .shouldNotContain("sHa1wIThRSA")
                .shouldContain("RSA")
                .shouldContain("SHA1withRSA");
    }
}
