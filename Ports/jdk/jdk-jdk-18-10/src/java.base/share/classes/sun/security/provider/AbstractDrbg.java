/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.util.Debug;

import java.security.*;
import java.util.Arrays;
import java.util.Objects;
import static java.security.DrbgParameters.Capability.*;

/**
 * The abstract base class for all DRBGs. It is used as {@link DRBG#impl}.
 * <p>
 * This class has 5 abstract methods. 3 are defined by SP800-90A:
 * <ol>
 *  <li>{@link #generateAlgorithm(byte[], byte[])}
 *  <li>{@link #reseedAlgorithm(byte[], byte[])} (In fact this is not an
 *      abstract method, but any DRBG supporting reseeding must override it.)
 *  <li>{@link #instantiateAlgorithm(byte[])}
 * </ol>
 * and 2 for implementation purpose:
 * <ol>
 *  <li>{@link #initEngine()}
 *  <li>{@link #chooseAlgorithmAndStrength}
 * </ol>
 * Although this class is not a child class of {@link SecureRandomSpi}, it
 * implements all abstract methods there as final.
 * <p>
 * The initialization process of a DRBG is divided into 2 phases:
 * {@link #configure configuration} is eagerly called to set up parameters,
 * and {@link #instantiateIfNecessary instantiation} is lazily called only
 * when nextBytes or reseed is called.
 * <p>
 * SecureRandom methods like reseed and nextBytes are not thread-safe.
 * An implementation is required to protect shared access to instantiate states
 * (instantiated, nonce) and DRBG states (v, c, key, reseedCounter, etc).
 */
public abstract class AbstractDrbg {

    /**
     * This field is not null if {@code -Djava.security.debug=securerandom} is
     * specified on the command line. An implementation can print useful
     * debug info.
     */
    protected static final Debug debug = Debug.getInstance(
            "securerandom", "drbg");

    // Common working status

    private boolean instantiated;

    /**
     * Reseed counter of a DRBG instance. A mechanism should increment it
     * after each random bits generation and reset it in reseed. A mechanism
     * does <em>not</em> need to compare it to {@link #reseedInterval}.
     *
     * Volatile, will be used in a double checked locking.
     */
    protected volatile int reseedCounter;

    // Mech features. If not same as below, must be redefined in constructor.

    /**
     * Default strength of a DRBG instance if it is not configured.
     * 128 is considered secure enough now. A mechanism
     * can change it in a constructor.
     *
     * Remember to sync with "securerandom.drbg.config" in java.security.
     */
    protected static final int DEFAULT_STRENGTH = 128;

    /**
     * Mechanism name, say, {@code HashDRBG}. Must be set in constructor.
     * This value will be used in {@code toString}.
     */
    protected String mechName = "DRBG";

    /**
     * highest_supported_security_strength of this mechanism for all algorithms
     * it supports. A mechanism should update the value in its constructor
     * if the value is not 256.
     */
    protected int highestSupportedSecurityStrength = 256;

    /**
     * Whether prediction resistance is supported. A mechanism should update
     * the value in its constructor if it is <em>not</em> supported.
     */
    protected boolean supportPredictionResistance = true;

    /**
     * Whether reseed is supported. A mechanism should update
     * the value in its constructor if it is <em>not</em> supported.
     */
    protected boolean supportReseeding = true;

    // Strength features. If not same as below, must be redefined in
    // chooseAlgorithmAndStrength. Among these, minLength and seedLen have no
    // default value and must be redefined. If personalization string or
    // additional input is not supported, set maxPersonalizationStringLength
    // or maxAdditionalInputLength to -1.

    /**
     * Minimum entropy input length in bytes for this DRBG instance.
     * Must be assigned in {@link #chooseAlgorithmAndStrength}.
     */
    protected int minLength;

    /**
     * Maximum entropy input length in bytes for this DRBG instance.
     * Should be assigned in {@link #chooseAlgorithmAndStrength} if it is not
     * {@link Integer#MAX_VALUE}.
     * <p>
     * In theory this value (and the values below) can be bigger than
     * {@code Integer.MAX_VALUE} but a Java array can only have an int32 index.
     */
    protected int maxLength = Integer.MAX_VALUE;

