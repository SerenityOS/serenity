/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import sun.security.jca.GetInstance;
import sun.security.util.Debug;
import sun.security.util.MessageDigestSpi2;

import javax.crypto.SecretKey;

/**
 * This MessageDigest class provides applications the functionality of a
 * message digest algorithm, such as SHA-1 or SHA-256.
 * Message digests are secure one-way hash functions that take arbitrary-sized
 * data and output a fixed-length hash value.
 *
 * <p>A MessageDigest object starts out initialized. The data is
 * processed through it using the {@link #update(byte) update}
 * methods. At any point {@link #reset() reset} can be called
 * to reset the digest. Once all the data to be updated has been
 * updated, one of the {@link #digest() digest} methods should
 * be called to complete the hash computation.
 *
 * <p>The {@code digest} method can be called once for a given number
 * of updates. After {@code digest} has been called, the MessageDigest
 * object is reset to its initialized state.
 *
 * <p>Implementations are free to implement the Cloneable interface.
 * Client applications can test cloneability by attempting cloning
 * and catching the CloneNotSupportedException:
 *
 * <pre>{@code
 * MessageDigest md = MessageDigest.getInstance("SHA-256");
 *
 * try {
 *     md.update(toChapter1);
 *     MessageDigest tc1 = md.clone();
 *     byte[] toChapter1Digest = tc1.digest();
 *     md.update(toChapter2);
 *     ...etc.
 * } catch (CloneNotSupportedException cnse) {
 *     throw new DigestException("couldn't make digest of partial content");
 * }
 * }</pre>
 *
 * <p>Note that if a given implementation is not cloneable, it is
 * still possible to compute intermediate digests by instantiating
 * several instances, if the number of digests is known in advance.
 *
 * <p>Note that this class is abstract and extends from
 * {@code MessageDigestSpi} for historical reasons.
 * Application developers should only take notice of the methods defined in
 * this {@code MessageDigest} class; all the methods in
 * the superclass are intended for cryptographic service providers who wish to
 * supply their own implementations of message digest algorithms.
 *
 * <p> Every implementation of the Java platform is required to support
 * the following standard {@code MessageDigest} algorithms:
 * <ul>
 * <li>{@code SHA-1}</li>
 * <li>{@code SHA-256}</li>
 * </ul>
 * These algorithms are described in the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
 * MessageDigest section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other algorithms are supported.
 *
 * @author Benjamin Renaud
 * @since 1.1
 *
 * @see DigestInputStream
 * @see DigestOutputStream
 */

public abstract class MessageDigest extends MessageDigestSpi {

    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("messagedigest");

    private String algorithm;

    // The state of this digest
    private static final int INITIAL = 0;
    private static final int IN_PROGRESS = 1;
    private int state = INITIAL;

    // The provider
    private Provider provider;

    /**
     * Creates a message digest with the specified algorithm name.
     *
     * @param algorithm the standard name of the digest algorithm.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     */
    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    // private constructor used only by Delegate class
    private MessageDigest(String algorithm, Provider p) {
        this.algorithm = algorithm;
        this.provider = p;
    }

    /**
     * Returns a MessageDigest object that implements the specified digest
     * algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new MessageDigest object encapsulating the
     * MessageDigestSpi implementation from the first
     * Provider that supports the specified algorithm is returned.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @implNote
     * The JDK Reference Implementation additionally uses the
     * {@code jdk.security.provider.preferred}
     * {@link Security#getProperty(String) Security} property to determine
     * the preferred provider order for the specified algorithm. This
     * may be different than the order of providers returned by
     * {@link Security#getProviders() Security.getProviders()}.
     *
     * @param algorithm the name of the algorithm requested.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @return a {@code MessageDigest} object that implements the
     *         specified algorithm
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code MessageDigestSpi} implementation for the
     *         specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     */
    public static MessageDigest getInstance(String algorithm)
        throws NoSuchAlgorithmException
    {
        Objects.requireNonNull(algorithm, "null algorithm name");
        MessageDigest md;

        GetInstance.Instance instance = GetInstance.getInstance("MessageDigest",
                MessageDigestSpi.class, algorithm);
        if (instance.impl instanceof MessageDigest messageDigest) {
            md = messageDigest;
            md.provider = instance.provider;
        } else {
            md = Delegate.of((MessageDigestSpi)instance.impl, algorithm,
                instance.provider);
        }

        if (!skipDebug && pdebug != null) {
            pdebug.println("MessageDigest." + algorithm +
                " algorithm from: " + md.provider.getName());
        }

        return md;
    }

