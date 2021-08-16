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

import jdk.test.lib.SecurityTools;
import sun.security.util.KnownOIDs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jdk.test.lib.security.DerUtils.*;
import static sun.security.util.KnownOIDs.*;

/*
 * @test
 * @bug 8076190 8242151 8153005 8266293
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.util
 * @summary Checks the preferences order of pkcs12 params, whether it's
 *          a system property or a security property, whether the name has
 *          "pkcs12" or "PKCS12", whether the legacy property is set.
 */
public class ParamsPreferences {

    public static final void main(String[] args) throws Exception {
        int c = 0;

        // default
        test(c++,
                Map.of(),
                Map.of(),
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                SHA_256, 10000);

        // legacy settings
        test(c++,
                Map.of("keystore.pkcs12.legacy", ""),
                Map.of(),
                PBEWithSHA1AndRC2_40, 50000,
                PBEWithSHA1AndDESede, 50000,
                SHA_1, 100000);

        // legacy override everything else
        test(c++,
                Map.of("keystore.pkcs12.legacy", "",
                        "keystore.pkcs12.certProtectionAlgorithm", "PBEWithHmacSHA256AndAES_128",
                        "keystore.pkcs12.certPbeIterationCount", 3000,
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithHmacSHA256AndAES_128",
                        "keystore.pkcs12.keyPbeIterationCount", 4000,
                        "keystore.pkcs12.macAlgorithm", "HmacPBESHA384",
                        "keystore.pkcs12.macIterationCount", 2000),
                Map.of(),
                PBEWithSHA1AndRC2_40, 50000,
                PBEWithSHA1AndDESede, 50000,
                SHA_1, 100000);

        // password-less with system property
        test(c++,
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "NONE",
                        "keystore.pkcs12.macAlgorithm", "NONE"),
                Map.of(),
                null,
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                null);

