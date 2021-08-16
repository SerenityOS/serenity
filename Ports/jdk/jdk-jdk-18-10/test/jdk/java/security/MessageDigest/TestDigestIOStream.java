/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.RandomFactory;
import static java.lang.System.out;

/**
 * @test
 * @bug 8050370 8156059
 * @summary MessageDigest tests with DigestIOStream
 * @author Kevin Liu
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main/timeout=180 TestDigestIOStream
 */

enum ReadModel {
    READ, BUFFER_READ, MIX_READ
}

public class TestDigestIOStream {

    private static final int[] DATA_LEN_ARRAY = { 1, 50, 2500, 125000,
            6250000 };
    private static final String[] ALGORITHM_ARRAY = { "MD2", "MD5", "SHA1",
            "SHA-224", "SHA-256", "SHA-384", "SHA-512", "SHA3-224", "SHA3-256",
            "SHA3-384", "SHA3-512" };

    private static byte[] data;

    private static MessageDigest md = null;

    public static void main(String argv[]) throws Exception {
        TestDigestIOStream test = new TestDigestIOStream();
        test.run();
    }

    public void run() throws Exception {
        for (String algorithm : ALGORITHM_ARRAY) {
            md = MessageDigest.getInstance(algorithm);
            for (int length : DATA_LEN_ARRAY) {

                Random rdm = RandomFactory.getRandom();
                data = new byte[length];
                rdm.nextBytes(data);

                if (!testMDChange(algorithm, length)) {
                    throw new RuntimeException("testMDChange failed at:"
                            + algorithm + "/" + length);
                }
                if (!testMDShare(algorithm, length)) {
                    throw new RuntimeException("testMDShare failed at:"
                            + algorithm + "/" + length);
                }
                for (ReadModel readModel : ReadModel.values()) {
                    // test Digest function when digest switch on
                    if (!testDigestOnOff(algorithm, readModel, true,
                            length)) {
                        throw new RuntimeException(
                                "testDigestOn failed at:" + algorithm + "/"
                                        + length + "/" + readModel);
                    }
                    // test Digest function when digest switch off
                    if (!testDigestOnOff(algorithm, readModel, false,
                            length)) {
                        throw new RuntimeException(
                                "testDigestOff failed at:" + algorithm + "/"
                                        + length + "/" + readModel);
                    }
                }
            }
        }
        int testNumber = ALGORITHM_ARRAY.length * ReadModel.values().length
                * DATA_LEN_ARRAY.length * 2
                + ALGORITHM_ARRAY.length * DATA_LEN_ARRAY.length * 2;
        out.println("All " + testNumber + " Tests Passed");
    }

