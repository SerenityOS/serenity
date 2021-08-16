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
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import jdk.test.lib.RandomFactory;

/**
 * @test
 * @bug 8050371 8156059
 * @summary Check md.digest(data) value whether same with digest output value
 *          with various update/digest methods.
 * @author Kevin Liu
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main TestSameValue
 */

public class TestSameValue {

    public static void main(String[] args) throws Exception {
        TestSameValue test1 = new TestSameValue();
        test1.run();
    }

    private void run() throws Exception {

        byte[] data = new byte[6706];
        MessageDigest md = null;
        // Initialize input data
        RandomFactory.getRandom().nextBytes(data);

        String[] algorithmArr = { "SHA", "Sha", "MD5", "md5", "SHA-224",
                "SHA-256", "SHA-384", "SHA-512", "SHA3-224", "SHA3-256",
                "SHA3-384", "SHA3-512" };

        for (String algorithm : algorithmArr) {
            md = MessageDigest.getInstance(algorithm);

            for (UpdateDigestMethod updateMethod : UpdateDigestMethod
                     .values()) {
                byte[] output = updateMethod.updateDigest(data, md);
                // Get the output and the "correct" one
                byte[] standard = md.digest(data);
                // Compare input and output
                if (!MessageDigest.isEqual(output, standard)) {
                    throw new RuntimeException(
                            "Test failed at algorithm/provider/numUpdate:"
                                    + algorithm + "/" + md.getProvider()
                                    + "/" + updateMethod);
                }
            }
        }

        out.println("All "
                + algorithmArr.length * UpdateDigestMethod.values().length
                + " tests Passed");
    }

    private static enum UpdateDigestMethod {

        /*
         * update the data one by one using method update(byte input) then do
         * digest (giving the output buffer, offset, and the number of bytes to
         * put in the output buffer)
         */
        UPDATE_DIGEST_BUFFER {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md)
                    throws DigestException {
                for (byte element : data) {
                    md.update(element);
                }
                byte[] output = new byte[md.getDigestLength()];
                int len = md.digest(output, 0, output.length);
                if (len != output.length) {
                    throw new RuntimeException(
                            "ERROR" + ": digest length differs!");
                }
                return output;
            }
        },