    /**
     * Maximum personalization string length in bytes for this DRBG instance.
     * Should be assigned in {@link #chooseAlgorithmAndStrength} if it is not
     * {@link Integer#MAX_VALUE}.
     */
    protected int maxPersonalizationStringLength = Integer.MAX_VALUE;

    /**
     * Maximum additional input length in bytes for this DRBG instance.
     * Should be assigned in {@link #chooseAlgorithmAndStrength} if it is not
     * {@link Integer#MAX_VALUE}.
     */
    protected int maxAdditionalInputLength = Integer.MAX_VALUE;

    /**
     * max_number_of_bits_per_request in bytes for this DRBG instance.
     * Should be assigned in {@link #chooseAlgorithmAndStrength} if it is not
     * {@link Integer#MAX_VALUE}.
     */
    protected int maxNumberOfBytesPerRequest = Integer.MAX_VALUE;

    /**
     * Maximum number of requests between reseeds for this DRBG instance.
     * Should be assigned in {@link #chooseAlgorithmAndStrength} if it is not
     * {@link Integer#MAX_VALUE}.
     */
    protected int reseedInterval = Integer.MAX_VALUE;


    /**
     * Algorithm used by this instance (SHA-512 or AES-256). Must be assigned
     * in {@link #chooseAlgorithmAndStrength}. This field is used in
     * {@link #toString()}.
     */
    protected String algorithm;

    // Configurable parameters

    /**
     * Security strength for this instance. Must be assigned in
     * {@link #chooseAlgorithmAndStrength}. Should be at least the requested
     * strength. Might be smaller than the highest strength
     * {@link #algorithm} supports. Must not be -1.
     */
    protected int securityStrength;     // in bits

    /**
     * Strength requested in {@link DrbgParameters.Instantiation}.
     * The real strength is based on it. Do not modify it in a mechanism.
     */
    protected int requestedInstantiationSecurityStrength = -1;

    /**
     * The personalization string used by this instance. Set inside
     * {@link #configure(SecureRandomParameters)} and
     * can be used in a mechanism. Do not modify it in a mechanism.
     */
    protected byte[] personalizationString;

    /**
     * The prediction resistance flag used by this instance. Set inside
     * {@link #configure(SecureRandomParameters)}.
     */
    private boolean predictionResistanceFlag;

    // Non-standard configurable parameters

    /**
     * Whether a derivation function is used. Requested in
     * {@link MoreDrbgParameters}. Only CtrDRBG uses it.
     * Do not modify it in a mechanism.
     */
    protected boolean usedf;

    /**
     * The nonce for this instance. Set in {@link #instantiateIfNecessary}.
     * After instantiation, this field is not null. Do not modify it
     * in a mechanism.
     */
    protected byte[] nonce;

    /**
     * Requested nonce in {@link MoreDrbgParameters}. If set to null,
     * nonce will be chosen by system, and a reinstantiated DRBG will get a
     * new system-provided nonce.
     */
    private byte[] requestedNonce;

    /**
     * Requested algorithm in {@link MoreDrbgParameters}.
     * Do not modify it in a mechanism.
     */
    protected String requestedAlgorithm;

    /**
     * The entropy source used by this instance. Set inside
     * {@link #configure(SecureRandomParameters)}. This field
     * can be null. {@link #getEntropyInput} will take care of null check.
     */
    private EntropySource es;

    // Five abstract methods for SP 800-90A DRBG

