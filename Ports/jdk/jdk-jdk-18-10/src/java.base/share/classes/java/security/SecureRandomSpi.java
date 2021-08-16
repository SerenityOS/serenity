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

package java.security;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the {@link SecureRandom} class.
 * <p>
 * All the abstract methods in this class must be implemented by each
 * service provider who wishes to supply the implementation
 * of a cryptographically strong pseudo-random number generator.
 *
 * @implSpec
 * If the {@link #SecureRandomSpi(SecureRandomParameters)}
 * constructor is overridden in an implementation, it will always be called
 * whenever a {@code SecureRandom} is instantiated. Precisely, if an object is
 * instantiated with one of {@code SecureRandom}'s {@code getInstance} methods
 * <em>without</em> a {@link SecureRandomParameters} parameter,
 * the constructor will be called with a {@code null} argument and the
 * implementation is responsible for creating its own
 * {@code SecureRandomParameters} parameter for use when
 * {@link #engineGetParameters()} is called. If an object
 * is instantiated with one of {@code SecureRandom}'s {@code getInstance}
 * methods <em>with</em> a {@code SecureRandomParameters} argument,
 * the constructor will be called with that argument. The
 * {@link #engineGetParameters()} method must not return {@code null}.
 * <p>
 * Otherwise, if the {@code SecureRandomSpi(SecureRandomParameters)}
 * constructor is not overridden in an implementation, the
 * {@link #SecureRandomSpi()} constructor must be overridden and it will be
 * called if an object is instantiated with one of {@code SecureRandom}'s
 * {@code getInstance} methods <em>without</em> a
 * {@code SecureRandomParameters} argument. Calling one of
 * {@code SecureRandom}'s {@code getInstance} methods <em>with</em>
 * a {@code SecureRandomParameters} argument will never
 * return an instance of this implementation. The
 * {@link #engineGetParameters()} method must return {@code null}.
 * <p>
 * See {@link SecureRandom} for additional details on thread safety. By
 * default, a {@code SecureRandomSpi} implementation is considered to be
 * not safe for use by multiple concurrent threads and {@code SecureRandom}
 * will synchronize access to each of the applicable engine methods
 * (see {@link SecureRandom} for the list of methods). However, if a
 * {@code SecureRandomSpi} implementation is thread-safe, the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#service-attributes">
 * service provider attribute</a> "ThreadSafe" should be set to "true" during
 * its registration, as follows:
 * <blockquote><pre>
 * put("SecureRandom.AlgName ThreadSafe", "true");</pre>
 * </blockquote>
 * or
 * <blockquote><pre>
 * putService(new Service(this, "SecureRandom", "AlgName", className,
 *          null, Map.of("ThreadSafe", "true")));</pre>
 * </blockquote>
 * {@code SecureRandom} will call the applicable engine methods
 * without any synchronization.
 *
 * @since 1.2
 */

public abstract class SecureRandomSpi implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = -2991854161009191830L;

    /**
     * Constructor without a parameter.
     */
    public SecureRandomSpi() {
        // ignored
    }

    /**
     * Constructor with a parameter.
     *
     * @param params the {@link SecureRandomParameters} object.
     *               This argument can be {@code null}.
     * @throws IllegalArgumentException if {@code params} is
     *         unrecognizable or unsupported by this {@code SecureRandom}
     *
     * @since 9
     */
    protected SecureRandomSpi(SecureRandomParameters params) {
        // ignored
    }

    /**
     * Reseeds this random object with the given seed. The seed supplements,
     * rather than replaces, the existing seed. Thus, repeated calls
     * are guaranteed never to reduce randomness.
     *
     * @param seed the seed.
     */
    protected abstract void engineSetSeed(byte[] seed);

    /**
     * Generates a user-specified number of random bytes.
     * <p>
     * Some random number generators can only generate a limited amount
     * of random bytes per invocation. If the size of {@code bytes}
     * is greater than this limit, the implementation should invoke
     * its generation process multiple times to completely fill the
     * buffer before returning from this method.
     *
     * @param bytes the array to be filled in with random bytes.
     */
    protected abstract void engineNextBytes(byte[] bytes);

    /**
     * Generates a user-specified number of random bytes with
     * additional parameters.
     * <p>
     * Some random number generators can only generate a limited amount
     * of random bytes per invocation. If the size of {@code bytes}
     * is greater than this limit, the implementation should invoke
     * its generation process multiple times to completely fill the
     * buffer before returning from this method.
     *
     * @implSpec The default implementation throws
     * an {@link UnsupportedOperationException}.
     *
     * @param bytes the array to be filled in with random bytes
     * @param params additional parameters
     * @throws UnsupportedOperationException if the implementation
     *         has not overridden this method
     * @throws IllegalArgumentException if {@code params} is {@code null},
     *         illegal or unsupported by this {@code SecureRandom}
     *
     * @since 9
     */
    protected void engineNextBytes(
            byte[] bytes, SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the given number of seed bytes.  This call may be used to
     * seed other random number generators.
     *
     * @param numBytes the number of seed bytes to generate.
     *
     * @return the seed bytes.
     */
    protected abstract byte[] engineGenerateSeed(int numBytes);

    /**
     * Reseeds this random object with entropy input read from its
     * entropy source with additional parameters.
     * <p>
     * If this method is called by {@link SecureRandom#reseed()},
     * {@code params} will be {@code null}.
     * <p>
     * Do not override this method if the implementation does not
     * support reseeding.
     *
     * @implSpec The default implementation throws
     *           an {@link UnsupportedOperationException}.
     *
     * @param params extra parameters, can be {@code null}.
     * @throws UnsupportedOperationException if the implementation
     *         has not overridden this method
     * @throws IllegalArgumentException if {@code params} is
     *         illegal or unsupported by this {@code SecureRandom}
     *
     * @since 9
     */
    protected void engineReseed(SecureRandomParameters params) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the effective {@link SecureRandomParameters} for this
     * {@code SecureRandom} instance.
     *
     * @implSpec The default implementation returns {@code null}.
     *
     * @return the effective {@link SecureRandomParameters} parameters,
     * or {@code null} if no parameters were used.
     *
     * @since 9
     */
    protected SecureRandomParameters engineGetParameters() {
        return null;
    }

    /**
     * Returns a Human-readable string representation of this
     * {@code SecureRandom}.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
