/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8010125 8192988
 * @summary keytool should support -storepasswd for pkcs12 keystores
 * @library /test/lib
 * @build jdk.test.lib.SecurityTools
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main JKStoPKCS12
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Collections;

public class JKStoPKCS12 {

    static String srcStorePass, srcKeyPass;

    public static void main(String[] args) throws Exception {

        // Part 1: JKS keystore with same storepass and keypass
        genJKS("pass1111", "pass1111");

        // Change storepass, keypass also changes
        convert("pass2222", null);
        // You can keep storepass unchanged
        convert("pass1111", null);
        // Or change storepass and keypass both, explicitly
        convert("pass2222", "pass2222");

        // Part 2: JKS keystore with different storepass and keypass
        Files.delete(Paths.get("jks"));
        genJKS("pass1111", "pass2222");

        // Can use old keypass as new storepass so new storepass and keypass are same
        convert("pass2222", null);
        // Or specify both storepass and keypass to brand new ones
        convert("pass3333", "pass3333");
        // Or change storepass, keypass also changes. Remember to provide srckeypass
        convert("pass1111", null);
    }

    // Generate JKS keystore with srcStorePass and srcKeyPass
    static void genJKS(String storePass, String keyPass)
            throws Exception {
        srcStorePass = storePass;
        srcKeyPass = keyPass;
        kt("-genkeypair -keystore jks -storetype jks "
                    + "-alias me -dname CN=Me -keyalg rsa "
                    + "-storepass " + srcStorePass + " -keypass " + srcKeyPass)
                .shouldHaveExitValue(0);
    }

    // Convert JKS to PKCS12 with destStorePass and destKeyPass (optional)
    static void convert(String destStorePass, String destKeyPass)
            throws Exception {

        String cmd = "-importkeystore -noprompt"
                + " -srcstoretype jks -srckeystore jks"
                + " -destkeystore p12 -deststoretype pkcs12"
                + " -srcstorepass " + srcStorePass
                + " -deststorepass " + destStorePass;

        // Must import by alias (-srckeypass not available when importing all)
        if (!srcStorePass.equals(srcKeyPass)) {
            cmd += " -srcalias me";
            cmd += " -srckeypass " + srcKeyPass;
        }
        if (destKeyPass != null) {
            cmd += " -destkeypass " + destKeyPass;
        }

        kt(cmd).shouldHaveExitValue(0);

        // Confirms the storepass and keypass are all correct
        KeyStore.getInstance(new File("p12"), destStorePass.toCharArray())
                .getKey("me", destStorePass.toCharArray());

        Files.delete(Paths.get("p12"));
    }

    static OutputAnalyzer kt(String arg) throws Exception {
        return SecurityTools.keytool(arg);
    }
}
