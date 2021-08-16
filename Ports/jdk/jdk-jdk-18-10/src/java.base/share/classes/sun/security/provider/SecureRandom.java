/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandomSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * <p>This class provides a crytpographically strong pseudo-random number
 * generator based on the SHA-1 hash algorithm.
 *
 * <p>Note that if a seed is not provided, we attempt to provide sufficient
 * seed bytes to completely randomize the internal state of the generator
 * (20 bytes).  However, our seed generation algorithm has not been thoroughly
 * studied or widely deployed.
 *
 * <p>Also note that when a random object is deserialized,
 * <a href="#engineNextBytes(byte[])">engineNextBytes</a> invoked on the
 * restored random object will yield the exact same (random) bytes as the
 * original object.  If this behaviour is not desired, the restored random
 * object should be seeded, using
 * <a href="#engineSetSeed(byte[])">engineSetSeed</a>.
 *
 * @author Benjamin Renaud
 * @author Josh Bloch
 * @author Gadi Guy
 */

public final class SecureRandom extends SecureRandomSpi
implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 3581829991155417889L;

    private static final int DIGEST_SIZE = 20;
    private transient MessageDigest digest;
    private byte[] state;
    private byte[] remainder;
    private int remCount;

    /**
     * An empty constructor that creates an unseeded SecureRandom object.
     * <p>
     * Unless the user calls setSeed(), the first call to engineGetBytes()
     * will have the SeedGenerator provide sufficient seed bytes to
     * completely randomize the internal state of the generator (20 bytes).
     * Note that the old threaded seed generation algorithm is provided
     * only as a fallback, and has not been thoroughly studied or widely
     * deployed.
     * <p>
     * The SeedGenerator relies on a VM-wide entropy pool to generate
     * seed bytes for these objects.  The first time the SeedGenerator is
     * called, it may take several seconds of CPU time to initialize,
     * depending on the underlying hardware.  Successive calls run
     * quickly because they rely on the same (internal) pseudo-random
     * number generator for their seed bits.
     */
    public SecureRandom() {
        init(null);
    }

    /**
     * This constructor is used to instantiate the private seeder object
     * with a given seed from the SeedGenerator.
     *
     * @param seed the seed.
     */
    private SecureRandom(byte[] seed) {
        init(seed);
    }

    /**
     * This call, used by the constructors, instantiates the SHA digest
     * and sets the seed, if given.
     */
    private void init(byte[] seed) {
        try {
            /*
             * Use the local SUN implementation to avoid native
             * performance overhead.
             */
            digest = MessageDigest.getInstance("SHA", "SUN");
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            // Fallback to any available.
            try {
                digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException exc) {
                throw new InternalError(
                    "internal error: SHA-1 not available.", exc);
            }
        }

        if (seed != null) {
           engineSetSeed(seed);
        }
    }

    /**
     * Returns the given number of seed bytes, computed using the seed
     * generation algorithm that this class uses to seed itself.  This
     * call may be used to seed other random number generators.  While
     * we attempt to return a "truly random" sequence of bytes, we do not
     * know exactly how random the bytes returned by this call are.  (See
     * the empty constructor <a href = "#SecureRandom">SecureRandom</a>
     * for a brief description of the underlying algorithm.)
     * The prudent user will err on the side of caution and get extra
     * seed bytes, although it should be noted that seed generation is
     * somewhat costly.
     *
     * @param numBytes the number of seed bytes to generate.
     *
     * @return the seed bytes.
     */
    @Override
    public byte[] engineGenerateSeed(int numBytes) {
        // Neither of the SeedGenerator implementations require
        // locking, so no sync needed here.
        byte[] b = new byte[numBytes];
        SeedGenerator.generateSeed(b);
        return b;
    }

    /**
     * Reseeds this random object. The given seed supplements, rather than
     * replaces, the existing seed. Thus, repeated calls are guaranteed
     * never to reduce randomness.
     *
     * @param seed the seed.
     */
    @Override
    public synchronized void engineSetSeed(byte[] seed) {
        if (state != null) {
            digest.update(state);
            for (int i = 0; i < state.length; i++) {
                state[i] = 0;
            }
        }
        state = digest.digest(seed);
        remCount = 0;
    }

    private static void updateState(byte[] state, byte[] output) {
        int last = 1;
        int v;
        byte t;
        boolean zf = false;

        // state(n + 1) = (state(n) + output(n) + 1) % 2^160;
        for (int i = 0; i < state.length; i++) {
            // Add two bytes
            v = (int)state[i] + (int)output[i] + last;
            // Result is lower 8 bits
            t = (byte)v;
            // Store result. Check for state collision.
            zf = zf | (state[i] != t);
            state[i] = t;
            // High 8 bits are carry. Store for next iteration.
            last = v >> 8;
        }

        // Make sure at least one bit changes!
        if (!zf) {
           state[0]++;
        }
    }

    /**
     * This static object will be seeded by SeedGenerator, and used
     * to seed future instances of SHA1PRNG SecureRandoms.
     *
     * Bloch, Effective Java Second Edition: Item 71
     */
    private static class SeederHolder {

        private static final SecureRandom seeder;

        static {
            /*
             * Call to SeedGenerator.generateSeed() to add additional
             * seed material (likely from the Native implementation).
             */
            seeder = new SecureRandom(SeedGenerator.getSystemEntropy());
            byte [] b = new byte[DIGEST_SIZE];
            SeedGenerator.generateSeed(b);
            seeder.engineSetSeed(b);
        }
    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @param result the array to be filled in with random bytes.
     */
    @Override
    public synchronized void engineNextBytes(byte[] result) {
        int index = 0;
        int todo;
        byte[] output = remainder;

        if (state == null) {
            byte[] seed = new byte[DIGEST_SIZE];
            SeederHolder.seeder.engineNextBytes(seed);
            state = digest.digest(seed);
        }

        // Use remainder from last time
        int r = remCount;
        if (r > 0) {
            // How many bytes?
            todo = (result.length - index) < (DIGEST_SIZE - r) ?
                        (result.length - index) : (DIGEST_SIZE - r);
            // Copy the bytes, zero the buffer
            for (int i = 0; i < todo; i++) {
                result[i] = output[r];
                output[r++] = 0;
            }
            remCount += todo;
            index += todo;
        }

        // If we need more bytes, make them.
        while (index < result.length) {
            // Step the state
            digest.update(state);
            output = digest.digest();
            updateState(state, output);

            // How many bytes?
            todo = (result.length - index) > DIGEST_SIZE ?
                DIGEST_SIZE : result.length - index;
            // Copy the bytes, zero the buffer
            for (int i = 0; i < todo; i++) {
                result[index++] = output[i];
                output[i] = 0;
            }
            remCount += todo;
        }

        // Store remainder for next time
        remainder = output;
        remCount %= DIGEST_SIZE;
    }

    /*
     * readObject is called to restore the state of the random object from
     * a stream.  We have to create a new instance of MessageDigest, because
     * it is not included in the stream (it is marked "transient").
     *
     * Note that the engineNextBytes() method invoked on the restored random
     * object will yield the exact same (random) bytes as the original.
     * If you do not want this behaviour, you should re-seed the restored
     * random object, using engineSetSeed().
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {

        s.defaultReadObject ();

        try {
            /*
             * Use the local SUN implementation to avoid native
             * performance overhead.
             */
            digest = MessageDigest.getInstance("SHA", "SUN");
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            // Fallback to any available.
            try {
                digest = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException exc) {
                throw new InternalError(
                    "internal error: SHA-1 not available.", exc);
            }
        }
    }
}
