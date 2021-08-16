/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.Objects;

/**
 * This class specifies the parameters used by a DRBG (Deterministic
 * Random Bit Generator).
 * <p>
 * According to
 * <a href="http://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-90Ar1.pdf">
 * NIST Special Publication 800-90A Revision 1, Recommendation for Random
 * Number Generation Using Deterministic Random Bit Generators</a> (800-90Ar1),
 * <blockquote>
 * A DRBG is based on a DRBG mechanism as specified in this Recommendation
 * and includes a source of randomness. A DRBG mechanism uses an algorithm
 * (i.e., a DRBG algorithm) that produces a sequence of bits from an initial
 * value that is determined by a seed that is determined from the output of
 * the randomness source."
 * </blockquote>
 * <p>
 * The 800-90Ar1 specification allows for a variety of DRBG implementation
 * choices, such as:
 * <ul>
 * <li> an entropy source,
 * <li> a DRBG mechanism (for example, Hash_DRBG),
 * <li> a DRBG algorithm (for example, SHA-256 for Hash_DRBG and AES-256
 * for CTR_DRBG. Please note that it is not the algorithm used in
 * {@link SecureRandom#getInstance}, which we will call a
 * <em>SecureRandom algorithm</em> below),
 * <li> optional features, including prediction resistance
 * and reseeding supports,
 * <li> highest security strength.
 * </ul>
 * <p>
 * These choices are set in each implementation and are not directly
 * managed by the {@code SecureRandom} API.  Check your DRBG provider's
 * documentation to find an appropriate implementation for the situation.
 * <p>
 * On the other hand, the 800-90Ar1 specification does have some configurable
 * options, such as:
 * <ul>
 * <li> required security strength,
 * <li> if prediction resistance is required,
 * <li> personalization string and additional input.
 * </ul>
 * <p>
 * A DRBG instance can be instantiated with parameters from an
 * {@link DrbgParameters.Instantiation} object and other information
 * (for example, the nonce, which is not managed by this API). This maps
 * to the {@code Instantiate_function} defined in NIST SP 800-90Ar1.
 * <p>
 * A DRBG instance can be reseeded with parameters from a
 * {@link DrbgParameters.Reseed} object. This maps to the
 * {@code Reseed_function} defined in NIST SP 800-90Ar1. Calling
 * {@link SecureRandom#reseed()} is equivalent to calling
 * {@link SecureRandom#reseed(SecureRandomParameters)} with the effective
 * instantiated prediction resistance flag (as returned by
 * {@link SecureRandom#getParameters()}) with no additional input.
 * <p>
 * A DRBG instance generates data with additional parameters from a
 * {@link DrbgParameters.NextBytes} object. This maps to the
 * {@code Generate_function} defined in NIST SP 800-90Ar1. Calling
 * {@link SecureRandom#nextBytes(byte[])} is equivalent to calling
 * {@link SecureRandom#nextBytes(byte[], SecureRandomParameters)}
 * with the effective instantiated strength and prediction resistance flag
 * (as returned by {@link SecureRandom#getParameters()}) with no
 * additional input.
 * <p>
 * A DRBG should be implemented as a subclass of {@link SecureRandomSpi}.
 * It is recommended that the implementation contain the 1-arg
 * {@linkplain SecureRandomSpi#SecureRandomSpi(SecureRandomParameters) constructor}
 * that takes a {@code DrbgParameters.Instantiation} argument. If implemented
 * this way, this implementation can be chosen by any
 * {@code SecureRandom.getInstance()} method. If it is chosen by a
 * {@code SecureRandom.getInstance()} with a {@link SecureRandomParameters}
 * parameter, the parameter is passed into this constructor. If it is chosen
 * by a {@code SecureRandom.getInstance()} without a
 * {@code SecureRandomParameters} parameter, the constructor is called with
 * a {@code null} argument and the implementation should choose its own
 * parameters. Its {@link SecureRandom#getParameters()} must always return a
 * non-null effective {@code DrbgParameters.Instantiation} object that reflects
 * how the DRBG is actually instantiated. A caller can use this information
 * to determine whether a {@code SecureRandom} object is a DRBG and what
 * features it supports. Please note that the returned value does not
 * necessarily equal to the {@code DrbgParameters.Instantiation} object passed
 * into the {@code SecureRandom.getInstance()} call. For example,
 * the requested capability can be {@link DrbgParameters.Capability#NONE}
 * but the effective value can be {@link DrbgParameters.Capability#RESEED_ONLY}
 * if the implementation supports reseeding. The implementation must implement
 * the {@link SecureRandomSpi#engineNextBytes(byte[], SecureRandomParameters)}
 * method which takes a {@code DrbgParameters.NextBytes} parameter. Unless
 * the result of {@link SecureRandom#getParameters()} has its
 * {@linkplain DrbgParameters.Instantiation#getCapability() capability} being
 * {@link Capability#NONE NONE}, it must implement
 * {@link SecureRandomSpi#engineReseed(SecureRandomParameters)} which takes
 * a {@code DrbgParameters.Reseed} parameter.
 * <p>
 * On the other hand, if a DRBG implementation does not contain a constructor
 * that has an {@code DrbgParameters.Instantiation} argument (not recommended),
 * it can only be chosen by a {@code SecureRandom.getInstance()} without
 * a {@code SecureRandomParameters} parameter, but will not be chosen if
 * a {@code getInstance} method with a {@code SecureRandomParameters} parameter
 * is called. If implemented this way, its {@link SecureRandom#getParameters()}
 * must return {@code null}, and it does not need to implement either
 * {@link SecureRandomSpi#engineNextBytes(byte[], SecureRandomParameters)}
 * or {@link SecureRandomSpi#engineReseed(SecureRandomParameters)}.
 * <p>
 * A DRBG might reseed itself automatically if the seed period is bigger
 * than the maximum seed life defined by the DRBG mechanism.
 * <p>
 * A DRBG implementation should support serialization and deserialization
 * by retaining the configuration and effective parameters, but the internal
 * state must not be serialized and the deserialized object must be
 * reinstantiated.
 * <p>
 * Examples:
 * <blockquote><pre>
 * SecureRandom drbg;
 * byte[] buffer = new byte[32];
 *
 * // Any DRBG is OK
 * drbg = SecureRandom.getInstance("DRBG");
 * drbg.nextBytes(buffer);
 *
 * SecureRandomParameters params = drbg.getParameters();
 * if (params instanceof DrbgParameters.Instantiation) {
 *     DrbgParameters.Instantiation ins = (DrbgParameters.Instantiation) params;
 *     if (ins.getCapability().supportsReseeding()) {
 *         drbg.reseed();
 *     }
 * }
 *
 * // The following call requests a weak DRBG instance. It is only
 * // guaranteed to support 112 bits of security strength.
 * drbg = SecureRandom.getInstance("DRBG",
 *         DrbgParameters.instantiation(112, NONE, null));
 *
 * // Both the next two calls will likely fail, because drbg could be
 * // instantiated with a smaller strength with no prediction resistance
 * // support.
 * drbg.nextBytes(buffer,
 *         DrbgParameters.nextBytes(256, false, "more".getBytes()));
 * drbg.nextBytes(buffer,
 *         DrbgParameters.nextBytes(112, true, "more".getBytes()));
 *
 * // The following call requests a strong DRBG instance, with a
 * // personalization string. If it successfully returns an instance,
 * // that instance is guaranteed to support 256 bits of security strength
 * // with prediction resistance available.
 * drbg = SecureRandom.getInstance("DRBG", DrbgParameters.instantiation(
 *         256, PR_AND_RESEED, "hello".getBytes()));
 *
 * // Prediction resistance is not requested in this single call,
 * // but an additional input is used.
 * drbg.nextBytes(buffer,
 *         DrbgParameters.nextBytes(-1, false, "more".getBytes()));
 *
 * // Same for this call.
 * drbg.reseed(DrbgParameters.reseed(false, "extra".getBytes()));</pre>
 * </blockquote>
 *
 * @implSpec
 * By convention, a provider should name its primary DRBG implementation
 * with the <a href=
 * "{@docRoot}/../specs/security/standard-names.html#securerandom-number-generation-algorithms">
 * standard {@code SecureRandom} algorithm name</a> "DRBG".
 *
 * @implNote
 * The following notes apply to the "DRBG" implementation in the SUN provider
 * of the JDK reference implementation.
 * <p>
 * This implementation supports the Hash_DRBG and HMAC_DRBG mechanisms with
 * DRBG algorithm SHA-224, SHA-512/224, SHA-256, SHA-512/256, SHA-384 and
 * SHA-512, and CTR_DRBG (both using derivation function and not using
 * derivation function) with DRBG algorithm AES-128, AES-192 and AES-256.
 * <p>
 * The mechanism name and DRBG algorithm name are determined by the
 * {@linkplain Security#getProperty(String) security property}
 * {@code securerandom.drbg.config}. The default choice is Hash_DRBG
 * with SHA-256.
 * <p>
 * For each combination, the security strength can be requested from 112
 * up to the highest strength it supports. Both reseeding and prediction
 * resistance are supported.
 * <p>
 * Personalization string is supported through the
 * {@link DrbgParameters.Instantiation} class and additional input is supported
 * through the {@link DrbgParameters.NextBytes} and
 * {@link DrbgParameters.Reseed} classes.
 * <p>
 * If a DRBG is not instantiated with a {@link DrbgParameters.Instantiation}
 * object explicitly, this implementation instantiates it with a default
 * requested strength of 128 bits, no prediction resistance request, and
 * no personalization string. These default instantiation parameters can also
 * be customized with the {@code securerandom.drbg.config} security property.
 * <p>
 * This implementation reads fresh entropy from the system default entropy
 * source determined by the security property {@code securerandom.source}.
 * <p>
 * Calling {@link SecureRandom#generateSeed(int)} will directly read
 * from this system default entropy source.
 *
 * @since 9
 */