        // password-less with security property
        test(c++,
                Map.of(),
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "NONE",
                        "keystore.pkcs12.macAlgorithm", "NONE"),
                null,
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                null);

        // back to with storepass by overriding security property with system property
        test(c++,
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "PBEWithSHA1AndDESede",
                        "keystore.pkcs12.macAlgorithm", "HmacPBESHA256"),
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "NONE",
                        "keystore.pkcs12.macAlgorithm", "NONE"),
                PBEWithSHA1AndDESede, 10000,
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                SHA_256, 10000);

        // back to with storepass by using "" to force hardcoded default
        test(c++,
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "",
                        "keystore.pkcs12.keyProtectionAlgorithm", "",
                        "keystore.pkcs12.macAlgorithm", ""),
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "NONE",
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40",
                        "keystore.pkcs12.macAlgorithm", "NONE"),
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                SHA_256, 10000);

        // change everything with system property
        test(c++,
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "PBEWithSHA1AndDESede",
                        "keystore.pkcs12.certPbeIterationCount", 3000,
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40",
                        "keystore.pkcs12.keyPbeIterationCount", 4000,
                        "keystore.pkcs12.macAlgorithm", "HmacPBESHA256",
                        "keystore.pkcs12.macIterationCount", 2000),
                Map.of(),
                PBEWithSHA1AndDESede, 3000,
                PBEWithSHA1AndRC2_40, 4000,
                SHA_256, 2000);

        // change everything with security property
        test(c++,
                Map.of(),
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "PBEWithSHA1AndDESede",
                        "keystore.pkcs12.certPbeIterationCount", 3000,
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40",
                        "keystore.pkcs12.keyPbeIterationCount", 4000,
                        "keystore.pkcs12.macAlgorithm", "HmacPBESHA256",
                        "keystore.pkcs12.macIterationCount", 2000),
                PBEWithSHA1AndDESede, 3000,
                PBEWithSHA1AndRC2_40, 4000,
                SHA_256, 2000);

        // override security property with system property
        test(c++,
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "PBEWithSHA1AndDESede",
                        "keystore.pkcs12.certPbeIterationCount", 13000,
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40",
                        "keystore.pkcs12.keyPbeIterationCount", 14000,
                        "keystore.pkcs12.macAlgorithm", "HmacPBESHA256",
                        "keystore.pkcs12.macIterationCount", 12000),
                Map.of("keystore.pkcs12.certProtectionAlgorithm", "PBEWithSHA1AndRC2_40",
                        "keystore.pkcs12.certPbeIterationCount", 3000,
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndDESede",
                        "keystore.pkcs12.keyPbeIterationCount", 4000,
                        "keystore.pkcs12.macAlgorithm", "HmacPBESHA1",
                        "keystore.pkcs12.macIterationCount", 2000),
                PBEWithSHA1AndDESede, 13000,
                PBEWithSHA1AndRC2_40, 14000,
                SHA_256, 12000);

        // check keyProtectionAlgorithm old behavior. Preferences of
        // 4 different settings.

        test(c++,
                Map.of(),
                Map.of("keystore.PKCS12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_128"),
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                PBEWithSHA1AndRC2_128, 10000,
                SHA_256, 10000);
        test(c++,
                Map.of(),
                Map.of("keystore.PKCS12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_128",
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40"),
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                PBEWithSHA1AndRC2_40, 10000,
                SHA_256, 10000);
        test(c++,
                Map.of("keystore.PKCS12.keyProtectionAlgorithm", "PBEWithSHA1AndRC4_128"),
                Map.of("keystore.PKCS12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_128",
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40"),
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                PBEWithSHA1AndRC4_128, 10000,
                SHA_256, 10000);
        test(c++,
                Map.of("keystore.PKCS12.keyProtectionAlgorithm", "PBEWithSHA1AndRC4_128",
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC4_40"),
                Map.of("keystore.PKCS12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_128",
                        "keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndRC2_40"),
                PBES2, HmacSHA256, AES_256$CBC$NoPadding, 10000,
                PBEWithSHA1AndRC4_40, 10000,
                SHA_256, 10000);

        // 8266293
        test(c++,
                Map.of("keystore.pkcs12.keyProtectionAlgorithm", "PBEWithMD5AndDES",
                        "keystore.pkcs12.certProtectionAlgorithm", "PBEWithMD5AndDES"),
                Map.of(),
                PBEWithMD5AndDES, 10000,
                PBEWithMD5AndDES, 10000,
                SHA_256, 10000);
    }

    /**
     * Run once.
     *
     * @param sysProps system properties
     * @param secProps security properties
     * @param args     an array expected certPbeAlg (sub algs), certPbeIC,
     *                 keyPbeAlg (sub algs), keyPbeIc, macAlg, macIC.
     */
    static void test(int n, Map<String, ?> sysProps,
                     Map<String, ?> secProps,
                     Object... args) throws Exception {

        String cmd = "-keystore ks" + n + " -genkeypair -keyalg EC "
                + "-alias a -dname CN=A -storepass changeit "
                + "-J-Djava.security.properties=" + n + ".conf";

        for (var p : sysProps.entrySet()) {
            cmd += " -J-D" + p.getKey() + "=" + p.getValue();
        }

        List<String> jsConf = new ArrayList<>();
        for (var p : secProps.entrySet()) {
            jsConf.add(p.getKey() + "=" + p.getValue());
        }
        Files.write(Path.of(n + ".conf"), jsConf);
        System.out.println("--------- test starts ----------");
        System.out.println(jsConf);
        SecurityTools.keytool(cmd).shouldHaveExitValue(0);

        int i = 0;
        byte[] data = Files.readAllBytes(Path.of("ks" + n));

        // cert pbe alg + ic
        KnownOIDs certAlg = (KnownOIDs)args[i++];
        if (certAlg == null) {
            checkAlg(data, "110c10", Data);
        } else {
            checkAlg(data, "110c10", EncryptedData);
            checkAlg(data, "110c110110", certAlg);
            if (certAlg == PBES2) {
                checkAlg(data, "110c11011100", PBKDF2WithHmacSHA1);
                checkAlg(data, "110c1101110130", (KnownOIDs)args[i++]);
                checkAlg(data, "110c11011110", (KnownOIDs)args[i++]);
                checkInt(data, "110c110111011", (int) args[i++]);
            } else {
                checkInt(data, "110c1101111", (int) args[i++]);
            }
        }

        // key pbe alg + ic
        KnownOIDs keyAlg = (KnownOIDs)args[i++];
        checkAlg(data, "110c010c01000", keyAlg);
        if (keyAlg == PBES2) {
            checkAlg(data, "110c010c0100100", PBKDF2WithHmacSHA1);
            checkAlg(data, "110c010c010010130", (KnownOIDs)args[i++]);
            checkAlg(data, "110c010c0100110", (KnownOIDs)args[i++]);
            checkInt(data, "110c010c01001011", (int) args[i++]);
        } else {
            checkInt(data, "110c010c010011", (int) args[i++]);
        }

        // mac alg + ic
        KnownOIDs macAlg = (KnownOIDs)args[i++];
        if (macAlg == null) {
            shouldNotExist(data, "2");
        } else {
            checkAlg(data, "2000", macAlg);
            checkInt(data, "22", (int) args[i++]);
        }
    }
}
