/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Use Cipher update and doFinal with a mixture of byte[], bytebuffer,
 * and offset while verifying return values.  Also using different and
 * in-place buffers.
 *
 * in-place is not tested with different buffer types as it is not a logical
 * scenario and is complicated by getOutputSize calculations.
 */

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;

public class GCMBufferTest implements Cloneable {

    // Data type for the operation
    enum dtype { BYTE, HEAP, DIRECT };
    // Data map
    static HashMap<String, List<Data>> datamap = new HashMap<>();
    // List of enum values for order of operation
    List<dtype> ops;

    static final int AESBLOCK = 16;
    // The remaining input data length is inserted at the particular index
    // in sizes[] during execution.
    static final int REMAINDER = -1;

    String algo;
    boolean same = true;
    int[] sizes;
    boolean incremental = false;
    // In some cases the theoretical check is too complicated to verify
    boolean theoreticalCheck;
    List<Data> dataSet;
    int inOfs = 0, outOfs = 0;
    static HexFormat hex = HexFormat.of();

    static class Data {
        int id;
        SecretKey key;
        byte[] iv;
        byte[] pt;
        byte[] aad;
        byte[] ct;
        byte[] tag;

        Data(String keyalgo, int id, String key, String iv, byte[] pt, String aad,
            String ct, String tag) {
            this.id = id;
            this.key = new SecretKeySpec(HexToBytes(key), keyalgo);
            this.iv = HexToBytes(iv);
            this.pt = pt;
            this.aad = HexToBytes(aad);
            this.ct = HexToBytes(ct);
            this.tag = HexToBytes(tag);
        }

        Data(String keyalgo, int id, String key, String iv, String pt, String aad,
            String ct, String tag) {
            this(keyalgo, id, key, iv, HexToBytes(pt), aad, ct, tag);
        }

        Data(String keyalgo, int id, String key, int ptlen) {
            this.id = id;
            this.key = new SecretKeySpec(HexToBytes(key), keyalgo);
            iv = new byte[16];
            pt = new byte[ptlen];
            tag = new byte[12];
            aad = new byte[0];
            byte[] tct = null;
            try {
                SecureRandom r = new SecureRandom();
                r.nextBytes(iv);
                r.nextBytes(pt);
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                c.init(Cipher.ENCRYPT_MODE, this.key,
                    new GCMParameterSpec(tag.length * 8, this.iv));
                tct = c.doFinal(pt);
            } catch (Exception e) {
                throw new RuntimeException("Error in generating data for length " +
                    ptlen, e);
            }
            ct = new byte[ptlen];
            System.arraycopy(tct, 0, ct, 0, ct.length);
            System.arraycopy(tct, ct.length, tag, 0, tag.length);
        }

        private static final byte[] HexToBytes(String hexVal) {
            if (hexVal == null) {
                return new byte[0];
            }
            return hex.parseHex(hexVal);
        }

    }

    /**
     * Construct a test with an algorithm and a list of dtype.
     * @param algo Algorithm string
     * @param ops List of dtypes.  If only one dtype is specified, only a
     *            doFinal operation will occur.  If multiple dtypes are
     *            specified, the last is a doFinal, the others are updates.
     */
    GCMBufferTest(String algo, List<dtype> ops) {
        this.algo = algo;
        this.ops = ops;
        theoreticalCheck = true;
        dataSet = datamap.get(algo);
    }

    public GCMBufferTest clone() throws CloneNotSupportedException{
        return (GCMBufferTest)super.clone();
    }

    /**
     * Define particular data sizes to be tested.  "REMAINDER", which has a
     * value of -1, can be used to insert the remaining input text length at
     * that index during execution.
     * @param sizes Data sizes for each dtype in the list.
     */
    GCMBufferTest dataSegments(int[] sizes) {
        this.sizes = sizes;
        return this;
    }

    /**
     * Do not perform in-place operations
     */
    GCMBufferTest differentBufferOnly() {
        this.same = false;
        return this;
    }

    /**
     * Enable incrementing through each data size available.  This can only be
     * used when the List has more than one dtype entry.
     */
    GCMBufferTest incrementalSegments() {
        this.incremental = true;
        return this;
    }

