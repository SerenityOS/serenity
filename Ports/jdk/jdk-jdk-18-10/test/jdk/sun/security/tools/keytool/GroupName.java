/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.security.KeyStore;
import java.security.interfaces.ECKey;

/**
 * @test
 * @bug 8213400 8214179
 * @summary Support choosing group name in keytool keypair generation
 * @library /test/lib
 */

public class GroupName {

    private static final String COMMON = "-keystore ks "
            + "-storepass changeit -keypass changeit -debug";

    public static void main(String[] args) throws Throwable {
        gen("a", "-keyalg RSA -groupname secp256r1")
                .shouldHaveExitValue(1);

        gen("b", "-keyalg EC")
                .shouldHaveExitValue(0)
                .shouldNotContain("Specifying -keysize for generating EC keys is deprecated");
        checkCurveName("b", "secp256r1");

        gen("c", "-keyalg EC -keysize 256")
                .shouldHaveExitValue(0)
                .shouldContain("Specifying -keysize for generating EC keys is deprecated")
                .shouldContain("please use \"-groupname secp256r1\" instead.");
        checkCurveName("c", "secp256r1");

        gen("d", "-keyalg EC -keysize 256 -groupname secp256r1")
                .shouldHaveExitValue(1)
                .shouldContain("Cannot specify both -groupname and -keysize");

        gen("e", "-keyalg EC -groupname secp256r1")
                .shouldHaveExitValue(0)
                .shouldNotContain("Specifying -keysize for generating EC keys is deprecated");
        checkCurveName("e", "secp256r1");

        kt("-list -v")
                .shouldHaveExitValue(0)
                .shouldContain("Subject Public Key Algorithm: 256-bit EC (secp256r1) key");
    }

    private static void checkCurveName(String a, String name)
            throws Exception {
        KeyStore ks = KeyStore.getInstance(new File("ks"), "changeit".toCharArray());
        ECKey key = (ECKey)ks.getCertificate(a).getPublicKey();
        // The following check is highly implementation dependent. In OpenJDK,
        // params.toString() should contain all alternative names and the OID.
        Asserts.assertTrue(key.getParams().toString().contains(name));
    }

    private static OutputAnalyzer kt(String cmd) throws Throwable {
        return SecurityTools.keytool(COMMON + " " + cmd);
    }

    private static OutputAnalyzer gen(String a, String extra) throws Throwable {
        return kt("-genkeypair -alias " + a + " -dname CN=" + a + " " + extra);
    }
}