public class DrbgParameters {

    private DrbgParameters() {
        // This class should not be instantiated
    }

    /**
     * The reseedable and prediction resistance capabilities of a DRBG.
     * <p>
     * When this object is passed to a {@code SecureRandom.getInstance()} call,
     * it is the requested minimum capability. When it's returned from
     * {@code SecureRandom.getParameters()}, it is the effective capability.
     * <p>
     * Please note that while the {@code Instantiate_function} defined in
     * NIST SP 800-90Ar1 only includes a {@code prediction_resistance_flag}
     * parameter, the {@code Capability} type includes an extra value
     * {@link #RESEED_ONLY} because reseeding is an optional function.
     * If {@code NONE} is used in an {@code Instantiation} object in calling the
     * {@code SecureRandom.getInstance} method, the returned DRBG instance
     * is not guaranteed to support reseeding. If {@code RESEED_ONLY} or
     * {@code PR_AND_RESEED} is used, the instance must support reseeding.
     * <p>
     * The table below lists possible effective values if a certain
     * capability is requested, i.e.
     * <blockquote><pre>
     * Capability requested = ...;
     * SecureRandom s = SecureRandom.getInstance("DRBG",
     *         DrbgParameters(-1, requested, null));
     * Capability effective = ((DrbgParametes.Initiate) s.getParameters())
     *         .getCapability();</pre>
     * </blockquote>
     * <table class="striped">
     * <caption style="display:none">requested and effective capabilities</caption>
     * <thead>
     * <tr>
     * <th scope="col">Requested Value</th>
     * <th scope="col">Possible Effective Values</th>
     * </tr>
     * </thead>
     * <tbody style="text-align:left">
     * <tr><th scope="row">NONE</th><td>NONE, RESEED_ONLY, PR_AND_RESEED</td></tr>
     * <tr><th scope="row">RESEED_ONLY</th><td>RESEED_ONLY, PR_AND_RESEED</td></tr>
     * <tr><th scope="row">PR_AND_RESEED</th><td>PR_AND_RESEED</td></tr>
     * </tbody>
     * </table>
     * <p>
     * A DRBG implementation supporting prediction resistance must also
     * support reseeding.
     *
     * @since 9
     */
    public enum Capability {