    /**
     * Returns a MessageDigest object that implements the specified digest
     * algorithm.
     *
     * <p> A new MessageDigest object encapsulating the
     * MessageDigestSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the name of the algorithm requested.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return a {@code MessageDigest} object that implements the
     *         specified algorithm
     *
     * @throws IllegalArgumentException if the provider name is {@code null}
     *         or empty
     *
     * @throws NoSuchAlgorithmException if a {@code MessageDigestSpi}
     *         implementation for the specified algorithm is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     */
    public static MessageDigest getInstance(String algorithm, String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (provider == null || provider.isEmpty())
            throw new IllegalArgumentException("missing provider");

        MessageDigest md;
        GetInstance.Instance instance = GetInstance.getInstance("MessageDigest",
                MessageDigestSpi.class, algorithm, provider);
        if (instance.impl instanceof MessageDigest messageDigest) {
            md = messageDigest;
            md.provider = instance.provider;
        } else {
            md = Delegate.of((MessageDigestSpi)instance.impl, algorithm,
                    instance.provider);
        }
        return md;
    }

    /**
     * Returns a MessageDigest object that implements the specified digest
     * algorithm.
     *
     * <p> A new MessageDigest object encapsulating the
     * MessageDigestSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the name of the algorithm requested.
     * See the MessageDigest section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the provider.
     *
     * @return a {@code MessageDigest} object that implements the
     *         specified algorithm
     *
     * @throws IllegalArgumentException if the specified provider is
     *         {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code MessageDigestSpi}
     *         implementation for the specified algorithm is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see Provider
     *
     * @since 1.4
     */
    public static MessageDigest getInstance(String algorithm,
                                            Provider provider)
        throws NoSuchAlgorithmException
    {
        Objects.requireNonNull(algorithm, "null algorithm name");
        if (provider == null)
            throw new IllegalArgumentException("missing provider");
        Object[] objs = Security.getImpl(algorithm, "MessageDigest", provider);
        if (objs[0] instanceof MessageDigest md) {
            md.provider = (Provider)objs[1];
            return md;
        } else {
            MessageDigest delegate =
                    Delegate.of((MessageDigestSpi)objs[0], algorithm,
                    (Provider)objs[1]);
            return delegate;
        }
    }

    /**
     * Returns the provider of this message digest object.
     *
     * @return the provider of this message digest object
     */
    public final Provider getProvider() {
        return this.provider;
    }

    /**
     * Updates the digest using the specified byte.
     *
     * @param input the byte with which to update the digest.
     */
    public void update(byte input) {
        engineUpdate(input);
        state = IN_PROGRESS;
    }

    /**
     * Updates the digest using the specified array of bytes, starting
     * at the specified offset.
     *
     * @param input the array of bytes.
     *
     * @param offset the offset to start from in the array of bytes.
     *
     * @param len the number of bytes to use, starting at
     * {@code offset}.
     */
    public void update(byte[] input, int offset, int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        engineUpdate(input, offset, len);
        state = IN_PROGRESS;
    }

    /**
     * Updates the digest using the specified array of bytes.
     *
     * @param input the array of bytes.
     */
    public void update(byte[] input) {
        engineUpdate(input, 0, input.length);
        state = IN_PROGRESS;
    }