    /**
     * Decides what algorithm and strength to use (SHA-256 or AES-256,
     * 128 or 256). Strength related fields must also be defined or redefined
     * here. Called in {@link #configure}. A mechanism uses
     * {@link #requestedAlgorithm},
     * {@link #requestedInstantiationSecurityStrength}, and
     * {@link #DEFAULT_STRENGTH} to decide which algorithm and strength to use.
     * <p>
     * If {@code requestedAlgorithm} is provided, it will always be used.
     * If {@code requestedInstantiationSecurityStrength} is also provided,
     * the algorithm will use the strength (an exception will be thrown if
     * the strength is not supported), otherwise, the smaller one of
     * the highest supported strength of the algorithm and the default strength
     * will be used.
     * <p>
     * If {@code requestedAlgorithm} is not provided, an algorithm will be
     * chosen that supports {@code requestedInstantiationSecurityStrength}
     * (or {@code DEFAULT_STRENGTH} if there is no request).
     * <p>
     * Since every call to {@link #configure} will call this method,
     * make sure to the calls do not contradict with each other.
     * <p>
     * Here are some examples of the algorithm and strength chosen (suppose
     * {@code DEFAULT_STRENGTH} is 128) for HashDRBG:
     * <pre>
     * requested             effective
     * (SHA-224, 256)        IAE
     * (SHA-256, -1)         (SHA-256,128)
     * (SHA-256, 112)        (SHA-256,112)
     * (SHA-256, 128)        (SHA-256,128)
     * (SHA-3, -1)           IAE
     * (null, -1)            (SHA-256,128)
     * (null, 112)           (SHA-256,112)
     * (null, 192)           (SHA-256,192)
     * (null, 256)           (SHA-256,256)
     * (null, 384)           IAE
     * </pre>
     *
     * @throws IllegalArgumentException if the requested parameters
     *      can not be supported or contradict with each other.
     */
    protected abstract void chooseAlgorithmAndStrength();

    /**
     * Initiates security engines ({@code MessageDigest}, {@code Mac},
     * or {@code Cipher}). This method is called during instantiation.
     */
    protected abstract void initEngine();

    /**
     * Instantiates a DRBG. Called automatically before the first
     * {@code nextBytes} call.
     * <p>
     * Note that the other parameters (nonce, strength, ps) are already
     * stored inside at configuration.
     *
     * @param ei the entropy input, its length is already conditioned to be
     *           between {@link #minLength} and {@link #maxLength}.
     */
    protected abstract void instantiateAlgorithm(byte[] ei);

    /**
     * The generate function.
     *
     * @param result fill result here, not null
     * @param additionalInput additional input, can be null. If not null,
     *          its length is smaller than {@link #maxAdditionalInputLength}
     */
    protected abstract void generateAlgorithm(
            byte[] result, byte[] additionalInput);

    /**
     * The reseed function.
     *
     * @param ei the entropy input, its length is already conditioned to be
     *           between {@link #minLength} and {@link #maxLength}.
     * @param additionalInput additional input, can be null. If not null,
     *          its length is smaller than {@link #maxAdditionalInputLength}
     * @throws UnsupportedOperationException if reseed is not supported
     */
    protected void reseedAlgorithm(
            byte[] ei, byte[] additionalInput) {
        throw new UnsupportedOperationException("No reseed function");
    }

    // SecureRandomSpi methods taken care of here. All final.

    protected final void engineNextBytes(byte[] result) {
        engineNextBytes(result, DrbgParameters.nextBytes(
                -1, predictionResistanceFlag, null));
    }

