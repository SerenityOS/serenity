/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import jdk.test.lib.RandomFactory;

/**
 * @test
 * @bug 8050371 8156059
 * @summary Check md.getDigestLength() equal digest output length with various
 *          algorithm/dataLen/(update,digest methods).
 * @author Kevin Liu
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main TestSameLength
 */

public class TestSameLength {

    public static void main(String[] args) throws Exception {
        TestSameLength test = new TestSameLength();
        test.run();
    }

    private void run() throws Exception {
        String[] algorithmArr = { "SHA", "Sha", "SHA-1", "sha-1", "SHA1",
                "sha1", "MD5", "md5", "SHA-224", "SHA-256", "SHA-384",
                "SHA-512", "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512" };
        int[] nUpdatesArr = { 0, 1, 2, 3 };
        int[] dataLenArr = { 1, 50, 2500, 125000, 6250000 };

        for (String algorithm : algorithmArr) {
            for (UpdateMethod update : UpdateMethod.values()) {
                for (int dataLen : dataLenArr) {
                    if (!runTest(algorithm, dataLen, update)) {
                        throw new RuntimeException(
                                "Test failed at algorithm/dataLen/numUpdate:"
                                        + algorithm + "/" + dataLen + "/"
                                        + update.toString());
                    }
                }
            }
        }

        out.println("All "
                + algorithmArr.length * nUpdatesArr.length * dataLenArr.length
                + " tests Passed");
    }

    private boolean runTest(String algo, long dataLen, UpdateMethod whichUpdate)
            throws Exception {
        try {
            // Do initialization
            byte[] data = new byte[(int) dataLen];
            RandomFactory.getRandom().nextBytes(data);
            MessageDigest md = MessageDigest.getInstance(algo);
            int outputLen = md.getDigestLength();

            // Perform the update using all available/possible update methods
            whichUpdate.updateDigest(data, md, dataLen);
            // Get the output
            byte[] output = md.digest();

            // Compare input and output
            return outputLen == output.length;
        } catch (NoSuchAlgorithmException nae) {
            throw nae;
        } catch (Exception ex) {
            System.err.println("Testing: " + algo + "/" + dataLen + "/"
                    + whichUpdate.toString()
                    + " failed with unexpected exception");
            ex.printStackTrace();
            throw ex;
        }
    }

    private static enum UpdateMethod {
        UPDATE_BYTE {
            @Override
            public void updateDigest(byte[] data, MessageDigest md,
                    long dataLen) {

                for (int i = 0; i < dataLen; i++) {
                    md.update(data[i]);
                }
            }
        },

        UPDATE_BUFFER {
            @Override
            public void updateDigest(byte[] data, MessageDigest md,
                    long dataLen) {

                md.update(data);
            }
        },

        UPDATE_BUFFER_LEN {
            @Override
            public void updateDigest(byte[] data, MessageDigest md,
                    long dataLen) {

                for (int i = 0; i < dataLen; i++) {
                    md.update(data, i, 1);
                }
            }
        },

        UPDATE_BYTE_BUFFER {
            @Override
            public void updateDigest(byte[] data, MessageDigest md,
                    long dataLen) {

                md.update(ByteBuffer.wrap(data));
            }
        };

        public abstract void updateDigest(byte[] data, MessageDigest md,
                long dataLen);
    }
}
