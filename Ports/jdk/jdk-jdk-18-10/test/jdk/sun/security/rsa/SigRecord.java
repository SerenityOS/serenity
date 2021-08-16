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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class SigRecord {

    static final String TEST_SRC = System.getProperty("test.src", ".");

    public static final class SigVector {
        // digest algorithm to use
        final String mdAlg;

        // message to test
        final String msg;

        // expected signature
        final String sig;

        public SigVector(String mdAlg, String msg, String sig) {
            if (mdAlg == null || mdAlg.isEmpty()) {
                throw new IllegalArgumentException("Digest algo must be specified");
            }
            if (msg == null || mdAlg.isEmpty()) {
                throw new IllegalArgumentException("Message must be specified");
            }
            if (sig == null || mdAlg.isEmpty()) {
                throw new IllegalArgumentException("Signature must be specified");
            }
            this.mdAlg = mdAlg;
            this.msg = msg;
            this.sig = sig;
        }

        @Override
        public String toString() {
            return (mdAlg + ": msg=" + msg + ": sig=" + sig);
        }
    }

    final String id;
    // RSA private key value associated with the corresponding test vectors
    final RSAPrivateKeySpec privKeySpec;

    // RSA public key value associated with the corresponding test vectors
    final RSAPublicKeySpec pubKeySpec;

    // set of test vectors
    final List<SigVector> testVectors;

    SigRecord(String mod, String pubExp, String privExp, List<SigVector> testVectors) {
        if (mod == null || mod.isEmpty()) {
            throw new IllegalArgumentException("Modulus n must be specified");
        }
        if (pubExp == null || pubExp.isEmpty()) {
            throw new IllegalArgumentException("Public Exponent e must be specified");
        }
        if (privExp == null || privExp.isEmpty()) {
            throw new IllegalArgumentException("Private Exponent d must be specified");
        }
        if (testVectors == null || (testVectors.size() == 0)) {
            throw new IllegalArgumentException("One or more test vectors must be specified");
        }

        BigInteger n = new BigInteger(1, HexFormat.of().parseHex(mod));
        BigInteger e = new BigInteger(1, HexFormat.of().parseHex(pubExp));
        BigInteger d = new BigInteger(1, HexFormat.of().parseHex(privExp));
        this.id = ("n=" + mod + ", e=" + pubExp);
        this.pubKeySpec = new RSAPublicKeySpec(n, e);
        this.privKeySpec = new RSAPrivateKeySpec(n, d);
        this.testVectors = testVectors;
    }

    /*
     * Read a data file into an ArrayList.
     * This function will exit the program if reading the file fails
     * or if the file is not in the expected format.
     */
    public static List<SigRecord> read(String filename)
            throws IOException {

        List<SigRecord> data = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(
                        TEST_SRC + File.separator + filename)))) {
            String line;
            String mod = null;
            String pubExp = null;
            String privExp = null;
            List<SigVector> testVectors = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.startsWith("n =")) {
                    mod = line.split("=")[1].trim();
                } else if (line.startsWith("e =")) {
                    pubExp = line.split("=")[1].trim();
                } else if (line.startsWith("d =")) {
                    privExp = line.split("=")[1].trim();

                    // now should start parsing for test vectors
                    String mdAlg = null;
                    String msg = null;
                    String sig = null;
                    boolean sigVectorDone = false;
                    while ((line = br.readLine()) != null) {
                        // we only care for lines starting with
                        // SHAALG, Msg, S
                        if (line.startsWith("SHAAlg =")) {
                            mdAlg = line.split(" = ")[1].trim();
                        } else if (line.startsWith("Msg =")) {
                            msg = line.split(" = ")[1].trim();
                        } else if (line.startsWith("S =")) {
                            sig = line.split(" = ")[1].trim();
                        } else if (line.startsWith("[mod")) {
                            sigVectorDone = true;
                        }

                        if ((mdAlg != null) && (msg != null) && (sig != null)) {
                            // finish off current SigVector
                            testVectors.add(new SigVector(mdAlg, msg, sig));
                            mdAlg = msg = sig = null;
                        }
                        if (sigVectorDone) {
                            break;
                        }
                    }
                    // finish off current SigRecord and clear data for next SigRecord
                    data.add(new SigRecord(mod, pubExp, privExp, testVectors));
                    mod = pubExp = privExp = null;
                    testVectors = new ArrayList<>();
                }
            }

            if (data.isEmpty()) {
                throw new RuntimeException("Nothing read from file "
                        + filename);
            }
        }
        return data;
    }

    @Override
    public String toString() {
        return (id + ", " + testVectors.size() + " test vectors");
    }
}