    /**
     * Specify a particular test dataset.
     *
     * @param id id value for the test data to used in this test.
     */
    GCMBufferTest dataSet(int id) throws Exception {
        for (Data d : datamap.get(algo)) {
            if (d.id == id) {
                dataSet = List.of(d);
                return this;
            }
        }
        throw new Exception("Unable to find dataSet id = " + id);
    }

    /**
     * Set both input and output offsets to the same offset
     * @param offset value for inOfs and outOfs
     * @return
     */
    GCMBufferTest offset(int offset) {
        this.inOfs = offset;
        this.outOfs = offset;
        return this;
    }

    /**
     * Set the input offset
     * @param offset value for input offset
     * @return
     */
    GCMBufferTest inOfs(int offset) {
        this.inOfs = offset;
        return this;
    }

    /**
     * Set the output offset
     * @param offset value for output offset
     * @return
     */
    GCMBufferTest outOfs(int offset) {
        this.outOfs = offset;
        return this;
    }

    /**
     * Reverse recursive loop that starts at the end-1 index, going to 0, in
     * the size array to calculate all the possible sizes.
     * It returns the remaining data size not used in the loop.  This remainder
     * is used for the end index which is the doFinal op.
     */
    int inc(int index, int max, int total) {
        if (sizes[index] == max - total) {
            sizes[index + 1]++;
            total++;
            sizes[index] = 0;
        } else if (index == 0) {
            sizes[index]++;
        }

        total += sizes[index];
        if (index > 0) {
            return inc(index - 1, max, total);
        }
        return total;
    }

    // Call recursive loop and take returned remainder value for last index
    boolean incrementSizes(int max) {
        sizes[ops.size() - 1] = max - inc(ops.size() - 2, max, 0);
        if (sizes[ops.size() - 2] == max) {
            // We are at the end, exit test loop
            return false;
        }
        return true;
    }

    void test() throws Exception {
        int i = 1;
        System.err.println("Algo: " + algo + " \tOps: " + ops.toString());
        for (Data data : dataSet) {

            // If incrementalSegments is enabled, run through that test only
            if (incremental) {
                if (ops.size() < 2) {
                    throw new Exception("To do incrementalSegments you must" +
                        "have more that 1 dtype in the list");
                }
                sizes = new int[ops.size()];

                while (incrementSizes(data.pt.length)) {
                    System.err.print("Encrypt:  Data Index: " + i + " \tSizes[ ");
                    for (int v : sizes) {
                        System.err.print(v + " ");
                    }
                    System.err.println("]");
                    encrypt(data);
                }
                Arrays.fill(sizes, 0);

                while (incrementSizes(data.ct.length + data.tag.length)) {
                    System.err.print("Decrypt:  Data Index: " + i + " \tSizes[ ");
                    for (int v : sizes) {
                        System.err.print(v + " ");
                    }
                    System.err.println("]");
                    decrypt(data);
                }

            } else {
                // Default test of 0 and 2 offset doing in place and different
                // i/o buffers
                System.err.println("Encrypt:  Data Index: " + i);
                encrypt(data);

                System.err.println("Decrypt:  Data Index: " + i);
                decrypt(data);
            }
            i++;
        }
    }

    // Setup data for encryption
    void encrypt(Data data) throws Exception {
        byte[] input, output;

        input = data.pt;
        output = new byte[data.ct.length + data.tag.length];
        System.arraycopy(data.ct, 0, output, 0, data.ct.length);
        System.arraycopy(data.tag, 0, output, data.ct.length,
            data.tag.length);

        // Test different input/output buffers
        System.err.println("\tinput len: " + input.length + "  inOfs " +
            inOfs + "  outOfs " + outOfs + "  in/out buffer: different");
        crypto(true, data, input, output);

        // Test with in-place buffers
        if (same) {
            System.err.println("\tinput len: " + input.length + "  inOfs " +
            inOfs + "  outOfs " + outOfs + "  in/out buffer: in-place");
            cryptoSameBuffer(true, data, input, output);
        }
    }

