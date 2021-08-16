/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.provider.EntropySource;
import sun.security.provider.MoreDrbgParameters;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.DrbgParameters;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static java.security.DrbgParameters.Capability.*;

/**
 * The Known-output DRBG test. The test vector can be downloaded from
 * https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Algorithm-Validation-Program/documents/drbg/drbgtestvectors.zip.
 * The test is described on https://csrc.nist.gov/Projects/Cryptographic-Algorithm-Validation-Program/Random-Number-Generators.
 *
 * Manually run this test with
 *
 *   java DrbgCavp drbgtestvectors.zip
 *
 */
public class DrbgCavp {

    // the current nonce
    private static byte[] nonce;

    // A buffer to store test materials for the current call and
    // can be printed out of an error occurs.
    private static ByteArrayOutputStream bout = new ByteArrayOutputStream();

    // Save err for restoring
    private static PrintStream err = System.err;

    private static final int AES_LIMIT;

    static {
        try {
            AES_LIMIT = Cipher.getMaxAllowedKeyLength("AES");
        } catch (Exception e) {
            // should not happen
            throw new AssertionError("Cannot detect AES");
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: java DrbgCavp drbgtestvectors.zip");
            return;
        }
        File tv = new File(args[0]);

        EntropySource es = new TestEntropySource();
        System.setErr(new PrintStream(bout));

        // The testsuite is a zip file containing more zip files for different
        // working modes. Each internal zip file contains test materials for
        // different mechanisms.

        try (ZipFile zf = new ZipFile(tv)) {
            String[] modes = {"no_reseed", "pr_false", "pr_true"};
            for (String mode : modes) {
                try (ZipInputStream zis = new ZipInputStream(zf.getInputStream(
                        zf.getEntry("drbgvectors_" + mode + ".zip")))) {
                    while (true) {
                        ZipEntry ze = zis.getNextEntry();
                        if (ze == null) {
                            break;
                        }
                        String fname = ze.getName();
                        if (fname.equals("Hash_DRBG.txt")
                                || fname.equals("HMAC_DRBG.txt")
                                || fname.equals("CTR_DRBG.txt")) {
                            String algorithm
                                    = fname.substring(0, fname.length() - 4);
                            test(mode, algorithm, es, zis);
                        }
                    }
                }
            }
        } finally {
            System.setErr(err);
        }
    }

    /**
     * A special entropy source you can set entropy input at will.
     */
    private static class TestEntropySource implements EntropySource {

        private static Queue<byte[]> data = new ArrayDeque<>();

        @Override
        public byte[] getEntropy(int minEntropy, int minLength,
                                 int maxLength, boolean pr) {
            byte[] result = data.poll();
            if (result == null
                    || result.length < minLength
                    || result.length > maxLength) {
                throw new RuntimeException("Invalid entropy: " +
                        "need [" + minLength + ", " + maxLength + "], " +
                        (result == null ? "none" : "has " + result.length));
            }
            return result;
        }

        private static void setEntropy(byte[] input) {
            data.offer(input);
        }

        private static void clearEntropy() {
            data.clear();
        }
    }