        /**
         * Both prediction resistance and reseed.
         */
        PR_AND_RESEED,

        /**
         * Reseed but no prediction resistance.
         */
        RESEED_ONLY,

        /**
         * Neither prediction resistance nor reseed.
         */
        NONE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * Returns whether this capability supports reseeding.
         *
         * @return {@code true} for {@link #PR_AND_RESEED} and
         *      {@link #RESEED_ONLY}, and {@code false} for {@link #NONE}
         */
        public boolean supportsReseeding() {
            return this != NONE;
        }

        /**
         * Returns whether this capability supports prediction resistance.
         *
         * @return {@code true} for {@link #PR_AND_RESEED}, and {@code false}
         *      for {@link #RESEED_ONLY} and {@link #NONE}
         */
        public boolean supportsPredictionResistance() {
            return this == PR_AND_RESEED;
        }
    }

    /**
     * DRBG parameters for instantiation.
     * <p>
     * When used in
     * {@link SecureRandom#getInstance(String, SecureRandomParameters)}
     * or one of the other similar {@code getInstance} calls that take a
     * {@code SecureRandomParameters} parameter, it means the
     * requested instantiate parameters the newly created {@code SecureRandom}
     * object must minimally support. When used as the return value of the
     * {@link SecureRandom#getParameters()} method, it means the effective
     * instantiate parameters of the {@code SecureRandom} object.
     *
     * @since 9
     */
    public static final class Instantiation
            implements SecureRandomParameters {

        private final int strength;
        private final Capability capability;
        private final byte[] personalizationString;

        /**
         * Returns the security strength in bits.
         *
         * @return If used in {@code getInstance}, returns the minimum strength
         * requested, or -1 if there is no specific request on the strength.
         * If used in {@code getParameters}, returns the effective strength.
         * The effective strength must be greater than or equal to the minimum
         * strength requested.
         */
        public int getStrength() {
            return strength;
        }

        /**
         * Returns the capability.
         *
         * @return If used in {@code getInstance}, returns the minimum
         * capability requested. If used in {@code getParameters}, returns
         * information on the effective prediction resistance flag and
         * whether it supports reseeding.
         */
        public Capability getCapability() {
            return capability;
        }

        /**
         * Returns the personalization string as a byte array.
         *
         * @return If used in {@code getInstance}, returns the requested
         * personalization string as a newly allocated array, or {@code null}
         * if no personalization string is requested. The same string should
         * be returned in {@code getParameters} as a new copy, or {@code null}
         * if no personalization string is requested in {@code getInstance}.
         */
        public byte[] getPersonalizationString() {
            return (personalizationString == null) ?
                    null : personalizationString.clone();
        }

        private Instantiation(int strength, Capability capability,
                              byte[] personalizationString) {
            if (strength < -1) {
                throw new IllegalArgumentException(
                        "Illegal security strength: " + strength);
            }
            this.strength = strength;
            this.capability = capability;
            this.personalizationString = (personalizationString == null) ?
                    null : personalizationString.clone();
        }

        /**
         * Returns a Human-readable string representation of this
         * {@code Instantiation}.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            // I don't care what personalizationString looks like
            return strength + "," + capability + "," + personalizationString;
        }
    }

    /**
     * DRBG parameters for random bits generation. It is used in
     * {@link SecureRandom#nextBytes(byte[], SecureRandomParameters)}.
     *
     * @since 9
     */
    public static final class NextBytes
            implements SecureRandomParameters {
        private final int strength;
        private final boolean predictionResistance;
        private final byte[] additionalInput;

        /**
         * Returns the security strength requested in bits.
         *
         * @return the strength requested, or -1 if the effective strength
         *      should be used.
         */
        public int getStrength() {
            return strength;
        }

        /**
         * Returns whether prediction resistance is requested.
         *
         * @return whether prediction resistance is requested
         */
        public boolean getPredictionResistance() {
            return predictionResistance;
        }

        /**
         * Returns the requested additional input.
         *
         * @return the requested additional input, {@code null} if not
         * requested. A new byte array is returned each time this method
         * is called.
         */
        public byte[] getAdditionalInput() {
            return additionalInput == null? null: additionalInput.clone();
        }

        private NextBytes(int strength, boolean predictionResistance,
                          byte[] additionalInput) {
            if (strength < -1) {
                throw new IllegalArgumentException(
                        "Illegal security strength: " + strength);
            }
            this.strength = strength;
            this.predictionResistance = predictionResistance;
            this.additionalInput = (additionalInput == null) ?
                    null : additionalInput.clone();
        }
    }

    /**
     * DRBG parameters for reseed. It is used in
     * {@link SecureRandom#reseed(SecureRandomParameters)}.
     *
     * @since 9
     */
    public static final class Reseed implements SecureRandomParameters {

        private final byte[] additionalInput;
        private final boolean predictionResistance;

        /**
         * Returns whether prediction resistance is requested.
         *
         * @return whether prediction resistance is requested
         */
        public boolean getPredictionResistance() {
            return predictionResistance;
        }

        /**
         * Returns the requested additional input.
         *
         * @return the requested additional input, or {@code null} if
         * not requested. A new byte array is returned each time this method
         * is called.
         */
        public byte[] getAdditionalInput() {
            return additionalInput == null ? null : additionalInput.clone();
        }

        private Reseed(boolean predictionResistance, byte[] additionalInput) {
            this.predictionResistance = predictionResistance;
            this.additionalInput = (additionalInput == null) ?
                    null : additionalInput.clone();
        }
    }

    /**
     * Generates a {@link DrbgParameters.Instantiation} object.
     *
     * @param strength security strength in bits, -1 for default strength
     *                 if used in {@code getInstance}.
     * @param capability capability
     * @param personalizationString personalization string as a byte array,
     *                              can be {@code null}. The content of this
     *                              byte array will be copied.
     * @return a new {@code Instantiation} object
     * @throws NullPointerException if {@code capability} is {@code null}
     * @throws IllegalArgumentException if {@code strength} is less than -1
     */
    public static Instantiation instantiation(int strength,
                                              Capability capability,
                                              byte[] personalizationString) {
        return new Instantiation(strength, Objects.requireNonNull(capability),
                personalizationString);
    }

    /**
     * Generates a {@link NextBytes} object.
     *
     * @param strength requested security strength in bits. If set to -1, the
     *                 effective strength will be used.
     * @param predictionResistance prediction resistance requested
     * @param additionalInput additional input, can be {@code null}.
     *                        The content of this byte array will be copied.
     * @throws IllegalArgumentException if {@code strength} is less than -1
     * @return a new {@code NextBytes} object
     */
    public static NextBytes nextBytes(int strength,
                                      boolean predictionResistance,
                                      byte[] additionalInput) {
        return new NextBytes(strength, predictionResistance, additionalInput);
    }

    /**
     * Generates a {@link Reseed} object.
     *
     * @param predictionResistance prediction resistance requested
     * @param additionalInput additional input, can be {@code null}.
     *                        The content of this byte array will be copied.
     * @return a new {@code Reseed} object
     */
    public static Reseed reseed(
            boolean predictionResistance, byte[] additionalInput) {
        return new Reseed(predictionResistance, additionalInput);
    }
}