    // Setup data for decryption
    void decrypt(Data data) throws Exception {
        byte[] input, output;

        input = new byte[data.ct.length + data.tag.length];
        System.arraycopy(data.ct, 0, input, 0, data.ct.length);
        System.arraycopy(data.tag, 0, input, data.ct.length, data.tag.length);
        output = data.pt;

        // Test different input/output buffers
        System.err.println("\tinput len: " + input.length + "  inOfs " +
            inOfs + "  outOfs " + outOfs + "  in/out buffer: different");
        crypto(false, data, input, output);

        // Test with in-place buffers
        if (same) {
            System.err.println("\tinput len: " + input.length + "  inOfs " +
            inOfs + "  outOfs " + outOfs + "  in-place: same");
            cryptoSameBuffer(false, data, input, output);
        }
    }

    /**
     * Perform cipher operation using different input and output buffers.
     *   This method allows mixing of data types (byte, heap, direct).
     */
     void crypto(boolean encrypt, Data d, byte[] input, byte[] output)
         throws Exception {
        byte[] pt = new byte[input.length + inOfs];
        System.arraycopy(input, 0, pt, inOfs, input.length);
         byte[] expectedOut = new byte[output.length + outOfs];
         System.arraycopy(output, 0, expectedOut, outOfs, output.length);
        int plen = input.length / ops.size(); // partial input length
        int theoreticallen;// expected output length
        int dataoffset = 0; // offset of unconsumed data in pt
        int index = 0; // index of which op we are on
        int rlen; // result length
        int pbuflen = 0; // plen remaining in the GCM internal buffers

        Cipher cipher = Cipher.getInstance(algo);
        cipher.init((encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE),
            d.key, new GCMParameterSpec(d.tag.length * 8, d.iv));
        cipher.updateAAD(d.aad);

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ba.write(new byte[outOfs], 0, outOfs);
        for (dtype v : ops) {
            if (index < ops.size() - 1) {
                if (sizes != null && input.length > 0) {
                    if (sizes[index] == -1) {
                        plen = input.length - dataoffset;
                    } else {
                        if (sizes[index] > input.length) {
                            plen = input.length;
                        } else {
                            plen = sizes[index];
                        }
                    }
                }

                int olen = cipher.getOutputSize(plen) + outOfs;

                /*
                 * The theoretical limit is the length of the data sent to
                 * update() + any data might be setting in CipherCore or GCM
                 * internal buffers % the block size.
                 */
                theoreticallen = (plen + pbuflen) - ((plen + pbuflen) % AESBLOCK);

                // Update operations
                switch (v) {
                    case BYTE -> {
                        byte[] out = new byte[olen];
                        rlen = cipher.update(pt, dataoffset + inOfs, plen, out,
                            outOfs);
                        ba.write(out, outOfs, rlen);
                    }
                    case HEAP -> {
                        ByteBuffer b = ByteBuffer.allocate(plen + outOfs);
                        b.position(outOfs);
                        b.put(pt, dataoffset + inOfs, plen);
                        b.flip();
                        b.position(outOfs);
                        ByteBuffer out = ByteBuffer.allocate(olen);
                        out.position(outOfs);
                        rlen = cipher.update(b, out);
                        ba.write(out.array(), outOfs, rlen);
                    }
                    case DIRECT -> {
                        ByteBuffer b = ByteBuffer.allocateDirect(plen + outOfs);
                        b.position(outOfs);
                        b.put(pt, dataoffset + inOfs, plen);
                        b.flip();
                        b.position(outOfs);
                        ByteBuffer out = ByteBuffer.allocateDirect(olen);
                        out.position(outOfs);
                        rlen = cipher.update(b, out);
                        byte[] o = new byte[rlen];
                        out.flip();
                        out.position(outOfs);
                        out.get(o, 0, rlen);
                        ba.write(o);
                    }
                    default -> throw new Exception("Unknown op: " + v.name());
                }

                if (theoreticalCheck) {
                    pbuflen += plen - rlen;
                    if (encrypt && rlen != theoreticallen) {
                        throw new Exception("Wrong update return len (" +
                            v.name() + "):  " + "rlen=" + rlen +
                            ", expected output len=" + theoreticallen);
                    }
                }

                dataoffset += plen;
                index++;

            } else {
                // doFinal operation
                plen = input.length - dataoffset;

                int olen = cipher.getOutputSize(plen) + outOfs;
                switch (v) {
                    case BYTE -> {
                        byte[] out = new byte[olen];
                        rlen = cipher.doFinal(pt, dataoffset + inOfs,
                            plen, out, outOfs);
                        ba.write(out, outOfs, rlen);
                    }
                    case HEAP -> {
                        ByteBuffer b = ByteBuffer.allocate(plen + inOfs);
                        b.limit(b.capacity());
                        b.position(inOfs);
                        b.put(pt, dataoffset + inOfs, plen);
                        b.flip();
                        b.position(inOfs);
                        ByteBuffer out = ByteBuffer.allocate(olen);
                        out.limit(out.capacity());
                        out.position(outOfs);
                        rlen = cipher.doFinal(b, out);
                        ba.write(out.array(), outOfs, rlen);
                    }
                    case DIRECT -> {
                        ByteBuffer b = ByteBuffer.allocateDirect(plen + inOfs);
                        b.limit(b.capacity());
                        b.position(inOfs);
                        b.put(pt, dataoffset + inOfs, plen);
                        b.flip();
                        b.position(inOfs);
                        ByteBuffer out = ByteBuffer.allocateDirect(olen);
                        out.limit(out.capacity());
                        out.position(outOfs);
                        rlen = cipher.doFinal(b, out);
                        byte[] o = new byte[rlen];
                        out.flip();
                        out.position(outOfs);
                        out.get(o, 0, rlen);
                        ba.write(o);
                    }
                    default -> throw new Exception("Unknown op: " + v.name());
                }

                if (theoreticalCheck && rlen != olen - outOfs) {
                    throw new Exception("Wrong doFinal return len (" +
                        v.name() + "):  " + "rlen=" + rlen +
                        ", expected output len=" + (olen - outOfs));
                }

                // Verify results
                byte[] ctresult = ba.toByteArray();
                if (ctresult.length != expectedOut.length ||
                    Arrays.compare(ctresult, expectedOut) != 0) {
                    String s = "Ciphertext mismatch (" + v.name() +
                        "):\nresult   (len=" + ctresult.length + "): " +
                        hex.formatHex(ctresult) +
                        "\nexpected (len=" + output.length + "): " +
                        hex.formatHex(output);
                    System.err.println(s);
                    throw new Exception(s);

                }
            }
        }
    }

