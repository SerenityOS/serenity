/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6316539 8136355
 * @summary Known-answer-test for TlsKeyMaterial generator
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules java.base/sun.security.internal.spec
 *          jdk.crypto.cryptoki
 * @run main/othervm TestKeyMaterial
 * @run main/othervm -Djava.security.manager=allow TestKeyMaterial sm policy
 */

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.ProviderException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import sun.security.internal.spec.TlsKeyMaterialParameterSpec;
import sun.security.internal.spec.TlsKeyMaterialSpec;

public class TestKeyMaterial extends PKCS11Test {

    private static final int PREFIX_LENGTH = "km-master:  ".length();

    public static void main(String[] args) throws Exception {
        System.out.println("NSS Version: " + getNSSVersion());
        main(new TestKeyMaterial(), args);
    }

    @Override
    public void main(Provider provider) throws Exception {
        if (provider.getService("KeyGenerator", "SunTlsKeyMaterial") == null) {
            System.out.println("Provider does not support algorithm, skipping");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get(BASE, "keymatdata.txt"))) {

            int n = 0;
            int lineNumber = 0;

            byte[] master = null;
            int major = 0;
            int minor = 0;
            byte[] clientRandom = null;
            byte[] serverRandom = null;
            String cipherAlgorithm = null;
            int keyLength = 0;
            int expandedKeyLength = 0;
            int ivLength = 0;
            int macLength = 0;
            byte[] clientCipherBytes = null;
            byte[] serverCipherBytes = null;
            byte[] clientIv = null;
            byte[] serverIv = null;
            byte[] clientMacBytes = null;
            byte[] serverMacBytes = null;

            while (true) {
                String line = reader.readLine();
                lineNumber++;
                if (line == null) {
                    break;
                }
                if (line.startsWith("km-") == false) {
                    continue;
                }
                String data = line.substring(PREFIX_LENGTH);
                if (line.startsWith("km-master:")) {
                    master = parse(data);
                } else if (line.startsWith("km-major:")) {
                    major = Integer.parseInt(data);
                } else if (line.startsWith("km-minor:")) {
                    minor = Integer.parseInt(data);
                } else if (line.startsWith("km-crandom:")) {
                    clientRandom = parse(data);
                } else if (line.startsWith("km-srandom:")) {
                    serverRandom = parse(data);
                } else if (line.startsWith("km-cipalg:")) {
                    cipherAlgorithm = data;
                } else if (line.startsWith("km-keylen:")) {
                    keyLength = Integer.parseInt(data);
                } else if (line.startsWith("km-explen:")) {
                    expandedKeyLength = Integer.parseInt(data);
                } else if (line.startsWith("km-ivlen:")) {
                    ivLength = Integer.parseInt(data);
                } else if (line.startsWith("km-maclen:")) {
                    macLength = Integer.parseInt(data);
                } else if (line.startsWith("km-ccipkey:")) {
                    clientCipherBytes = parse(data);
                } else if (line.startsWith("km-scipkey:")) {
                    serverCipherBytes = parse(data);
                } else if (line.startsWith("km-civ:")) {
                    clientIv = parse(data);
                } else if (line.startsWith("km-siv:")) {
                    serverIv = parse(data);
                } else if (line.startsWith("km-cmackey:")) {
                    clientMacBytes = parse(data);
                } else if (line.startsWith("km-smackey:")) {
                    serverMacBytes = parse(data);

                    System.out.print(".");
                    n++;

                    KeyGenerator kg =
                        KeyGenerator.getInstance("SunTlsKeyMaterial", provider);
                    SecretKey masterKey =
                        new SecretKeySpec(master, "TlsMasterSecret");
                    TlsKeyMaterialParameterSpec spec =
                        new TlsKeyMaterialParameterSpec(masterKey, major, minor,
                        clientRandom, serverRandom, cipherAlgorithm,
                        keyLength, expandedKeyLength, ivLength, macLength,
                        null, -1, -1);

                    try {
                        kg.init(spec);
                        TlsKeyMaterialSpec result =
                            (TlsKeyMaterialSpec)kg.generateKey();
                        match(lineNumber, clientCipherBytes,
                            result.getClientCipherKey(), cipherAlgorithm);
                        match(lineNumber, serverCipherBytes,
                            result.getServerCipherKey(), cipherAlgorithm);
                        match(lineNumber, clientIv, result.getClientIv(), "");
                        match(lineNumber, serverIv, result.getServerIv(), "");
                        match(lineNumber, clientMacBytes, result.getClientMacKey(), "");
                        match(lineNumber, serverMacBytes, result.getServerMacKey(), "");
                    } catch (ProviderException pe) {
                        if (provider.getName().indexOf("NSS") != -1) {
                            Throwable t = pe.getCause();
                            if (expandedKeyLength != 0
                                    && t.getMessage().indexOf(
                                            "CKR_MECHANISM_PARAM_INVALID") != -1) {
                                // NSS removed support for export-grade cipher suites in 3.28,
                                // see https://bugzilla.mozilla.org/show_bug.cgi?id=1252849
                                System.out.println("Ignore known NSS failure on CKR_MECHANISM_PARAM_INVALID");
                                continue;
                            }
                        }
                        throw pe;
                    }
               } else {
                    throw new Exception("Unknown line: " + line);
               }
            }
            if (n == 0) {
                throw new Exception("no tests");
            }
            System.out.println();
            System.out.println("OK: " + n + " tests");
        }
    }

    private static void stripParity(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            b[i] &= 0xfe;
        }
    }

    private static void match(int lineNumber, byte[] out, Object res,
            String cipherAlgorithm) throws Exception {
        if ((out == null) || (res == null)) {
            if (out != res) {
                throw new Exception("null mismatch line " + lineNumber);
            } else {
                return;
            }
        }
        byte[] b;
        if (res instanceof SecretKey) {
            b = ((SecretKey)res).getEncoded();
            if (cipherAlgorithm.equalsIgnoreCase("DES") ||
                    cipherAlgorithm.equalsIgnoreCase("DESede")) {
                // strip DES parity bits before comparision
                stripParity(out);
                stripParity(b);
            }
        } else if (res instanceof IvParameterSpec) {
            b = ((IvParameterSpec)res).getIV();
        } else {
            throw new Exception(res.getClass().getName());
        }
        if (Arrays.equals(out, b) == false) {
            System.out.println();
            System.out.println("out: " + toString(out));
            System.out.println("b:   " + toString(b));
            throw new Exception("mismatch line " + lineNumber);
        }
    }

}
