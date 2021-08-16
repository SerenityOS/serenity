/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto;

import java.util.*;

import java.security.*;
import java.security.Provider.Service;
import java.security.spec.AlgorithmParameterSpec;

import java.nio.ByteBuffer;

import sun.security.util.Debug;
import sun.security.jca.*;
import sun.security.jca.GetInstance.Instance;

/**
 * This class provides the functionality of a "Message Authentication Code"
 * (MAC) algorithm.
 *
 * <p> A MAC provides a way to check
 * the integrity of information transmitted over or stored in an unreliable
 * medium, based on a secret key. Typically, message
 * authentication codes are used between two parties that share a secret
 * key in order to validate information transmitted between these
 * parties.
 *
 * <p> A MAC mechanism that is based on cryptographic hash functions is
 * referred to as HMAC. HMAC can be used with any cryptographic hash function,
 * e.g., SHA256 or SHA384, in combination with a secret shared key. HMAC is
 * specified in RFC 2104.
 *
 * <p> Every implementation of the Java platform is required to support
 * the following standard {@code Mac} algorithms:
 * <ul>
 * <li>{@code HmacSHA1}</li>
 * <li>{@code HmacSHA256}</li>
 * </ul>
 * These algorithms are described in the
 * <a href="{@docRoot}/../specs/security/standard-names.html#mac-algorithms">
 * Mac section</a> of the
 * Java Security Standard Algorithm Names Specification.
 * Consult the release documentation for your implementation to see if any
 * other algorithms are supported.
 *
 * @author Jan Luehe
 *
 * @since 1.4
 */

public class Mac implements Cloneable {

    private static final Debug debug =
                        Debug.getInstance("jca", "Mac");

    private static final Debug pdebug =
                        Debug.getInstance("provider", "Provider");
    private static final boolean skipDebug =
        Debug.isOn("engine=") && !Debug.isOn("mac");

    // The provider
    private Provider provider;

    // The provider implementation (delegate)
    private MacSpi spi;

    // The name of the MAC algorithm.
    private final String algorithm;

    // Has this object been initialized?
    private boolean initialized = false;

    // next service to try in provider selection
    // null once provider is selected
    private Service firstService;

    // remaining services to try in provider selection
    // null once provider is selected
    private Iterator<Service> serviceIterator;

    private final Object lock;

    /**
     * Creates a MAC object.
     *
     * @param macSpi the delegate
     * @param provider the provider
     * @param algorithm the algorithm
     */
    protected Mac(MacSpi macSpi, Provider provider, String algorithm) {
        this.spi = macSpi;
        this.provider = provider;
        this.algorithm = algorithm;
        serviceIterator = null;
        lock = null;
    }

    private Mac(Service s, Iterator<Service> t, String algorithm) {
        firstService = s;
        serviceIterator = t;
        this.algorithm = algorithm;
        lock = new Object();
    }