        /*
         * update the data one by one using method update(byte input) then do
         * digest
         */
        UPDATE_DIGEST {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                for (byte element : data) {
                    md.update(element);
                }
                return md.digest();
            }
        },

        /*
         * update all the data at once as a block, then do digest ( giving the
         * output buffer, offset, and the number of bytes to put in the output
         * buffer)
         */
        UPDATE_BLOCK_DIGEST_BUFFER {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md)
                    throws DigestException {
                md.update(data);
                byte[] output = new byte[md.getDigestLength()];
                int len = md.digest(output, 0, output.length);
                if (len != output.length) {
                    throw new RuntimeException(
                            "ERROR" + ": digest length differs!");
                }
                return output;
            }
        },

        // update all the data at once as a block, then do digest
        UPDATE_BLOCK_DIGEST {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                md.update(data);
                return md.digest();
            }
        },

        /*
         * update the leading bytes (length is "data.length-LASTNBYTES") at once
         * as a block, then do digest (do a final update using the left
         * LASTNBYTES bytes which is passed as a parameter for the digest
         * method, then complete the digest)
         */
        UPDATE_LEADING_BLOCK_DIGEST_REMAIN {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                byte[] mainPart = new byte[data.length - LASTNBYTES];
                for (int i = 0; i < mainPart.length; i++) {
                    mainPart[i] = data[i];
                }
                for (int j = 0; j < LASTNBYTES; j++) {
                    REMAIN[j] = data[data.length - LASTNBYTES + j];
                }
                md.update(mainPart);
                return md.digest(REMAIN);
            }
        },

        /*
         * update the data 2 bytes each time, after finishing updating, do
         * digest (giving the output buffer, offset, and the number of bytes to
         * put in the output buffer)
         */
        UPDATE_BYTES_DIGEST_BUFFER {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md)
                    throws DigestException {

                for (int i = 0; i < data.length / 2; i++) {
                    md.update(data, i * 2, 2);
                }
                byte[] output = new byte[md.getDigestLength()];
                int len = md.digest(output, 0, output.length);
                if (len != output.length) {
                    throw new RuntimeException(
                            "ERROR" + ": digest length differs!");
                }
                return output;
            }
        },

        /*
         * update the data 2 bytes each time, after finishing updating, do
         * digest
         */
        UPDATE_BYTES_DIGEST {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                for (int i = 0; i < data.length / 2; i++) {
                    md.update(data, i * 2, 2);
                }
                return md.digest();
            }
        },

        /*
         * update the data one by one using method update(byte[] input, int
         * offset, int len) for the leading bytes (length is
         * "data.length-LASTNBYTES"), then do digest (do a final update using
         * the left LASTNBYTES bytes which is passed as a parameter for digest
         * method then complete the digest)
         */
        UPDATE_BUFFER_LEADING_DIGEST_REMAIN {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                for (int i = 0; i < data.length - LASTNBYTES; i++) {
                    md.update(data, i, 1);
                }
                for (int j = 0; j < LASTNBYTES; j++) {
                    REMAIN[j] = data[data.length - LASTNBYTES + j];
                }
                return md.digest(REMAIN);
            }
        },

        /*
         * update the data one by one using method update(byte input) for the
         * leading bytes (length is "data.length-LASTNBYTES"), then do digest
         * (do a final update using the left LASTNBYTES bytes which is passed as
         * a parameter for digest method, then complete the digest)
         */
        UPDATE_LEADING_DIGEST_REMAIN {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                for (int i = 0; i < data.length - LASTNBYTES; i++) {
                    md.update(data[i]);
                }
                for (int j = 0; j < LASTNBYTES; j++) {
                    REMAIN[j] = data[data.length - LASTNBYTES + j];
                }
                return md.digest(REMAIN);
            }
        },

        /*
         * update all the data at once as a ByteBuffer, then do digest (giving
         * the output buffer, offset, and the number of bytes to put in the
         * output buffer)
         */
        UPDATE_BYTE_BUFFER_DIGEST_BUFFER {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md)
                    throws DigestException {
                md.update(ByteBuffer.wrap(data));
                byte[] output = new byte[md.getDigestLength()];
                int len = md.digest(output, 0, output.length);
                if (len != output.length) {
                    throw new RuntimeException(
                            "ERROR" + ": digest length differs!");
                }
                return output;
            }
        },

        // update all the data at once as a ByteBuffer, then do digest
        UPDATE_BYTE_BUFFER_DIGEST {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                md.update(ByteBuffer.wrap(data));
                return md.digest();
            }
        },

        /*
         * update the leading bytes (length is "data.length-LASTNBYTES") at once
         * as a ByteBuffer, then do digest (do a final update using the left
         * LASTNBYTES bytes which is passed as a parameter for the digest
         * method, then complete the digest)
         */
        UPDATE_BYTE_BUFFER_LEADING_DIGEST_REMAIN {
            @Override
            public byte[] updateDigest(byte[] data, MessageDigest md) {
                byte[] mainPart = new byte[data.length - LASTNBYTES];
                for (int i = 0; i < mainPart.length; i++) {
                    mainPart[i] = data[i];
                }
                for (int j = 0; j < LASTNBYTES; j++) {
                    REMAIN[j] = data[data.length - LASTNBYTES + j];
                }
                md.update(ByteBuffer.wrap(mainPart));
                return md.digest(REMAIN);
            }
        };

        private static final int LASTNBYTES = 5;
        private static final byte[] REMAIN = new byte[LASTNBYTES];

        public abstract byte[] updateDigest(byte[] data, MessageDigest md)
                throws DigestException;
    }
}