    /**
     * Perform cipher operation using in-place buffers.  This method does not
     * allow mixing of data types (byte, heap, direct).
     *
     * Mixing data types makes no sense for in-place operations and would
     * greatly complicate the test code.
     */
    void cryptoSameBuffer(boolean encrypt, Data d, byte[] input, byte[] output) throws Exception {

        byte[] data, out;
        if (encrypt) {
            data = new byte[output.length + Math.max(inOfs, outOfs)];
        } else {
            data = new byte[input.length + Math.max(inOfs, outOfs)];
        }

        ByteBuffer bbin = null, bbout = null;
        System.arraycopy(input, 0, data, inOfs, input.length);
        byte[] expectedOut = new byte[output.length + outOfs];
        System.arraycopy(output, 0, expectedOut, outOfs, output.length);
        int plen = input.length / ops.size(); // partial input length
        int theorticallen = plen - (plen % AESBLOCK); // output length
        int dataoffset = 0;
        int index = 0;
        int rlen = 0; // result length
        int len = 0;

        Cipher cipher = Cipher.getInstance(algo);
        cipher.init((encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE),
            d.key, new GCMParameterSpec(d.tag.length * 8, d.iv));
        cipher.updateAAD(d.aad);

        // Prepare data
        switch (ops.get(0)) {
            case HEAP -> {
                bbin = ByteBuffer.wrap(data);
                bbin.limit(input.length + inOfs);
                bbout = bbin.duplicate();
            }
            case DIRECT -> {
                bbin = ByteBuffer.allocateDirect(data.length);
                bbout = bbin.duplicate();
                bbin.put(data, 0, input.length + inOfs);
                bbin.flip();
            }
        }

        // Set data limits for bytebuffers
        if (bbin != null) {
            bbin.position(inOfs);
            bbout.limit(output.length + outOfs);
            bbout.position(outOfs);
        }

        // Iterate through each operation
        for (dtype v : ops) {
            if (index < ops.size() - 1) {
                switch (v) {
                    case BYTE -> {
                        rlen = cipher.update(data, dataoffset + inOfs, plen,
                            data, len + outOfs);
                    }
                    case HEAP, DIRECT -> {
                        theorticallen = bbin.remaining() -
                            (bbin.remaining() % AESBLOCK);
                        rlen = cipher.update(bbin, bbout);
                    }
                    default -> throw new Exception("Unknown op: " + v.name());
                }

                // Check that the theoretical return value matches the actual.
                if (theoreticalCheck && encrypt && rlen != theorticallen) {
                    throw new Exception("Wrong update return len (" +
                        v.name() + "):  " + "rlen=" + rlen +
                        ", expected output len=" + theorticallen);
                }

                dataoffset += plen;
                len += rlen;
                index++;

            } else {
                // Run doFinal op
                plen = input.length - dataoffset;

                switch (v) {
                    case BYTE -> {
                        rlen = cipher.doFinal(data, dataoffset + inOfs,
                            plen, data, len + outOfs);
                        out = Arrays.copyOfRange(data, 0,len + rlen + outOfs);
                    }
                    case HEAP, DIRECT -> {
                        rlen = cipher.doFinal(bbin, bbout);
                        bbout.flip();
                        out = new byte[bbout.remaining()];
                        bbout.get(out);
                    }
                    default -> throw new Exception("Unknown op: " + v.name());
                }
                len += rlen;

                // Verify results
                if (len != output.length ||
                    Arrays.compare(out, 0, len, expectedOut, 0,
                        output.length) != 0) {
                    String s = "Ciphertext mismatch (" + v.name() +
                        "):\nresult (len=" + len + "):\n" +
                        hex.formatHex(out) +
                        "\nexpected (len=" + output.length + "):\n" +
                        hex.formatHex(output);
                    System.err.println(s);
                    throw new Exception(s);
                }
            }
        }
    }
    static void offsetTests(GCMBufferTest t) throws Exception {
        t.clone().offset(2).test();
        t.clone().inOfs(2).test();
        // Test not designed for overlap situations
        t.clone().outOfs(2).differentBufferOnly().test();
    }