    /**
     * Returns the algorithm name of this {@code Mac} object.
     *
     * <p>This is the same name that was specified in one of the
     * {@code getInstance} calls that created this
     * {@code Mac} object.
     *
     * @return the algorithm name of this {@code Mac} object.
     */
    public final String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns a {@code Mac} object that implements the
     * specified MAC algorithm.
     *
     * <p> This method traverses the list of registered security Providers,
     * starting with the most preferred Provider.
     * A new Mac object encapsulating the
     * MacSpi implementation from the first
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
     * @param algorithm the standard name of the requested MAC algorithm.
     * See the Mac section in the <a href=
     *   "{@docRoot}/../specs/security/standard-names.html#mac-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @return the new {@code Mac} object
     *
     * @throws NoSuchAlgorithmException if no {@code Provider} supports a
     *         {@code MacSpi} implementation for the specified algorithm
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final Mac getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        List<Service> services = GetInstance.getServices("Mac", algorithm);
        // make sure there is at least one service from a signed provider
        Iterator<Service> t = services.iterator();
        while (t.hasNext()) {
            Service s = t.next();
            if (JceSecurity.canUseProvider(s.getProvider()) == false) {
                continue;
            }
            return new Mac(s, t, algorithm);
        }
        throw new NoSuchAlgorithmException
                                ("Algorithm " + algorithm + " not available");
    }

    /**
     * Returns a {@code Mac} object that implements the
     * specified MAC algorithm.
     *
     * <p> A new Mac object encapsulating the
     * MacSpi implementation from the specified provider
     * is returned.  The specified provider must be registered
     * in the security provider list.
     *
     * <p> Note that the list of registered providers may be retrieved via
     * the {@link Security#getProviders() Security.getProviders()} method.
     *
     * @param algorithm the standard name of the requested MAC algorithm.
     * See the Mac section in the <a href=
     *   "{@docRoot}/../specs/security/standard-names.html#mac-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the name of the provider.
     *
     * @return the new {@code Mac} object
     *
     * @throws IllegalArgumentException if the {@code provider}
     *         is {@code null} or empty
     *
     * @throws NoSuchAlgorithmException if a {@code MacSpi}
     *         implementation for the specified algorithm is not
     *         available from the specified provider
     *
     * @throws NoSuchProviderException if the specified provider is not
     *         registered in the security provider list
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final Mac getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = JceSecurity.getInstance
                ("Mac", MacSpi.class, algorithm, provider);
        return new Mac((MacSpi)instance.impl, instance.provider, algorithm);
    }

    /**
     * Returns a {@code Mac} object that implements the
     * specified MAC algorithm.
     *
     * <p> A new Mac object encapsulating the
     * MacSpi implementation from the specified Provider
     * object is returned.  Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * @param algorithm the standard name of the requested MAC algorithm.
     * See the Mac section in the <a href=
     *   "{@docRoot}/../specs/security/standard-names.html#mac-algorithms">
     * Java Security Standard Algorithm Names Specification</a>
     * for information about standard algorithm names.
     *
     * @param provider the provider.
     *
     * @return the new {@code Mac} object
     *
     * @throws IllegalArgumentException if the {@code provider} is
     *         {@code null}
     *
     * @throws NoSuchAlgorithmException if a {@code MacSpi}
     *         implementation for the specified algorithm is not available
     *         from the specified {@code Provider} object
     *
     * @throws NullPointerException if {@code algorithm} is {@code null}
     *
     * @see java.security.Provider
     */
    public static final Mac getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm, "null algorithm name");
        Instance instance = JceSecurity.getInstance
                ("Mac", MacSpi.class, algorithm, provider);
        return new Mac((MacSpi)instance.impl, instance.provider, algorithm);
    }

    // max number of debug warnings to print from chooseFirstProvider()
    private static int warnCount = 10;

    /**
     * Choose the Spi from the first provider available. Used if
     * delayed provider selection is not possible because init()
     * is not the first method called.
     */
    void chooseFirstProvider() {
        if ((spi != null) || (serviceIterator == null)) {
            return;
        }
        synchronized (lock) {
            if (spi != null) {
                return;
            }
            if (debug != null) {
                int w = --warnCount;
                if (w >= 0) {
                    debug.println("Mac.init() not first method "
                        + "called, disabling delayed provider selection");
                    if (w == 0) {
                        debug.println("Further warnings of this type will "
                            + "be suppressed");
                    }
                    new Exception("Call trace").printStackTrace();
                }
            }
            Exception lastException = null;
            while ((firstService != null) || serviceIterator.hasNext()) {
                Service s;
                if (firstService != null) {
                    s = firstService;
                    firstService = null;
                } else {
                    s = serviceIterator.next();
                }
                if (JceSecurity.canUseProvider(s.getProvider()) == false) {
                    continue;
                }
                try {
                    Object obj = s.newInstance(null);
                    if (obj instanceof MacSpi == false) {
                        continue;
                    }
                    spi = (MacSpi)obj;
                    provider = s.getProvider();
                    // not needed any more
                    firstService = null;
                    serviceIterator = null;
                    return;
                } catch (NoSuchAlgorithmException e) {
                    lastException = e;
                }
            }
            ProviderException e = new ProviderException
                    ("Could not construct MacSpi instance");
            if (lastException != null) {
                e.initCause(lastException);
            }
            throw e;
        }
    }

    private void chooseProvider(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        synchronized (lock) {
            if (spi != null) {
                spi.engineInit(key, params);
                return;
            }
            Exception lastException = null;
            while ((firstService != null) || serviceIterator.hasNext()) {
                Service s;
                if (firstService != null) {
                    s = firstService;
                    firstService = null;
                } else {
                    s = serviceIterator.next();
                }
                // if provider says it does not support this key, ignore it
                if (s.supportsParameter(key) == false) {
                    continue;
                }
                if (JceSecurity.canUseProvider(s.getProvider()) == false) {
                    continue;
                }
                try {
                    MacSpi spi = (MacSpi)s.newInstance(null);
                    spi.engineInit(key, params);
                    provider = s.getProvider();
                    this.spi = spi;
                    firstService = null;
                    serviceIterator = null;
                    return;
                } catch (Exception e) {
                    // NoSuchAlgorithmException from newInstance()
                    // InvalidKeyException from init()
                    // RuntimeException (ProviderException) from init()
                    if (lastException == null) {
                        lastException = e;
                    }
                }
            }
            // no working provider found, fail
            if (lastException instanceof InvalidKeyException) {
                throw (InvalidKeyException)lastException;
            }
            if (lastException instanceof InvalidAlgorithmParameterException) {
                throw (InvalidAlgorithmParameterException)lastException;
            }
            if (lastException instanceof RuntimeException) {
                throw (RuntimeException)lastException;
            }
            String kName = (key != null) ? key.getClass().getName() : "(null)";
            throw new InvalidKeyException
                ("No installed provider supports this key: "
                + kName, lastException);
        }
    }

    /**
     * Returns the provider of this {@code Mac} object.
     *
     * @return the provider of this {@code Mac} object.
     */
    public final Provider getProvider() {
        chooseFirstProvider();
        return this.provider;
    }

    /**
     * Returns the length of the MAC in bytes.
     *
     * @return the MAC length in bytes.
     */
    public final int getMacLength() {
        chooseFirstProvider();
        return spi.engineGetMacLength();
    }

    private String getProviderName() {
        return (provider == null) ? "(no provider)" : provider.getName();
    }

    /**
     * Initializes this {@code Mac} object with the given key.
     *
     * @param key the key.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this MAC.
     */
    public final void init(Key key) throws InvalidKeyException {
        try {
            if (spi != null) {
                spi.engineInit(key, null);
            } else {
                chooseProvider(key, null);
            }
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("init() failed", e);
        }
        initialized = true;

        if (!skipDebug && pdebug != null) {
            pdebug.println("Mac." + algorithm + " algorithm from: " +
                getProviderName());
        }
    }

    /**
     * Initializes this {@code Mac} object with the given key and
     * algorithm parameters.
     *
     * @param key the key.
     * @param params the algorithm parameters.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this MAC.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this MAC.
     */
    public final void init(Key key, AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (spi != null) {
            spi.engineInit(key, params);
        } else {
            chooseProvider(key, params);
        }
        initialized = true;

        if (!skipDebug && pdebug != null) {
            pdebug.println("Mac." + algorithm + " algorithm from: " +
                getProviderName());
        }
    }

    /**
     * Processes the given byte.
     *
     * @param input the input byte to be processed.
     *
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     */
    public final void update(byte input) throws IllegalStateException {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }
        spi.engineUpdate(input);
    }

    /**
     * Processes the given array of bytes.
     *
     * @param input the array of bytes to be processed.
     *
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     */
    public final void update(byte[] input) throws IllegalStateException {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (input != null) {
            spi.engineUpdate(input, 0, input.length);
        }
    }

    /**
     * Processes the first {@code len} bytes in {@code input},
     * starting at {@code offset} inclusive.
     *
     * @param input the input buffer.
     * @param offset the offset in {@code input} where the input starts.
     * @param len the number of bytes to process.
     *
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     */
    public final void update(byte[] input, int offset, int len)
            throws IllegalStateException {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }

        if (input != null) {
            if ((offset < 0) || (len > (input.length - offset)) || (len < 0))
                throw new IllegalArgumentException("Bad arguments");
            spi.engineUpdate(input, offset, len);
        }
    }

    /**
     * Processes {@code input.remaining()} bytes in the ByteBuffer
     * {@code input}, starting at {@code input.position()}.
     * Upon return, the buffer's position will be equal to its limit;
     * its limit will not have changed.
     *
     * @param input the ByteBuffer
     *
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     * @since 1.5
     */
    public final void update(ByteBuffer input) {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (input == null) {
            throw new IllegalArgumentException("Buffer must not be null");
        }
        spi.engineUpdate(input);
    }

    /**
     * Finishes the MAC operation.
     *
     * <p>A call to this method resets this {@code Mac} object to the
     * state it was in when previously initialized via a call to
     * {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     * That is, the object is reset and available to generate another MAC from
     * the same key, if desired, via new calls to {@code update} and
     * {@code doFinal}.
     * (In order to reuse this {@code Mac} object with a different key,
     * it must be reinitialized via a call to {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     *
     * @return the MAC result.
     *
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     */
    public final byte[] doFinal() throws IllegalStateException {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }
        byte[] mac = spi.engineDoFinal();
        spi.engineReset();
        return mac;
    }

    /**
     * Finishes the MAC operation.
     *
     * <p>A call to this method resets this {@code Mac} object to the
     * state it was in when previously initialized via a call to
     * {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     * That is, the object is reset and available to generate another MAC from
     * the same key, if desired, via new calls to {@code update} and
     * {@code doFinal}.
     * (In order to reuse this {@code Mac} object with a different key,
     * it must be reinitialized via a call to {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     *
     * <p>The MAC result is stored in {@code output}, starting at
     * {@code outOffset} inclusive.
     *
     * @param output the buffer where the MAC result is stored
     * @param outOffset the offset in {@code output} where the MAC is
     * stored
     *
     * @exception ShortBufferException if the given output buffer is too small
     * to hold the result
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     */
    public final void doFinal(byte[] output, int outOffset)
        throws ShortBufferException, IllegalStateException
    {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }
        int macLen = getMacLength();
        if (output == null || output.length-outOffset < macLen) {
            throw new ShortBufferException
                ("Cannot store MAC in output buffer");
        }
        byte[] mac = doFinal();
        System.arraycopy(mac, 0, output, outOffset, macLen);
        return;
    }

    /**
     * Processes the given array of bytes and finishes the MAC operation.
     *
     * <p>A call to this method resets this {@code Mac} object to the
     * state it was in when previously initialized via a call to
     * {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     * That is, the object is reset and available to generate another MAC from
     * the same key, if desired, via new calls to {@code update} and
     * {@code doFinal}.
     * (In order to reuse this {@code Mac} object with a different key,
     * it must be reinitialized via a call to {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     *
     * @param input data in bytes
     * @return the MAC result.
     *
     * @exception IllegalStateException if this {@code Mac} has not been
     * initialized.
     */
    public final byte[] doFinal(byte[] input) throws IllegalStateException
    {
        chooseFirstProvider();
        if (initialized == false) {
            throw new IllegalStateException("MAC not initialized");
        }
        update(input);
        return doFinal();
    }

    /**
     * Resets this {@code Mac} object.
     *
     * <p>A call to this method resets this {@code Mac} object to the
     * state it was in when previously initialized via a call to
     * {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     * That is, the object is reset and available to generate another MAC from
     * the same key, if desired, via new calls to {@code update} and
     * {@code doFinal}.
     * (In order to reuse this {@code Mac} object with a different key,
     * it must be reinitialized via a call to {@code init(Key)} or
     * {@code init(Key, AlgorithmParameterSpec)}.
     */
    public final void reset() {
        chooseFirstProvider();
        spi.engineReset();
    }

    /**
     * Returns a clone if the provider implementation is cloneable.
     *
     * @return a clone if the provider implementation is cloneable.
     *
     * @exception CloneNotSupportedException if this is called on a
     * delegate that does not support {@code Cloneable}.
     */
    public final Object clone() throws CloneNotSupportedException {
        chooseFirstProvider();
        Mac that = (Mac)super.clone();
        that.spi = (MacSpi)this.spi.clone();
        return that;
    }
}