    protected final void engineNextBytes(
            byte[] result, SecureRandomParameters params) {

        Objects.requireNonNull(result);

        if (debug != null) {
            debug.println(this, "nextBytes");
        }
        if (params instanceof DrbgParameters.NextBytes) {

            // 800-90Ar1 9.3: Generate Process.

            DrbgParameters.NextBytes dp = (DrbgParameters.NextBytes) params;

            // Step 2: max_number_of_bits_per_request
            if (result.length > maxNumberOfBytesPerRequest) {
                // generateAlgorithm should be called multiple times to fill
                // up result. Unimplemented since maxNumberOfBytesPerRequest
                // is now Integer.MAX_VALUE.
            }

            // Step 3: check requested_security_strength
            if (dp.getStrength() > securityStrength) {
                throw new IllegalArgumentException("strength too high: "
                        + dp.getStrength());
            }

            // Step 4: check max_additional_input_length
            byte[] ai = dp.getAdditionalInput();
            if (ai != null && ai.length > maxAdditionalInputLength) {
                throw new IllegalArgumentException("ai too long: "
                        + ai.length);
            }

            // Step 5: check prediction_resistance_flag
            boolean pr = dp.getPredictionResistance();
            if (!predictionResistanceFlag && pr) {
                throw new IllegalArgumentException("pr not available");
            }

            instantiateIfNecessary(null);

            // Step 7: Auto reseed (reseedCounter might overflow)
            // Double checked locking, safe because reseedCounter is volatile
            if (reseedCounter < 0 || reseedCounter > reseedInterval || pr) {
                synchronized (this) {
                    if (reseedCounter < 0 || reseedCounter > reseedInterval
                            || pr) {
                        reseedAlgorithm(getEntropyInput(pr), ai);
                        ai = null;
                    }
                }
            }

            // Step 8, 10: Generate_algorithm
            // Step 9: Unnecessary. reseedCounter only updated after generation
            generateAlgorithm(result, ai);

            // Step 11: Return
        } else {
            throw new IllegalArgumentException("unknown params type:"
                    + params.getClass());
        }
    }

    public final void engineReseed(SecureRandomParameters params) {
        if (debug != null) {
            debug.println(this, "reseed with params");
        }
        if (!supportReseeding) {
            throw new UnsupportedOperationException("Reseed not supported");
        }
        if (params == null) {
            params = DrbgParameters.reseed(predictionResistanceFlag, null);
        }
        if (params instanceof DrbgParameters.Reseed) {
            DrbgParameters.Reseed dp = (DrbgParameters.Reseed) params;

            // 800-90Ar1 9.2: Reseed Process.

            // Step 2: Check prediction_resistance_request
            boolean pr = dp.getPredictionResistance();
            if (!predictionResistanceFlag && pr) {
                throw new IllegalArgumentException("pr not available");
            }

            // Step 3: Check additional_input length
            byte[] ai = dp.getAdditionalInput();
            if (ai != null && ai.length > maxAdditionalInputLength) {
                throw new IllegalArgumentException("ai too long: "
                        + ai.length);
            }
            instantiateIfNecessary(null);

            // Step 4: Get_entropy_input
            // Step 5: Check step 4
            // Step 6-7: Reseed_algorithm
            reseedAlgorithm(getEntropyInput(pr), ai);

            // Step 8: Return
        } else {
            throw new IllegalArgumentException("unknown params type: "
                    + params.getClass());
        }
    }

    /**
     * Returns the given number of seed bytes. A DRBG always uses
     * {@link SeedGenerator} to get an array with full-entropy.
     * <p>
     * The implementation is identical to SHA1PRNG's
     * {@link SecureRandom#engineGenerateSeed}.
     *
     * @param numBytes the number of seed bytes to generate.
     * @return the seed bytes.
     */
    public final byte[] engineGenerateSeed(int numBytes) {
        byte[] b = new byte[numBytes];
        SeedGenerator.generateSeed(b);
        return b;
    }

    /**
     * Reseeds this random object with the given seed. A DRBG always expands
     * or truncates the input to be between {@link #minLength} and
     * {@link #maxLength} and uses it to instantiate or reseed itself
     * (depending on whether the DRBG is instantiated).
     *
     * @param input the seed
     */
    public final synchronized void engineSetSeed(byte[] input) {
        if (debug != null) {
            debug.println(this, "setSeed");
        }
        if (input.length < minLength) {
            input = Arrays.copyOf(input, minLength);
        } else if (input.length > maxLength) {
            input = Arrays.copyOf(input, maxLength);
        }
        if (!instantiated) {
            instantiateIfNecessary(input);
        } else {
            reseedAlgorithm(input, null);
        }
    }

    // get_entropy_input

    private byte[] getEntropyInput(boolean isPr) {
        // Should the 1st arg be minEntropy or minLength?
        //
        // Technically it should be minEntropy, but CtrDRBG
        // (not using derivation function) is so confusing
        // (does it need only strength or seedlen of entropy?)
        // that it's safer to assume minLength. In all other
        // cases minLength is equal to minEntropy.
        return getEntropyInput(minLength, minLength, maxLength, isPr);
    }