    public static void main(String args[]) throws Exception {
        GCMBufferTest t;

        initTest();

        // Test single byte array
        new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.BYTE)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.BYTE)));
        // Test update-doFinal with byte arrays
        new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.BYTE, dtype.BYTE)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.BYTE, dtype.BYTE)));
        // Test update-update-doFinal with byte arrays
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.BYTE, dtype.BYTE, dtype.BYTE)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.BYTE, dtype.BYTE, dtype.BYTE)));

        // Test single heap bytebuffer
        new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.HEAP)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.HEAP)));
        // Test update-doFinal with heap bytebuffer
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.HEAP, dtype.HEAP)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.HEAP, dtype.HEAP)));
        // Test update-update-doFinal with heap bytebuffer
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.HEAP, dtype.HEAP, dtype.HEAP)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.HEAP, dtype.HEAP, dtype.HEAP)));

        // Test single direct bytebuffer
        new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.DIRECT)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding", List.of(dtype.DIRECT)));
        // Test update-doFinal with direct bytebuffer
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.DIRECT)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.DIRECT)));
        // Test update-update-doFinal with direct bytebuffer
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.DIRECT, dtype.DIRECT)).test();
        offsetTests(new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.DIRECT, dtype.DIRECT)));

        // Test update-update-doFinal with byte arrays and preset data sizes
        t = new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.BYTE, dtype.BYTE, dtype.BYTE)).dataSegments(
            new int[] { 1, 1, GCMBufferTest.REMAINDER});
        t.clone().test();
        offsetTests(t.clone());

        // Test update-doFinal with a byte array and a direct bytebuffer
        t = new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.BYTE, dtype.DIRECT)).differentBufferOnly();
        t.clone().test();
        offsetTests(t.clone());
        // Test update-doFinal with a byte array and heap and direct bytebuffer
        t = new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.BYTE, dtype.HEAP, dtype.DIRECT)).differentBufferOnly();
        t.clone().test();
        offsetTests(t.clone());

        // Test update-doFinal with a direct bytebuffer and a byte array.
        t = new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.BYTE)).differentBufferOnly();
        t.clone().test();
        offsetTests(t.clone());

        // Test update-doFinal with a direct bytebuffer and a byte array with
        // preset data sizes.
        t = new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.BYTE)).differentBufferOnly().
            dataSegments(new int[] { 20, GCMBufferTest.REMAINDER });
        t.clone().test();
        offsetTests(t.clone());
        // Test update-update-doFinal with a direct and heap bytebuffer and a
        // byte array with preset data sizes.
        t = new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.BYTE, dtype.HEAP)).
            differentBufferOnly().dataSet(5).
            dataSegments(new int[] { 5000, 1000, GCMBufferTest.REMAINDER });
        t.clone().test();
        offsetTests(t.clone());

        // Test update-update-doFinal with byte arrays, incrementing through
        // every data size combination for the Data set 0
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.BYTE, dtype.BYTE, dtype.BYTE)).incrementalSegments().
            dataSet(0).test();
        // Test update-update-doFinal with direct bytebuffers, incrementing through
        // every data size combination for the Data set 0
        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.DIRECT, dtype.DIRECT)).
            incrementalSegments().dataSet(0).test();

        new GCMBufferTest("AES/GCM/NoPadding",
            List.of(dtype.DIRECT, dtype.DIRECT, dtype.DIRECT)).
            dataSegments(new int[] { 49, 0, 2 }).dataSet(0).test();
    }

    // Test data
    static void initTest() {
        datamap.put("AES/GCM/NoPadding", List.of(
            // GCM KAT
            new Data("AES", 0,
            "141f1ce91989b07e7eb6ae1dbd81ea5e",
                "49451da24bd6074509d3cebc2c0394c972e6934b45a1d91f3ce1d3ca69e19" +
                "4aa1958a7c21b6f21d530ce6d2cc5256a3f846b6f9d2f38df0102c4791e5" +
                "7df038f6e69085646007df999751e248e06c47245f4cd3b8004585a7470d" +
                "ee1690e9d2d63169a58d243c0b57b3e5b4a481a3e4e8c60007094ef3adea" +
                "2e8f05dd3a1396f",
            "d384305af2388699aa302f510913fed0f2cb63ba42efa8c5c9de2922a2ec" +
                "2fe87719dadf1eb0aef212b51e74c9c5b934104a43",
            "630cf18a91cc5a6481ac9eefd65c24b1a3c93396bd7294d6b8ba3239517" +
                "27666c947a21894a079ef061ee159c05beeb4",
            "f4c34e5fbe74c0297313268296cd561d59ccc95bbfcdfcdc71b0097dbd83" +
                "240446b28dc088abd42b0fc687f208190ff24c0548",
            "dbb93bbb56d0439cd09f620a57687f5d"),
            // GCM KAT
            new Data("AES", 1, "11754cd72aec309bf52f7687212e8957",
                "3c819d9a9bed087615030b65",
                (String)null, null, null,
                "250327c674aaf477aef2675748cf6971"),
            // GCM KAT
            new Data("AES", 2, "272f16edb81a7abbea887357a58c1917",
                "794ec588176c703d3d2a7a07",
                (String)null, null, null,
                "b6e6f197168f5049aeda32dafbdaeb"),
            // zero'd test data
            new Data("AES", 3, "272f16edb81a7abbea887357a58c1917",
                "794ec588176c703d3d2a7a07", new byte[256], null,
                "15b461672153270e8ba1e6789f7641c5411f3e642abda731b6086f535c216457" +
                "e87305bc59a1ff1f7e1e0bbdf302b75549b136606c67d7e5f71277aeca4bc670" +
                "07a98f78e0cfa002ed183e62f07893ad31fe67aad1bb37e15b957a14d145f14f" +
                "7483d041f2c3612ad5033155984470bdfc64d18df73c2745d92f28461bb09832" +
                "33524811321ba87d213692825815dd13f528dba601a3c319cac6be9b48686c23" +
                "a0ce23d5062916ea8827bbb243f585e446131489e951354c8ab24661f625c02e" +
                "15536c5bb602244e98993ff745f3e523399b2059f0e062d8933fad2366e7e147" +
                "510a931282bb0e3f635efe7bf05b1dd715f95f5858261b00735224256b6b3e80",
                "08b3593840d4ed005f5234ae062a5c"),
            // Random test data
            new Data("AES", 4, "272f16edb81a7abbea887357a58c1917",
                "794ec588176c703d3d2a7a07",
                new byte[2075], null,
                "15b461672153270e8ba1e6789f7641c5411f3e642abda731b6086f535c216457" +
                "e87305bc59a1ff1f7e1e0bbdf302b75549b136606c67d7e5f71277aeca4bc670" +
                "07a98f78e0cfa002ed183e62f07893ad31fe67aad1bb37e15b957a14d145f14f" +
                "7483d041f2c3612ad5033155984470bdfc64d18df73c2745d92f28461bb09832" +
                "33524811321ba87d213692825815dd13f528dba601a3c319cac6be9b48686c23" +
                "a0ce23d5062916ea8827bbb243f585e446131489e951354c8ab24661f625c02e" +
                "15536c5bb602244e98993ff745f3e523399b2059f0e062d8933fad2366e7e147" +
                "510a931282bb0e3f635efe7bf05b1dd715f95f5858261b00735224256b6b3e80" +
                "7364cb53ff6d4e88f928cf67ac70da127718a8a35542efbae9dd7567c818a074" +
                "9a0c74bd69014639f59768bc55056d1166ea5523e8c66f9d78d980beb8f0d83b" +
                "a9e2c5544b94dc3a1a4b6f0f95f897b010150e89ebcacf0daee3c2793d6501a0" +
                "b58b411de273dee987e8e8cf8bb29ef2e7f655b46b55fabf64c6a4295e0d080b" +
                "6a570ace90eb0fe0f5b5d878bdd90eddaa1150e4d5a6505b350aac814fe99615" +
                "317ecd0516a464c7904011ef5922409c0d65b1e43b69d7c3293a8f7d3e9fbee9" +
                "eb91ec0007a7d6f72e64deb675d459c5ba07dcfd58d08e6820b100465e6e04f0" +
                "663e310584a00d36d23699c1bffc6afa094c75184fc7cde7ad35909c0f49f2f3" +
                "fe1e6d745ab628d74ea56b757047de57ce18b4b3c71e8af31a6fac16189cb0a3" +
                "a97a1bea447042ce382fcf726560476d759c24d5c735525ea26a332c2094408e" +
                "671c7deb81d5505bbfd178f866a6f3a011b3cfdbe089b4957a790688028dfdf7" +
                "9a096b3853f9d0d6d3feef230c7f5f46ffbf7486ebdaca5804dc5bf9d202415e" +
                "e0d67b365c2f92a17ea740807e4f0b198b42b54f15faa9dff2c7c35d2cf8d72e" +
                "b8f8b18875a2e7b5c43d1e0aa5139c461e8153c7f632895aa46ffe2b134e6a0d" +
                "dfbf6a336e709adfe951bd52c4dfc7b07a15fb3888fc35b7e758922f87a104c4" +
                "563c5c7839cfe5a7edbdb97264a7c4ebc90367b10cbe09dbf2390767ad7afaa8" +
                "8fb46b39d3f55f216d2104e5cf040bf3d39b758bea28e2dbce576c808d17a8eb" +
                "e2fd183ef42a774e39119dff1f539efeb6ad15d889dfcb0d54d0d4d4cc03c8d9" +
                "aa6c9ebd157f5e7170183298d6a30ada8792dcf793d931e2a1eafccbc63c11c0" +
                "c5c5ed60837f30017d693ccb294df392a8066a0594a56954aea7b78a16e9a11f" +
                "4a8bc2104070a7319f5fab0d2c4ccad8ec5cd8f47c839179bfd54a7bf225d502" +
                "cd0a318752fe763e8c09eb88fa57fc5399ad1f797d0595c7b8afdd23f13603e9" +
                "6802192bb51433b7723f4e512bd4f799feb94b458e7f9792f5f9bd6733828f70" +
                "a6b7ffbbc0bb7575021f081ec2a0d37fecd7cda2daec9a3a9d9dfe1c8034cead" +
                "e4b56b581cc82bd5b74b2b30817967d9da33850336f171a4c68e2438e03f4b11" +
                "96da92f01b3b7aeab795180ccf40a4b090b1175a1fc0b67c95f93105c3aef00e" +
                "13d76cc402539192274fee703730cd0d1c5635257719cc96cacdbad00c6255e2" +
                "bd40c775b43ad09599e84f2c3205d75a6661ca3f151183be284b354ce21457d1" +
                "3ba65b9b2cdb81874bd14469c2008b3ddec78f7225ecc710cc70de7912ca6a6d" +
                "348168322ab59fdafcf5c833bfa0ad4046f4b6da90e9f263db7079af592eda07" +
                "5bf16c6b1a8346da9c292a48bf660860a4fc89eaef40bc132779938eca294569" +
                "787c740af2b5a8de7f5e10ac750d1e3d0ef3ed168ba408a676e10b8a20bd4be8" +
                "3e8336b45e54481726d73e1bd19f165a98e242aca0d8387f2dd22d02d74e23db" +
                "4cef9a523587413e0a44d7e3260019a34d3a6b38426ae9fa4655be338d721970" +
                "cb9fe76c073f26f9303093a033022cd2c62b2790bce633ba9026a1c93b6535f1" +
                "1882bf5880e511b9e1b0b7d8f23a993aae5fd275faac3a5b4ccaf7c06b0b266a" +
                "ee970a1e3a4cd7a41094f516960630534e692545b25a347c30e3f328bba4825f" +
                "ed754e5525d846131ecba7ca120a6aeabc7bab9f59c890c80b7e31f9bc741591" +
                "55d292433ce9558e104102f2cc63ee267c1c8333e841522707ea6d595cb802b9" +
                "61697da77bbc4cb404ea62570ab335ebffa2023730732ac5ddba1c3dbb5be408" +
                "3c50aea462c1ffa166d7cc3db4b742b747e81b452db2363e91374dee8c6b40f0" +
                "e7fbf50e60eaf5cc5649f6bb553aae772c185026ceb052af088c545330a1ffbf" +
                "50615b8c7247c6cd386afd7440654f4e15bcfae0c45442ec814fe88433a9d616" +
                "ee6cc3f163f0d3d325526d05f25d3b37ad5eeb3ca77248ad86c9042b16c65554" +
                "aebb6ad3e17b981492b13f42c5a5dc088e991da303e5a273fdbb8601aece4267" +
                "47b01f6cb972e6da1743a0d7866cf206e95f23c6f8e337c901b9cd34a9a1fbbe" +
                "1694f2c26b00dfa4d02c0d54540163e798fbdc9c25f30d6406f5b4c13f7ed619" +
                "34e350f4059c13aa5e973307a9e3058917cda96fdd082e9c629ccfb2a9f98d12" +
                "5c6e4703a7b0f348f5cdeb63cef2133d1c6c1a087591e0a2bca29d09c6565e66" +
                "e91042f83b0e74e60a5d57562c23e2fbcd6599c29d7c19e47cf625c2ce24bb8a" +
                "13f8e54041498437eec2cedd1e3d8e57a051baa962c0a62d70264d99c5ee716d" +
                "5c8b9078db08c8b2c5613f464198a7aff43f76c5b4612b46a4f1cd2a494386c5" +
                "7fd28f3d199f0ba8d8e39116cc7db16ce6188205ee49a9dce3d4fa32ea394919" +
                "f6e91ef58b84d00b99596b4306c2d9f432d917bb4ac73384c42ae12adb4920d8" +
                "c33a816febcb299dcddf3ec7a8eb6e04cdc90891c6e145bd9fc5f41dc4061a46" +
                "9feba38545b64ec8203f386ceef52785619e991d274ae80af7e54af535e0b011" +
                "5effdf847472992875e09398457604d04e0bb965db692c0cdcf11a",
                "687cc09c89298491deb51061d709af"),
            // Randomly generated data at the time of execution.
            new Data("AES", 5, "11754cd72aec309bf52f7687212e8957", 12345)
            )
        );
    }
}
