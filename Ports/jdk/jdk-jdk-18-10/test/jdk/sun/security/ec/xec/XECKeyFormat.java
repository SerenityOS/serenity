/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8213363
 * @summary Check for correct formatting of X25519/X448 private keys
 * @library /test/lib
 * @modules java.base/sun.security.util
 * @run main XECKeyFormat
 */

import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import java.io.*;
import java.nio.file.*;
import java.math.*;
import java.util.*;

import java.util.HexFormat;

import sun.security.util.*;

public class XECKeyFormat {

    private interface Test {
        public void runTest(Provider p) throws Exception;
    }

    private static void forEachProvider(Test t, String algName)
        throws Exception {

        int tested = 0;
        for (Provider p : Security.getProviders()) {
            Provider.Service s = p.getService("KeyPairGenerator", algName);
            if (s != null) {
                t.runTest(p);
                tested++;
            }
        }
        if (tested == 0) {
            throw new RuntimeException("no service found for " + algName);
        }
    }

    private static Map<String, String> privKeys = Map.of(
        "X25519",
        "302e020100300506032b656e0422042010fdf8358b9cda51eb98d2479fb092a80639" +
        "bf31c5e7c5ba5000387fbf9c6678",
        "X448",
        "3046020100300506032b656f043a043880998f387e05852d217c1d715b177c24aa7b" +
        "f3f4c3a72223f4983597b9ab2ed4793c30d871c24388b380d80bb36d963f5c276219" +
        "b0677fed"
    );

    private static List<String> pubKeys = List.of(
        "302a300506032b656e03210019bf44096984cdfe8541bac167dc3b96c85086aa30b6" +
        "b6cb0c5c38ad703166e1"
    );

    public static void main(String[] args) throws Exception {
        privKeyTest("X25519");
        privKeyTest("X448");
        pubKeyTest();
    }

    private static void pubKeyTest() throws Exception {
        forEachProvider(XECKeyFormat::pubKeyTest, "XDH");
    }

    private static void pubKeyTest(Provider p) throws Exception {
        for (String s : pubKeys) {
            pubKeyTest(p, s);
        }
    }

    private static void pubKeyTest(Provider p, String key) throws Exception {
        // ensure that a properly-formatted key can be read
        byte[] encodedKey = HexFormat.of().parseHex(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory kf = KeyFactory.getInstance("XDH", p);
        kf.generatePublic(keySpec);
    }

    private static void privKeyTest(String algName) throws Exception {

        forEachProvider(p -> privKeyTest(algName, p), algName);
    }

    private static void privKeyTest(String algName, Provider p)
        throws Exception {

        System.out.println("Testing " + algName + " in " + p.getName());

        // ensure format produced is correct
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algName, p);
        KeyPair kp = kpg.generateKeyPair();
        PrivateKey priv = kp.getPrivate();
        checkPrivKeyFormat(priv.getEncoded());
        KeyFactory kf = KeyFactory.getInstance(algName, p);
        PKCS8EncodedKeySpec keySpec =
            kf.getKeySpec(priv, PKCS8EncodedKeySpec.class);
        checkPrivKeyFormat(keySpec.getEncoded());

        // ensure that a properly-formatted key can be read
        byte[] encodedKey = HexFormat.of().parseHex(privKeys.get(algName));
        keySpec = new PKCS8EncodedKeySpec(encodedKey);
        kf.generatePrivate(keySpec);
    }

    private static void checkPrivKeyFormat(byte[] key) throws IOException {
        // key value should be nested octet strings
        DerValue val = new DerValue(new ByteArrayInputStream(key));
        BigInteger version = val.data.getBigInteger();
        DerValue algId = val.data.getDerValue();
        byte[] keyValue = val.data.getOctetString();
        val = new DerValue(new ByteArrayInputStream(keyValue));
        if (val.tag != DerValue.tag_OctetString) {
            throw new RuntimeException("incorrect format");
        }
    }
}