    private byte[] getEntropyInput(int minEntropy, int minLength,
                                   int maxLength, boolean pr) {
        if (debug != null) {
            debug.println(this, "getEntropy(" + minEntropy + "," + minLength +
                    "," + maxLength + "," + pr + ")");
        }
        EntropySource esNow = es;
        if (esNow == null) {
            esNow = pr ? SeederHolder.prseeder : SeederHolder.seeder;
        }
        return esNow.getEntropy(minEntropy, minLength, maxLength, pr);
    }

    // Defaults

    /**
     * The default {@code EntropySource} determined by system property
     * "java.security.egd" or security property "securerandom.source".
     * <p>
     * This object uses {@link SeedGenerator#generateSeed(byte[])} to
     * return a byte array containing {@code minLength} bytes. It is
     * assumed to support prediction resistance and always contains
     * full-entropy.
     */
    private static final EntropySource defaultES =
            (minE, minLen, maxLen, pr) -> {
        byte[] result = new byte[minLen];
        SeedGenerator.generateSeed(result);
        return result;
    };

    private static class SeederHolder {

        /**
         * Default EntropySource for SecureRandom with prediction resistance,
         */
        static final EntropySource prseeder;

        /**
         * Default EntropySource for SecureRandom without prediction resistance,
         * which is backed by a DRBG whose EntropySource is {@link #prseeder}.
         */
        static final EntropySource seeder;

        static {
            prseeder = defaultES;
            // According to SP800-90C section 7, a DRBG without live
            // entropy (drbg here, with pr being false) can instantiate
            // another DRBG with weaker strength. So we choose highest
            // strength we support.
            HashDrbg first = new HashDrbg(new MoreDrbgParameters(
                    prseeder, null, "SHA-256", null, false,
                    DrbgParameters.instantiation(
                            256, NONE,
                            SeedGenerator.getSystemEntropy())));
            seeder = (entropy, minLen, maxLen, pr) -> {
                if (pr) {
                    // This SEI does not support pr
                    throw new IllegalArgumentException("pr not supported");
                }
                byte[] result = new byte[minLen];
                first.engineNextBytes(result);
                return result;
            };
        }
    }

    // Constructor called by overridden methods, initializer...

    /**
     * A constructor without argument so that an implementation does not
     * need to always write {@code super(params)}.
     */
    protected AbstractDrbg() {
        // Nothing
    }

    /**
     * A mechanism shall override this constructor to setup {@link #mechName},
     * {@link #highestSupportedSecurityStrength},
     * {@link #supportPredictionResistance}, {@link #supportReseeding}
     * or other features like {@link #DEFAULT_STRENGTH}. Finally it shall
     * call {@link #configure} on {@code params}.
     *
     * @param params the {@link SecureRandomParameters} object.
     *               This argument can be {@code null}.
     * @throws IllegalArgumentException if {@code params} is
     *         inappropriate for this SecureRandom.
     */
    protected AbstractDrbg(SecureRandomParameters params) {
        // Nothing
    }

    /**
     * Returns the current configuration as a {@link DrbgParameters.Instantiation}
     * object.
     *
     * @return the curent configuration
     */
    protected SecureRandomParameters engineGetParameters() {
        // Or read from variable.
        return DrbgParameters.instantiation(
                securityStrength,
                predictionResistanceFlag ? PR_AND_RESEED :
                        (supportReseeding ? RESEED_ONLY : NONE),
                personalizationString);
    }