    /**
     * Test DigestInputStream and DigestOutputStream digest function when digest
     * set on and off
     *
     * @param algo
     *            Message Digest algorithm
     * @param readModel
     *            which read method used(READ, BUFFER_READ, MIX_READ)
     * @param on
     *            digest switch(on and off)
     * @param dataLength
     *            plain test data length.
     * @exception Exception
     *                throw unexpected exception
     */
    public boolean testDigestOnOff(String algo, ReadModel readModel, boolean on,
            int dataLength) throws Exception {

        // Generate the DigestInputStream/DigestOutputStream object
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DigestInputStream dis = new DigestInputStream(bais,
                        MessageDigest.getInstance(algo));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DigestOutputStream dos = new DigestOutputStream(baos,
                        MessageDigest.getInstance(algo));
                ByteArrayOutputStream baOut = new ByteArrayOutputStream();) {

            // Perform the update using all available/possible update methods
            int k = 0;
            byte[] buffer = new byte[5];
            boolean enDigest = true;
            // Make sure the digest function is on (default)
            dis.on(enDigest);
            dos.on(enDigest);

            switch (readModel) {
            case READ: // use only read()
                while ((k = dis.read()) != -1) {
                    if (on) {
                        dos.write(k);
                    } else {
                        dos.write(k);
                        if (enDigest) {
                            baOut.write(k);
                        }
                        enDigest = !enDigest;
                        dos.on(enDigest);
                        dis.on(enDigest);
                    }
                }
                break;
            case BUFFER_READ: // use only read(byte[], int, int)
                while ((k = dis.read(buffer, 0, buffer.length)) != -1) {
                    if (on) {
                        dos.write(buffer, 0, k);
                    } else {
                        dos.write(buffer, 0, k);
                        if (enDigest) {
                            baOut.write(buffer, 0, k);
                        }
                        enDigest = !enDigest;
                        dis.on(enDigest);
                        dos.on(enDigest);
                    }
                }
                break;
            case MIX_READ: // use both read() and read(byte[], int, int)
                while ((k = dis.read()) != -1) {
                    if (on) {
                        dos.write(k);
                        if ((k = dis.read(buffer, 0, buffer.length)) != -1) {
                            dos.write(buffer, 0, k);
                        }
                    } else {
                        dos.write(k);
                        if (enDigest) {
                            baOut.write(k);
                        }
                        enDigest = !enDigest;
                        dis.on(enDigest);
                        dos.on(enDigest);
                        if ((k = dis.read(buffer, 0, buffer.length)) != -1) {
                            dos.write(buffer, 0, k);
                            if (enDigest) {
                                baOut.write(buffer, 0, k);
                            }
                            enDigest = !enDigest;
                            dis.on(enDigest);
                            dos.on(enDigest);
                        }
                    }
                }
                break;
            default:
                out.println("ERROR: Invalid read/write combination choice!");
                return false;
            }

            // Get the output and the "correct" digest values
            byte[] output1 = dis.getMessageDigest().digest();
            byte[] output2 = dos.getMessageDigest().digest();
            byte[] standard;
            if (on) {
                standard = md.digest(data);
            } else {
                byte[] dataDigested = baOut.toByteArray();
                standard = md.digest(dataDigested);
            }

            // Compare the output byte array value to the input data
            if (!MessageDigest.isEqual(data, baos.toByteArray())) {
                out.println("ERROR of " + readModel
                        + ": output and input data unexpectedly changed");
                return false;
            }
            // Compare generated digest values
            if (!MessageDigest.isEqual(output1, standard)
                    || !MessageDigest.isEqual(output2, standard)) {
                out.println("ERROR" + readModel
                        + ": generated digest data unexpectedly changed");
                return false;
            }

            return true;
        } catch (Exception ex) {
            out.println("testDigestOnOff failed at:" + algo + "/" + readModel
                    + "/" + dataLength + " with unexpected exception");
            throw ex;
        }
    }

    /**
     * Test DigestInputStream and DigestOutputStream digest function when Swap
     * the message digest engines between DigestIn/OutputStream
     *
     * @param algo
     *            Message Digest algorithm
     * @param dataLength
     *            plain test data length.
     * @exception Exception
     *                throw unexpected exception
     */
    public boolean testMDChange(String algo, int dataLength) throws Exception {
        // Generate the DigestInputStream/DigestOutputStream object
        MessageDigest mdIn = MessageDigest.getInstance(algo);
        MessageDigest mdOut = MessageDigest.getInstance(algo);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DigestInputStream dis = new DigestInputStream(bais, mdIn);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DigestOutputStream dos = new DigestOutputStream(baos, mdOut);) {

            // Perform the update using all available/possible update methods
            int k = 0;
            byte[] buffer = new byte[10];

            // use both read() and read(byte[], int, int)
            while ((k = dis.read()) != -1) {
                dos.write(k);
                if ((k = dis.read(buffer, 0, buffer.length)) != -1) {
                    dos.write(buffer, 0, k);
                }

                // Swap the message digest engines between
                // DigestIn/OutputStream objects
                dis.setMessageDigest(mdOut);
                dos.setMessageDigest(mdIn);
                mdIn = dis.getMessageDigest();
                mdOut = dos.getMessageDigest();
            }

            // Get the output and the "correct" digest values
            byte[] output1 = mdIn.digest();
            byte[] output2 = mdOut.digest();
            byte[] standard = md.digest(data);

            // Compare generated digest values
            return MessageDigest.isEqual(output1, standard)
                    && MessageDigest.isEqual(output2, standard);
        } catch (Exception ex) {
            out.println("testMDChange failed at:" + algo + "/" + dataLength
                    + " with unexpected exception");
            throw ex;
        }
    }

    /**
     * Test DigestInputStream and DigestOutputStream digest function when use
     * same message digest object.
     *
     * @param algo
     *            Message Digest algorithm
     * @param dataLength
     *            plain test data length.
     * @exception Exception
     *                throw unexpected exception
     */
    public boolean testMDShare(String algo, int dataLength) throws Exception {
        MessageDigest mdCommon = MessageDigest.getInstance(algo);
        // Generate the DigestInputStream/DigestOutputStream object
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DigestInputStream dis = new DigestInputStream(bais, mdCommon);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DigestOutputStream dos = new DigestOutputStream(baos,
                        mdCommon);) {

            // Perform the update using all available/possible update methods
            int k = 0;
            byte[] buffer = new byte[10];

            // use both read() and read(byte[], int, int)
            while (k < data.length) {
                int len = dis.read(buffer, 0, buffer.length);
                if (len != -1) {
                    k += len;
                    if (k < data.length) {
                        dos.write(data[k]);
                        k++;
                        dis.skip(1);
                    }
                }
            }

            // Get the output and the "correct" digest values
            byte[] output = mdCommon.digest();
            byte[] standard = md.digest(data);

            // Compare generated digest values
            return MessageDigest.isEqual(output, standard);
        } catch (Exception ex) {
            out.println("TestMDShare failed at:" + algo + "/" + dataLength
                    + " with unexpected exception");
            throw ex;
        }
    }
}
