/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.rsa;

import sun.security.jca.JCAUtil;

import javax.crypto.BadPaddingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core of the RSA implementation. Has code to perform public and private key
 * RSA operations (with and without CRT for private key ops). Private CRT ops
 * also support blinding to thwart timing attacks.
 *
 * The code in this class only does the core RSA operation. Padding and
 * unpadding must be done externally.
 *
 * Note: RSA keys should be at least 512 bits long
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class RSACore {

    // globally enable/disable use of blinding
    private static final boolean ENABLE_BLINDING = true;

    // cache for blinding parameters. Map<BigInteger,
    // ConcurrentLinkedQueue<BlindingParameters>> use a weak hashmap so that,
    // cached values are automatically cleared when the modulus is GC'ed.
    // Multiple BlindingParameters can be queued during times of heavy load,
    // like performance testing.
    private static final Map<BigInteger, ConcurrentLinkedQueue<BlindingParameters>>
                blindingCache = new WeakHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    private RSACore() {
        // empty
    }

    /**
     * Return the number of bytes required to store the magnitude byte[] of
     * this BigInteger. Do not count a 0x00 byte toByteArray() would
     * prefix for 2's complement form.
     */
    public static int getByteLength(BigInteger b) {
        int n = b.bitLength();
        return (n + 7) >> 3;
    }

    /**
     * Return the number of bytes required to store the modulus of this
     * RSA key.
     */
    public static int getByteLength(RSAKey key) {
        return getByteLength(key.getModulus());
    }

    // temporary, used by RSACipher and RSAPadding. Move this somewhere else
    public static byte[] convert(byte[] b, int ofs, int len) {
        if ((ofs == 0) && (len == b.length)) {
            return b;
        } else {
            byte[] t = new byte[len];
            System.arraycopy(b, ofs, t, 0, len);
            return t;
        }
    }

    /**
     * Perform an RSA public key operation.
     */
    public static byte[] rsa(byte[] msg, RSAPublicKey key)
            throws BadPaddingException {
        return crypt(msg, key.getModulus(), key.getPublicExponent());
    }

    /**
     * Perform an RSA private key operation. Uses CRT if the key is a
     * CRT key with additional verification check after the signature
     * is computed.
     */
    @Deprecated
    public static byte[] rsa(byte[] msg, RSAPrivateKey key)
            throws BadPaddingException {
        return rsa(msg, key, true);
    }

    /**
     * Perform an RSA private key operation. Uses CRT if the key is a
     * CRT key. Set 'verify' to true if this function is used for
     * generating a signature.
     */
    public static byte[] rsa(byte[] msg, RSAPrivateKey key, boolean verify)
            throws BadPaddingException {
        if (key instanceof RSAPrivateCrtKey) {
            return crtCrypt(msg, (RSAPrivateCrtKey)key, verify);
        } else {
            return priCrypt(msg, key.getModulus(), key.getPrivateExponent());
        }
    }

    /**
     * RSA public key ops. Simple modPow().
     */
    private static byte[] crypt(byte[] msg, BigInteger n, BigInteger exp)
            throws BadPaddingException {
        BigInteger m = parseMsg(msg, n);
        BigInteger c = m.modPow(exp, n);
        return toByteArray(c, getByteLength(n));
    }

    /**
     * RSA non-CRT private key operations.
     */
    private static byte[] priCrypt(byte[] msg, BigInteger n, BigInteger exp)
            throws BadPaddingException {

        BigInteger c = parseMsg(msg, n);
        BlindingRandomPair brp = null;
        BigInteger m;
        if (ENABLE_BLINDING) {
            brp = getBlindingRandomPair(null, exp, n);
            c = c.multiply(brp.u).mod(n);
            m = c.modPow(exp, n);
            m = m.multiply(brp.v).mod(n);
        } else {
            m = c.modPow(exp, n);
        }

        return toByteArray(m, getByteLength(n));
    }

    /**
     * RSA private key operations with CRT. Algorithm and variable naming
     * are taken from PKCS#1 v2.1, section 5.1.2.
     */
    private static byte[] crtCrypt(byte[] msg, RSAPrivateCrtKey key,
            boolean verify) throws BadPaddingException {
        BigInteger n = key.getModulus();
        BigInteger c0 = parseMsg(msg, n);
        BigInteger c = c0;
        BigInteger p = key.getPrimeP();
        BigInteger q = key.getPrimeQ();
        BigInteger dP = key.getPrimeExponentP();
        BigInteger dQ = key.getPrimeExponentQ();
        BigInteger qInv = key.getCrtCoefficient();
        BigInteger e = key.getPublicExponent();
        BigInteger d = key.getPrivateExponent();

        BlindingRandomPair brp;
        if (ENABLE_BLINDING) {
            brp = getBlindingRandomPair(e, d, n);
            c = c.multiply(brp.u).mod(n);
        }

        // m1 = c ^ dP mod p
        BigInteger m1 = c.modPow(dP, p);
        // m2 = c ^ dQ mod q
        BigInteger m2 = c.modPow(dQ, q);

        // h = (m1 - m2) * qInv mod p
        BigInteger mtmp = m1.subtract(m2);
        if (mtmp.signum() < 0) {
            mtmp = mtmp.add(p);
        }
        BigInteger h = mtmp.multiply(qInv).mod(p);

        // m = m2 + q * h
        BigInteger m = h.multiply(q).add(m2);

        if (ENABLE_BLINDING) {
            m = m.multiply(brp.v).mod(n);
        }
        if (verify && !c0.equals(m.modPow(e, n))) {
            throw new BadPaddingException("RSA private key operation failed");
        }

        return toByteArray(m, getByteLength(n));
    }

    /**
     * Parse the msg into a BigInteger and check against the modulus n.
     */
    private static BigInteger parseMsg(byte[] msg, BigInteger n)
            throws BadPaddingException {
        BigInteger m = new BigInteger(1, msg);
        if (m.compareTo(n) >= 0) {
            throw new BadPaddingException("Message is larger than modulus");
        }
        return m;
    }

    /**
     * Return the encoding of this BigInteger that is exactly len bytes long.
     * Prefix/strip off leading 0x00 bytes if necessary.
     * Precondition: bi must fit into len bytes
     */
    private static byte[] toByteArray(BigInteger bi, int len) {
        byte[] b = bi.toByteArray();
        int n = b.length;
        if (n == len) {
            return b;
        }
        // BigInteger prefixed a 0x00 byte for 2's complement form, remove it
        if ((n == len + 1) && (b[0] == 0)) {
            byte[] t = new byte[len];
            System.arraycopy(b, 1, t, 0, len);
            Arrays.fill(b, (byte)0);
            return t;
        }
        // must be smaller
        assert (n < len);
        byte[] t = new byte[len];
        System.arraycopy(b, 0, t, (len - n), n);
        Arrays.fill(b, (byte)0);
        return t;
    }

    /**
     * Parameters (u,v) for RSA Blinding.  This is described in the RSA
     * Bulletin#2 (Jan 96) and other places:
     *
     *     ftp://ftp.rsa.com/pub/pdfs/bull-2.pdf
     *
     * The standard RSA Blinding decryption requires the public key exponent
     * (e) and modulus (n), and converts ciphertext (c) to plaintext (p).
     *
     * Before the modular exponentiation operation, the input message should
     * be multiplied by (u (mod n)), and afterward the result is corrected
     * by multiplying with (v (mod n)).  The system should reject messages
     * equal to (0 (mod n)).  That is:
     *
     *     1.  Generate r between 0 and n-1, relatively prime to n.
     *     2.  Compute x = (c*u) mod n
     *     3.  Compute y = (x^d) mod n
     *     4.  Compute p = (y*v) mod n
     *
     * The Java APIs allows for either standard RSAPrivateKey or
     * RSAPrivateCrtKey RSA keys.
     *
     * If the public exponent is available to us (e.g. RSAPrivateCrtKey),
     * choose a random r, then let (u, v):
     *
     *     u = r ^ e mod n
     *     v = r ^ (-1) mod n
     *
     * The proof follows:
     *
     *     p = (((c * u) ^ d mod n) * v) mod n
     *       = ((c ^ d) * (u ^ d) * v) mod n
     *       = ((c ^ d) * (r ^ e) ^ d) * (r ^ (-1))) mod n
     *       = ((c ^ d) * (r ^ (e * d)) * (r ^ (-1))) mod n
     *       = ((c ^ d) * (r ^ 1) * (r ^ (-1))) mod n  (see below)
     *       = (c ^ d) mod n
     *
     * because in RSA cryptosystem, d is the multiplicative inverse of e:
     *
     *    (r^(e * d)) mod n
     *       = (r ^ 1) mod n
     *       = r mod n
     *
     * However, if the public exponent is not available (e.g. RSAPrivateKey),
     * we mitigate the timing issue by using a similar random number blinding
     * approach using the private key:
     *
     *     u = r
     *     v = ((r ^ (-1)) ^ d) mod n
     *
     * This returns the same plaintext because:
     *
     *     p = (((c * u) ^ d mod n) * v) mod n
     *       = ((c ^ d) * (u ^ d) * v) mod n
     *       = ((c ^ d) * (u ^ d) * ((u ^ (-1)) ^d)) mod n
     *       = (c ^ d) mod n
     *
     * Computing inverses mod n and random number generation is slow, so
     * it is often not practical to generate a new random (u, v) pair for
     * each new exponentiation.  The calculation of parameters might even be
     * subject to timing attacks.  However, (u, v) pairs should not be
     * reused since they themselves might be compromised by timing attacks,
     * leaving the private exponent vulnerable.  An efficient solution to
     * this problem is update u and v before each modular exponentiation
     * step by computing:
     *
     *     u = u ^ 2
     *     v = v ^ 2
     *
     * The total performance cost is small.
     */
    private static final class BlindingRandomPair {
        final BigInteger u;
        final BigInteger v;

        BlindingRandomPair(BigInteger u, BigInteger v) {
            this.u = u;
            this.v = v;
        }
    }

    /**
     * Set of blinding parameters for a given RSA key.
     *
     * The RSA modulus is usually unique, so we index by modulus in
     * {@code blindingCache}.  However, to protect against the unlikely
     * case of two keys sharing the same modulus, we also store the public
     * or the private exponent.  This means we cannot cache blinding
     * parameters for multiple keys that share the same modulus, but
     * since sharing moduli is fundamentally broken and insecure, this
     * does not matter.
     */
    private static final class BlindingParameters {
        private static final BigInteger BIG_TWO = BigInteger.valueOf(2L);

        // RSA public exponent
        private final BigInteger e;

        // hash code of RSA private exponent
        private final BigInteger d;

        // r ^ e mod n (CRT), or r mod n (Non-CRT)
        private BigInteger u;

        // r ^ (-1) mod n (CRT) , or ((r ^ (-1)) ^ d) mod n (Non-CRT)
        private BigInteger v;

        // e: the public exponent
        // d: the private exponent
        // n: the modulus
        BlindingParameters(BigInteger e, BigInteger d, BigInteger n) {
            this.u = null;
            this.v = null;
            this.e = e;
            this.d = d;

            int len = n.bitLength();
            SecureRandom random = JCAUtil.getSecureRandom();
            u = new BigInteger(len, random).mod(n);
            // Although the possibility is very much limited that u is zero
            // or is not relatively prime to n, we still want to be careful
            // about the special value.
            //
            // Secure random generation is expensive, try to use BigInteger.ONE
            // this time if this new generated random number is zero or is not
            // relatively prime to n.  Next time, new generated secure random
            // number will be used instead.
            if (u.equals(BigInteger.ZERO)) {
                u = BigInteger.ONE;     // use 1 this time
            }

            try {
                // The call to BigInteger.modInverse() checks that u is
                // relatively prime to n.  Otherwise, ArithmeticException is
                // thrown.
                v = u.modInverse(n);
            } catch (ArithmeticException ae) {
                // if u is not relatively prime to n, use 1 this time
                u = BigInteger.ONE;
                v = BigInteger.ONE;
            }

            if (e != null) {
                u = u.modPow(e, n);   // e: the public exponent
                                      // u: random ^ e
                                      // v: random ^ (-1)
            } else {
                v = v.modPow(d, n);   // d: the private exponent
                                      // u: random
                                      // v: random ^ (-d)
            }
        }

        // return null if need to reset the parameters
        BlindingRandomPair getBlindingRandomPair(
                BigInteger e, BigInteger d, BigInteger n) {

            if ((this.e != null && this.e.equals(e)) ||
                (this.d != null && this.d.equals(d))) {

                BlindingRandomPair brp = new BlindingRandomPair(u, v);
                if (u.compareTo(BigInteger.ONE) <= 0 ||
                    v.compareTo(BigInteger.ONE) <= 0) {
                    // Reset so the parameters will be not queued later
                    u = BigInteger.ZERO;
                    v = BigInteger.ZERO;
                } else {
                    u = u.modPow(BIG_TWO, n);
                    v = v.modPow(BIG_TWO, n);
                }

                return brp;
            }

            return null;
        }

        // Check if reusable, return true if both u & v are not zero.
        boolean isReusable() {
            return !u.equals(BigInteger.ZERO) && !v.equals(BigInteger.ZERO);
        }
    }

    private static BlindingRandomPair getBlindingRandomPair(
            BigInteger e, BigInteger d, BigInteger n) {

        ConcurrentLinkedQueue<BlindingParameters> queue;

        // Get queue from map, if there is none then create one
        lock.lock();
        try {
            queue = blindingCache.computeIfAbsent(n,
                ignored -> new ConcurrentLinkedQueue<>());
        } finally {
            lock.unlock();
        }

        BlindingParameters bps = queue.poll();
        if (bps == null) {
            bps = new BlindingParameters(e, d, n);
        }

        BlindingRandomPair brp = null;

        // Loops to get a valid pair, going through the queue or create a new
        // parameters if needed.
        while (brp == null) {
            brp = bps.getBlindingRandomPair(e, d, n);
            if (brp == null) {
                // need to reset the blinding parameters, first check for
                // another in the queue.
                bps = queue.poll();
                if (bps == null) {
                    bps = new BlindingParameters(e, d, n);
                }
            }
        }

        // If this parameters are still usable, put them back into the queue.
        if (bps.isReusable()) {
            queue.add(bps);
        }
        return brp;
    }

}