    /**
     * Configure this DRBG. This method calls
     * {@link #chooseAlgorithmAndStrength()} and {@link #initEngine()}
     * but does not do the actual instantiation.
     *
     * @param params configuration, if null, default configuration (default
     *               strength, pr_false, no personalization string) is used.
     * @throws IllegalArgumentException if {@code params} is
     *         inappropriate for this SecureRandom.
     */
    protected final void configure(SecureRandomParameters params) {
        if (debug != null) {
            debug.println(this, "configure " + this + " with " + params);
        }
        if (params == null) {
            params = DrbgParameters.instantiation(-1, RESEED_ONLY, null);
        }
        if (params instanceof MoreDrbgParameters) {
            MoreDrbgParameters m = (MoreDrbgParameters)params;
            this.requestedNonce = m.nonce;
            this.es = m.es;
            this.requestedAlgorithm = m.algorithm;
            this.usedf = m.usedf;
            params = DrbgParameters.instantiation(m.strength,
                    m.capability, m.personalizationString);
        }
        if (params != null) {
            if (params instanceof DrbgParameters.Instantiation) {
                DrbgParameters.Instantiation inst =
                        (DrbgParameters.Instantiation) params;

                // 800-90Ar1 9.1: Instantiate Process. Steps 1-5.

                // Step 1: Check requested_instantiation_security_strength
                if (inst.getStrength() > highestSupportedSecurityStrength) {
                    throw new IllegalArgumentException("strength too big: "
                            + inst.getStrength());
                }

                // Step 2: Check prediction_resistance_flag
                if (inst.getCapability().supportsPredictionResistance()
                        && !supportPredictionResistance) {
                    throw new IllegalArgumentException("pr not supported");
                }

                // Step 3: Check personalization_string
                byte[] ps = inst.getPersonalizationString();
                if (ps != null && ps.length > maxPersonalizationStringLength) {
                    throw new IllegalArgumentException("ps too long: "
                            + ps.length);
                }

                if (inst.getCapability().supportsReseeding()
                        && !supportReseeding) {
                    throw new IllegalArgumentException("reseed not supported");
                }
                this.personalizationString = ps;
                this.predictionResistanceFlag =
                        inst.getCapability().supportsPredictionResistance();
                this.requestedInstantiationSecurityStrength = inst.getStrength();
            } else {
                throw new IllegalArgumentException("unknown params: "
                        + params.getClass());
            }
        }

        // Step 4: Set security_strength
        chooseAlgorithmAndStrength();
        instantiated = false;

        // Step 5: no-op.

        if (debug != null) {
            debug.println(this, "configured " + this);
        }
    }

    /**
     * Instantiate if necessary,
     *
     * @param entropy a user-provided entropy, the length is already good.
     *                If null, will fetch entropy input automatically.
     */
    private synchronized void instantiateIfNecessary(byte[] entropy) {
        if (!instantiated) {

            // 800-90Ar1 9.1: Instantiate Process. Steps 6-12.

            // Step 6: Get_entropy_input
            // Step 7: check error (getEntropyInput throw no exception now)
            if (entropy == null) {
                entropy = getEntropyInput(predictionResistanceFlag);
            }

            // Step 8. nonce
            if (requestedNonce != null) {
                nonce = requestedNonce;
            } else {
                nonce = NonceProvider.next();
            }
            initEngine();

            // Step 9-11: Instantiate_algorithm
            instantiateAlgorithm(entropy);
            instantiated = true;

            // Step 12: Return
        }
    }

    // Nonce provider

    private static class NonceProvider {

        // 128 bits of nonce can be used by 256-bit strength DRBG
        private static final byte[] block = new byte[16];

        private static synchronized byte[] next() {
            int k = 15;
            while ((k >= 0) && (++block[k] == 0)) {
                k--;
            }
            return block.clone();
        }
    }

    // Misc

    /**
     * Returns the smallest standard strength (112, 128, 192, 256) that is
     * greater or equal to the input.
     *
     * @param input the input strength
     * @return the standard strength
     */
    protected static int getStandardStrength(int input) {
        if (input <= 112) return 112;
        if (input <= 128) return 128;
        if (input <= 192) return 192;
        if (input <= 256) return 256;
        throw new IllegalArgumentException("input too big: " + input);
    }

    @Override
    public String toString() {
        return mechName + ","  + algorithm
                + "," + securityStrength + ","
                + (predictionResistanceFlag ? "pr_and_reseed"
                        : (supportReseeding ? "reseed_only" : "none"));
    }
}
