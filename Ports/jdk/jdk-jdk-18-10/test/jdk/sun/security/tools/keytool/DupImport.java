/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6968542
 * @summary keytool -importcert cannot deal with duplicate certs
 * @modules java.base/sun.security.tools.keytool
 * @compile -XDignore.symbol.file DupImport.java
 * @run main DupImport pkcs12
 * @run main DupImport jks
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DupImport {

    static String storeType = null;

    public static void main(String[] args) throws Exception {

        storeType = args[0];
        Files.deleteIfExists(Paths.get("dup.ks"));

        // Create chain: root -> int -> me
        run("-genkeypair -keyalg DSA -alias me -dname CN=Me");
        run("-genkeypair -keyalg DSA -alias int -dname CN=Int");
        run("-genkeypair -keyalg DSA -alias root -dname CN=Root");

        run("-certreq -alias int -file int.req");
        run("-gencert -infile int.req -alias root -rfc -outfile int.resp");
        run("-importcert -file int.resp -alias int");

        run("-certreq -alias me -file me.req");
        run("-gencert -infile me.req -alias int -rfc -outfile me.resp");
        run("-importcert -file me.resp -alias me");

        // Export certs
        run("-exportcert -alias me -file me -rfc");
        run("-exportcert -alias int -file int -rfc");
        run("-exportcert -alias root -file root -rfc");

        // test 1: just the 3 certs
        test("me", "int", "root");

        // test 2: 3 chains (without root) concatenated
        test("me", "int", "int", "root");

        // test 3: 3 full chains concatenated
        test("me", "int", "root", "int", "root", "root");

        // test 4: a mess
        test("root", "me", "int", "int", "me", "me", "root", "int");
    }

    // Run keytool command with common options
    static void run(String s) throws Exception {
        sun.security.tools.keytool.Main.main((
                "-keystore dup.ks -storepass changeit -keypass changeit "
                        + "-storetype " + storeType + " -debug "
                        + s).split(" "));
    }

    // Test "cat files... | keytool -import"
    static void test(String... files) throws Exception {

        System.out.println("Testing " + Arrays.toString(files));

        List<String> all = new ArrayList<>();
        for (String file : files) {
            all.addAll(Files.readAllLines(Paths.get(file)));
        }
        Files.write(Paths.get("reply"), all);

        run("-importcert -file reply -alias me");
        KeyStore ks = KeyStore.getInstance(
                new File("dup.ks"), "changeit".toCharArray());
        Certificate[] chain = ks.getCertificateChain("me");
        if (chain.length != 3) {
            throw new Exception("Length is " + chain.length);
        }

        checkName(chain[0], "CN=Me");
        checkName(chain[1], "CN=Int");
        checkName(chain[2], "CN=Root");
    }

    // Check if c's dname is expected
    static void checkName(Certificate c, String expected) throws Exception {
        X509Certificate x = (X509Certificate)c;
        String name = x.getSubjectX500Principal().toString();
        if (!expected.equals(name)) {
            throw new Exception("Expected " + expected + ", but " + name);
        }
    }
}
