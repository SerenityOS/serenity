/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.provider;

import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandomParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public class HashDrbg extends AbstractHashDrbg {

    private static final byte[] ZERO = new byte[1];
    private static final byte[] ONE = new byte[]{1};

    private MessageDigest digest;

    private byte[] v;
    private byte[] c;

    public HashDrbg(SecureRandomParameters params) {
        mechName = "Hash_DRBG";
        configure(params);
    }

    /**
     * This call, used by the constructors, instantiates the digest.
     */
    @Override
    protected void initEngine() {
        try {
            /*
             * Use the local SUN implementation to avoid native
             * performance overhead.
             */
            digest = MessageDigest.getInstance(algorithm, "SUN");
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            // Fallback to any available.
            try {
                digest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException exc) {
                throw new InternalError(
                    "internal error: " + algorithm + " not available.", exc);
            }
        }
    }

    private byte[] hashDf(int requested, List<byte[]> inputs) {
        return hashDf(digest, outLen, requested, inputs);
    }

    /**
     * A hash-based derivation function defined in NIST SP 800-90Ar1 10.3.1.
     * The function is used inside Hash_DRBG, and can also be used as an
     * approved conditioning function as described in 800-90B 6.4.2.2.
     *
     * Note: In each current call, requested is seedLen, therefore small,
     * no need to worry about overflow.
     *
     * @param digest a {@code MessageDigest} object in reset state
     * @param outLen {@link MessageDigest#getDigestLength} of {@code digest}
     * @param requested requested output length, in bytes
     * @param inputs input data
     * @return the condensed/expanded output
     */
    public static byte[] hashDf(MessageDigest digest, int outLen,
                                int requested, List<byte[]> inputs) {
        // 1. temp = the Null string.
        // 2. len = upper_int(no_of_bits_to_return / outLen)
        int len = (requested + outLen - 1) / outLen;
        byte[] temp = new byte[len * outLen];
        // 3. counter = 0x01
        int counter = 1;

        // 4. For i = 1 to len do
        for (int i=0; i<len; i++) {
            // 4.1 temp = temp
            //      || Hash (counter || no_of_bits_to_return || input_string).
            digest.update((byte) counter);
            digest.update((byte)(requested >> 21)); // requested*8 as int32
            digest.update((byte)(requested >> 13));
            digest.update((byte)(requested >> 5));
            digest.update((byte)(requested << 3));
            for (byte[] input : inputs) {
                digest.update(input);
            }
            try {
                digest.digest(temp, i * outLen, outLen);
            } catch (DigestException e) {
                throw new AssertionError("will not happen", e);
            }
            // 4.2 counter = counter + 1
            counter++;
        }
        // 5. requested_bits = leftmost (temp, no_of_bits_to_return).
        return temp.length == requested? temp: Arrays.copyOf(temp, requested);
        // 6. Return
    }

    // This method is used by both instantiation and reseeding.
    @Override
    protected final synchronized void hashReseedInternal(List<byte[]> inputs) {

        // 800-90Ar1 10.1.1.2: Instantiate Process.
        // 800-90Ar1 10.1.1.3: Reseed Process.
        byte[] seed;

        // Step 2: seed = Hash_df (seed_material, seedlen).
        if (v != null) {
            // Step 1 of 10.1.1.3: Prepend 0x01 || V
            inputs.add(0, ONE);
            inputs.add(1, v);
            seed = hashDf(seedLen, inputs);
        } else {
            seed = hashDf(seedLen, inputs);
        }

        // Step 3. V = seed.
        v = seed;

        // Step 4. C = Hash_df ((0x00 || V), seedlen).
        inputs = new ArrayList<>(2);
        inputs.add(ZERO);
        inputs.add(v);
        c = hashDf(seedLen, inputs);

        // Step 5. reseed_counter = 1.
        reseedCounter = 1;

        //status();

        // Step 6: Return
    }

    private void status() {
        if (debug != null) {
            debug.println(this, "V = " + HexFormat.of().formatHex(v));
            debug.println(this, "C = " + HexFormat.of().formatHex(c));
            debug.println(this, "reseed counter = " + reseedCounter);
        }
    }

    /**
     * Adds byte arrays into an existing one.
     *
     * @param out existing array
     * @param data more arrays, can be of different length
     */
    private static void addBytes(byte[] out, int len, byte[]... data) {
        for (byte[] d: data) {
            int dlen = d.length;
            int carry = 0;
            for (int i = 0; i < len; i++) {
                int sum = (out[len - i - 1] & 0xff) + carry;
                if (i < dlen) {
                    sum += (d[dlen - i - 1] & 0xff);
                }
                out[len - i - 1] = (byte) sum;
                carry = sum >> 8;
                if (i >= dlen - 1 && carry == 0) break;
            }
        }
    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @param result the array to be filled in with random bytes.
     */
    @Override
    public final synchronized void generateAlgorithm(
            byte[] result, byte[] additionalInput) {

        if (debug != null) {
            debug.println(this, "generateAlgorithm");
        }

        // 800-90Ar1 10.1.1.4: Hash_DRBG_Generate Process

        // Step 1: Check reseed_counter. Will not fail. Already checked in
        // AbstractDrbg#engineNextBytes.

        // Step 2: additional_input
        if (additionalInput != null) {
            digest.update((byte)2);
            digest.update(v);
            digest.update(additionalInput);
            addBytes(v, seedLen, digest.digest());
        }

        // Step 3. Hashgen (requested_number_of_bits, V).
        hashGen(result, v);

        // Step 4. H = Hash (0x03 || V).
        digest.update((byte)3);
        digest.update(v);
        byte[] h = digest.digest();

        // Step 5. V = (V + H + C + reseed_counter) mod 2seedlen.
        byte[] rcBytes;
        if (reseedCounter < 256) {
            rcBytes = new byte[]{(byte)reseedCounter};
        } else {
            rcBytes = BigInteger.valueOf(reseedCounter).toByteArray();
        }
        addBytes(v, seedLen, h, c, rcBytes);

        // Step 6. reseed_counter = reseed_counter + 1.
        reseedCounter++;

        //status();

        // Step 7: Return.
    }

    // 800-90Ar1 10.1.1.4: Hashgen
    private void hashGen(byte[] output, byte[] v) {

        // Step 2. data = V
        byte[] data = v;

        // Step 3: W is output not filled

        // Step 4: For i = 1 to m
        int pos = 0;
        int len = output.length;

        while (len > 0) {
            // Step 4.1 w = Hash (data).
            digest.update(data);
            if (len < outLen) {
                // Step 4.2 W = W || w.
                byte[] out = digest.digest();
                System.arraycopy(out, 0, output, pos, len);
                Arrays.fill(out, (byte)0);
            } else {
                try {
                    // Step 4.2 digest into right position, no need to cat
                    digest.digest(output, pos, outLen);
                } catch (DigestException e) {
                    throw new AssertionError("will not happen", e);
                }
            }
            len -= outLen;
            if (len <= 0) {
                // shortcut, so that data and pos needn't be updated
                break;
            }
            // Step 4.3 data = (data + 1) mod 2^seedlen.
            if (data == v) {
                data = Arrays.copyOf(v, v.length);
            }
            addBytes(data, seedLen, ONE);
            pos += outLen;
        }

        // Step 5: No need to truncate
        // Step 6: Return
    }
}
