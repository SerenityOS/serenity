/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;

/*
 * @test
 * @bug 8075286
 * @summary Test the SHA algorithm OIDs in JDK.
 *          OID and algorithm transformation string should match.
 *          Both could be able to be used to generate the algorithm instance.
 * @run main TestSHAOids
 */
public class TestSHAOids {

    private static final String PROVIDER_NAME = "SUN";
    private static final byte[] INPUT = "1234567890".getBytes();

    private static final List<DataTuple> DATA = Arrays.asList(
            new DataTuple("2.16.840.1.101.3.4.2.1", "SHA-256"),
            new DataTuple("2.16.840.1.101.3.4.2.2", "SHA-384"),
            new DataTuple("2.16.840.1.101.3.4.2.3", "SHA-512"),
            new DataTuple("2.16.840.1.101.3.4.2.4", "SHA-224"));

    public static void main(String[] args) throws Exception {
        for (DataTuple dataTuple : DATA) {
            runTest(dataTuple);
            System.out.println("passed");
        }
        System.out.println("All tests passed");
    }

    private static void runTest(DataTuple dataTuple)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest mdAlgorithm = MessageDigest.getInstance(
                dataTuple.algorithm, PROVIDER_NAME);
        MessageDigest mdOid = MessageDigest.getInstance(dataTuple.oid,
                PROVIDER_NAME);

        if (mdAlgorithm == null) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance failed.%n",
                    dataTuple.algorithm));
        }

        if (mdOid == null) {
            throw new RuntimeException(
                    String.format("Test failed: OID %s getInstance failed.%n",
                            dataTuple.oid));
        }

        if (!mdAlgorithm.getAlgorithm().equals(dataTuple.algorithm)) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance doesn't "
                            + "generate expected algorithm.%n",
                    dataTuple.algorithm));
        }

        mdAlgorithm.update(INPUT);
        mdOid.update(INPUT);

        // Comparison
        if (!Arrays.equals(mdAlgorithm.digest(), mdOid.digest())) {
            throw new RuntimeException("Digest comparison failed: "
                    + "the two digests are not the same");
        }
    }

    private static class DataTuple {

        private final String oid;
        private final String algorithm;

        private DataTuple(String oid, String algorithm) {
            this.oid = oid;
            this.algorithm = algorithm;
        }
    }
}