    /**
     * Update the digest using the specified ByteBuffer. The digest is
     * updated using the {@code input.remaining()} bytes starting
     * at {@code input.position()}.
     * Upon return, the buffer's position will be equal to its limit;
     * its limit will not have changed.
     *
     * @param input the ByteBuffer
     * @since 1.5
     */
    public final void update(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException();
        }
        engineUpdate(input);
        state = IN_PROGRESS;
    }

    /**
     * Completes the hash computation by performing final operations
     * such as padding. The digest is reset after this call is made.
     *
     * @return the array of bytes for the resulting hash value.
     */
    public byte[] digest() {
        /* Resetting is the responsibility of implementors. */
        byte[] result = engineDigest();
        state = INITIAL;
        return result;
    }

    /**
     * Completes the hash computation by performing final operations
     * such as padding. The digest is reset after this call is made.
     *
     * @param buf output buffer for the computed digest
     *
     * @param offset offset into the output buffer to begin storing the digest
     *
     * @param len number of bytes within buf allotted for the digest
     *
     * @return the number of bytes placed into {@code buf}
     *
     * @throws    DigestException if an error occurs.
     */
    public int digest(byte[] buf, int offset, int len) throws DigestException {
        if (buf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (buf.length - offset < len) {
            throw new IllegalArgumentException
                ("Output buffer too small for specified offset and length");
        }
        int numBytes = engineDigest(buf, offset, len);
        state = INITIAL;
        return numBytes;
    }

    /**
     * Performs a final update on the digest using the specified array
     * of bytes, then completes the digest computation. That is, this
     * method first calls {@link #update(byte[]) update(input)},
     * passing the <i>input</i> array to the {@code update} method,
     * then calls {@link #digest() digest()}.
     *
     * @param input the input to be updated before the digest is
     * completed.
     *
     * @return the array of bytes for the resulting hash value.
     */
    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    private String getProviderName() {
        return (provider == null) ? "(no provider)" : provider.getName();
    }

    /**
     * Returns a string representation of this message digest object.
     */
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(baos);
        p.print(algorithm+" Message Digest from "+getProviderName()+", ");
        switch (state) {
            case INITIAL     -> p.print("<initialized>");
            case IN_PROGRESS -> p.print("<in progress>");
        }
        p.println();
        return (baos.toString());
    }

    /**
     * Compares two digests for equality. Two digests are equal if they have
     * the same length and all bytes at corresponding positions are equal.
     *
     * @implNote
     * All bytes in {@code digesta} are examined to determine equality.
     * The calculation time depends only on the length of {@code digesta}.
     * It does not depend on the length of {@code digestb} or the contents
     * of {@code digesta} and {@code digestb}.
     *
     * @param digesta one of the digests to compare.
     *
     * @param digestb the other digest to compare.
     *
     * @return true if the digests are equal, false otherwise.
     */
    public static boolean isEqual(byte[] digesta, byte[] digestb) {
        if (digesta == digestb) return true;
        if (digesta == null || digestb == null) {
            return false;
        }

        int lenA = digesta.length;
        int lenB = digestb.length;

        if (lenB == 0) {
            return lenA == 0;
        }

        int result = 0;
        result |= lenA - lenB;

        // time-constant comparison
        for (int i = 0; i < lenA; i++) {
            // If i >= lenB, indexB is 0; otherwise, i.
            int indexB = ((i - lenB) >>> 31) * i;
            result |= digesta[i] ^ digestb[indexB];
        }
        return result == 0;
    }

    /**
     * Resets the digest for further use.
     */
    public void reset() {
        engineReset();
        state = INITIAL;
    }

    /**
     * Returns a string that identifies the algorithm, independent of
     * implementation details. The name should be a standard
     * Java Security name (such as "SHA-256").
     * See the MessageDigest section in the <a href=
     * "{@docRoot}/../specs/security/standard-names.html#messagedigest-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @return the name of the algorithm
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns the length of the digest in bytes, or 0 if this operation is
     * not supported by the provider and the implementation is not cloneable.
     *
     * @return the digest length in bytes, or 0 if this operation is not
     * supported by the provider and the implementation is not cloneable.
     *
     * @since 1.2
     */
    public final int getDigestLength() {
        int digestLen = engineGetDigestLength();
        if (digestLen == 0) {
            try {
                MessageDigest md = (MessageDigest)clone();
                byte[] digest = md.digest();
                return digest.length;
            } catch (CloneNotSupportedException e) {
                return digestLen;
            }
        }
        return digestLen;
    }

    /**
     * Returns a clone if the implementation is cloneable.
     *
     * @return a clone if the implementation is cloneable.
     *
     * @throws    CloneNotSupportedException if this is called on an
     * implementation that does not support {@code Cloneable}.
     */
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        } else {
            throw new CloneNotSupportedException();
        }
    }


    /*
     * The following class allows providers to extend from MessageDigestSpi
     * rather than from MessageDigest. It represents a MessageDigest with an
     * encapsulated, provider-supplied SPI object (of type MessageDigestSpi).
     * If the provider implementation is an instance of MessageDigestSpi,
     * the getInstance() methods above return an instance of this class, with
     * the SPI object encapsulated.
     *
     * Note: All SPI methods from the original MessageDigest class have been
     * moved up the hierarchy into a new class (MessageDigestSpi), which has
     * been interposed in the hierarchy between the API (MessageDigest)
     * and its original parent (Object).
     */

    private static class Delegate extends MessageDigest
            implements MessageDigestSpi2 {
        // use this class for spi objects which implements Cloneable
        private static final class CloneableDelegate extends Delegate
                implements Cloneable {

            private CloneableDelegate(MessageDigestSpi digestSpi,
                    String algorithm, Provider p) {
                super(digestSpi, algorithm, p);
            }
        }

        // The provider implementation (delegate)
        private final MessageDigestSpi digestSpi;

        // factory method used by MessageDigest class to create Delegate objs
        static Delegate of(MessageDigestSpi digestSpi, String algo,
                Provider p) {
            Objects.requireNonNull(digestSpi);
            boolean isCloneable = (digestSpi instanceof Cloneable);
            // Spi impls from SunPKCS11 provider implement Cloneable but their
            // clone() may throw CloneNotSupportException
            if (isCloneable && p.getName().startsWith("SunPKCS11") &&
                    p.getClass().getModule().getName().equals
                    ("jdk.crypto.cryptoki")) {
                try {
                    digestSpi.clone();
                } catch (CloneNotSupportedException cnse) {
                    isCloneable = false;
                }
            }
            return (isCloneable? new CloneableDelegate(digestSpi, algo, p) :
                    new Delegate(digestSpi, algo, p));
        }

        // private constructor
        private Delegate(MessageDigestSpi digestSpi, String algorithm,
                Provider p) {
            super(algorithm, p);
            this.digestSpi = digestSpi;
        }

        /**
         * Returns a clone if the delegate is cloneable.
         *
         * @return a clone if the delegate is cloneable.
         *
         * @throws    CloneNotSupportedException if this is called on a
         * delegate that does not support {@code Cloneable}.
         */
        @Override
        public Object clone() throws CloneNotSupportedException {
            if (this instanceof Cloneable) {
                // Because 'algorithm', 'provider', and 'state' are private
                // members of our supertype, we must perform a cast to
                // access them.
                MessageDigest that = new CloneableDelegate(
                        (MessageDigestSpi)digestSpi.clone(),
                        ((MessageDigest)this).algorithm,
                        ((MessageDigest)this).provider);
                that.state = ((MessageDigest)this).state;
                return that;
            } else {
                throw new CloneNotSupportedException();
            }
        }

        @Override
        protected int engineGetDigestLength() {
            return digestSpi.engineGetDigestLength();
        }

        @Override
        protected void engineUpdate(byte input) {
            digestSpi.engineUpdate(input);
        }

        @Override
        protected void engineUpdate(byte[] input, int offset, int len) {
            digestSpi.engineUpdate(input, offset, len);
        }

        @Override
        protected void engineUpdate(ByteBuffer input) {
            digestSpi.engineUpdate(input);
        }

        @Override
        public void engineUpdate(SecretKey key) throws InvalidKeyException {
            if (digestSpi instanceof MessageDigestSpi2) {
                ((MessageDigestSpi2)digestSpi).engineUpdate(key);
            } else {
                throw new UnsupportedOperationException
                ("Digest does not support update of SecretKey object");
            }
        }

        @Override
        protected byte[] engineDigest() {
            return digestSpi.engineDigest();
        }

        @Override
        protected int engineDigest(byte[] buf, int offset, int len)
            throws DigestException {
                return digestSpi.engineDigest(buf, offset, len);
        }

        @Override
        protected void engineReset() {
            digestSpi.engineReset();
        }
    }
}