    /**
     * The test.
     *
     * // Algorithm line, might contain usedf flag
     * [AES-128 use df]
     * // Ignored, use mode argument
     * [PredictionResistance = True]
     * // Ignored, just read EntropyInput
     * [EntropyInputLen = 128]
     * // Ignored, just read Nonce
     * [NonceLen = 64]
     * // Ignored, just read PersonalizationString
     * [PersonalizationStringLen = 128]
     * // Ignored, just read AdditionalInput
     * [AdditionalInputLen = 128]
     * // Used to allocate buffer for nextBytes() call
     * [ReturnedBitsLen = 512]
     *
     * // A sign we can ignore old unused entropy input
     * COUNT = 0
     *
     * // Instantiate
     * EntropyInput = 92898f...
     * Nonce = c2a4d9...
     * PersonalizationString = ea65ee...  // Enough to call getInstance()
     *
     * // Reseed
     * EntropyInputReseed = bfd503...
     * AdditionalInputReseed = 009e0b... // Enough to call reseed()
     *
     * // Generation
     * AdditionalInput = 1a40fa....  // Enough to call nextBytes() for PR off
     * EntropyInputPR = 20728a...  // Enough to call nextBytes() for PR on
     * ReturnedBits = 5a3539...  // Compare this to last nextBytes() output
     *
     * @param mode one of "no_reseed", "pr_false", "pr_true"
     * @param mech one of "Hash_DRBG", "HMAC_DRBG", "CTR_DRBG"
     * @param es our own entropy source
     * @param is test material
     */
    private static void test(String mode, String mech, EntropySource es,
            InputStream is) throws Exception {

        SecureRandom hd = null;

        // Expected output length in bits as in [ReturnedBitsLen]
        int outLen = 0;

        // DRBG algorithm as in the algorithm line
        String algorithm = null;

        // When CTR_DRBG uses a derivation function as in the algorithm line
        boolean usedf = false;

        // Additional input as in "AdditionalInput"
        byte[] additional = null;

        // Random bits generated
        byte[] output = null;

        // Prediction resistance flag, determined by mode
        boolean isPr = false;

        StringBuilder sb = new StringBuilder();

        int lineno = 0;

        System.out.println(mode + "/" + mech);

        try (Stream<String> lines =
                     new BufferedReader(new InputStreamReader(is)).lines()) {
            for (String s: (Iterable<String>) lines::iterator) {
                lineno++;
                err.print(hd == null ? '-' : '*');
                Line l = new Line(s);
                if (l.key.contains("no df") || l.key.contains("use df") ||
                        l.key.startsWith("SHA-")) {
                    sb = new StringBuilder();
                    bout.reset();
                }
                sb.append(String.format(
                        "%9s %4s %5d %s\n", mode, mech, lineno, s));
                switch (l.key) {
                    case "3KeyTDEA no df":
                    case "AES-128 no df":
                    case "AES-192 no df":
                    case "AES-256 no df":
                    case "3KeyTDEA use df":
                    case "AES-128 use df":
                    case "AES-192 use df":
                    case "AES-256 use df":
                        algorithm = l.key.split(" ")[0];
                        usedf = l.key.contains("use df");
                        break;
                    case "ReturnedBitsLen":
                        outLen = l.vint();
                        output = new byte[outLen / 8];
                        break;
                    case "EntropyInput":
                        TestEntropySource.setEntropy(l.vdata());
                        break;
                    case "Nonce":
                        nonce = l.vdata();
                        break;
                    case "COUNT":
                        // Remove unused entropy (say, when AES-256 is skipped)
                        TestEntropySource.clearEntropy();
                        break;
                    case "PersonalizationString":
                        try {
                            isPr = mode.equals("pr_true");
                            byte[] ps = null;
                            if (l.vdata().length != 0) {
                                ps = l.vdata();
                            }

                            // MoreDrbgParameters must be used because we
                            // want to set entropy input and nonce. Since
                            // it can also set mechanism, algorithm and usedf,
                            // we don't need to touch securerandom.drbg.config.
                            hd = SecureRandom.getInstance("DRBG",
                                    new MoreDrbgParameters(es, mech, algorithm,
                                            nonce, usedf,
                                            DrbgParameters.instantiation(
                                                    -1,
                                                    isPr ? PR_AND_RESEED
                                                            : RESEED_ONLY,
                                                    ps)),
                                    "SUN");
                        } catch (NoSuchAlgorithmException iae) {
                            // We don't support SHA-1 and 3KeyTDEA. AES-192 or
                            // AES-256 might not be available. This is OK.
                            if (algorithm.equals("SHA-1") ||
                                    algorithm.equals("3KeyTDEA") ||
                                    ((algorithm.equals("AES-192")
                                    || algorithm.equals("AES-256"))
                                    && AES_LIMIT == 128)) {
                                hd = null;
                            } else {
                                throw iae;
                            }
                        }
                        break;
                    case "EntropyInputReseed":
                        TestEntropySource.setEntropy(l.vdata());
                        break;
                    case "AdditionalInputReseed":
                        if (l.vdata().length == 0) {
                            additional = null;
                        } else {
                            additional = l.vdata();
                        }
                        if (hd != null) {
                            if (additional == null) {
                                hd.reseed();
                            } else {
                                hd.reseed(DrbgParameters.reseed(
                                        isPr, additional));
                            }
                        }
                        break;
                    case "EntropyInputPR":
                        if (l.vdata().length != 0) {
                            TestEntropySource.setEntropy(l.vdata());
                        }
                        if (mode.equals("pr_true")) {
                            if (hd != null) {
                                if (additional == null) {
                                    hd.nextBytes(output);
                                } else {
                                    hd.nextBytes(output,
                                            DrbgParameters.nextBytes(
                                                    -1, isPr, additional));
                                }
                            }
                        }
                        break;
                    case "AdditionalInput":
                        if (l.vdata().length == 0) {
                            additional = null;
                        } else {
                            additional = l.vdata();
                        }
                        if (!mode.equals("pr_true")) {
                            if (hd != null) {
                                if (additional == null) {
                                    hd.nextBytes(output);
                                } else {
                                    hd.nextBytes(output,
                                            DrbgParameters.nextBytes(
                                                    -1, isPr, additional));
                                }
                            }
                        }
                        break;
                    case "ReturnedBits":
                        if (hd != null) {
                            if (!Arrays.equals(output, l.vdata())) {
                                throw new Exception("\nExpected: " +
                                        l.value + "\n  Actual: " + hex(output));
                            }
                        }
                        break;
                    default:
                        // Algorithm line for Hash_DRBG and HMAC_DRBG
                        if (l.key.startsWith("SHA-")) {
                            algorithm = l.key;
                        }
                }
            }
            err.println();
        } catch (Exception e) {
            err.println();
            err.println(sb.toString());
            err.println(bout.toString());
            throw e;
        }
    }

    /**
     * Parse a line from test material.
     *
     * Brackets are removed. Key and value separated.
     */
    static class Line {

        final String key;
        final String value;

        Line(String s) {
            s = s.trim();
            if (s.length() >= 2) {
                if (s.charAt(0) == '[') {
                    s = s.substring(1, s.length() - 1);
                }
            }
            if (s.indexOf('=') < 0) {
                key = s;
                value = null;
            } else {
                key = s.substring(0, s.indexOf('=')).trim();
                value = s.substring(s.indexOf('=') + 1).trim();
            }
        }

        int vint() {
            return Integer.parseInt(value);
        }

        byte[] vdata() {
            return xeh(value);
        }
    }

    // Bytes to HEX
    private static String hex(byte[] in) {
        StringBuilder sb = new StringBuilder();
        for (byte b: in) {
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }

    // HEX to bytes
    private static byte[] xeh(String in) {
        in = in.replaceAll(" ", "");
        int len = in.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) Integer.parseInt(
                    in.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
