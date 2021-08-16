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

import java.io.IOException;
import java.security.AccessController;
import java.security.DrbgParameters;
import java.security.PrivilegedAction;
import java.security.SecureRandomParameters;
import java.security.SecureRandomSpi;
import java.security.Security;
import java.util.Locale;
import static java.security.DrbgParameters.Capability.*;

/**
 * Implement the "SecureRandom.DRBG" algorithm.
 *
 * About the default "securerandom.drbg.config" value:
 *
 * The default value in java.security is set to "". This is because
 * the default values of different aspects are dependent (For example,
 * strength depends on algorithm) and if we write a full string there
 * it will be difficult to modify one and keep all others legal.
 *
 * When changing default values, touch all places including:
 *
 * 1. comments of the security property in java.security
 * 2. Default mech, cap, usedf set in this class
 * 3. Default algorithm set in final implementation of each mech
 * 4. Default strength set in AbstractDrbg, but the effective
 *    value can be smaller if an algorithm does not support it.
 *
 * The default value is also mentioned in the @implNote part of
 * {@link DrbgParameters} class.
 */
public final class DRBG extends SecureRandomSpi {

    private static final String PROP_NAME = "securerandom.drbg.config";

    @java.io.Serial
    private static final long serialVersionUID = 9L;

    private transient AbstractDrbg impl;

    /**
     * @serial
     */
    private final MoreDrbgParameters mdp;

    public DRBG(SecureRandomParameters params) {

        // All parameters at unset status (null or -1).

        // Configurable with the "securerandom.drbg.config" security property
        String mech = null;
        Boolean usedf = null;
        String algorithm = null;

        // Default instantiate parameters also configurable with
        // "securerandom.drbg.config", and can be changed with params
        // in getInstance("drbg", params)
        int strength = -1;
        DrbgParameters.Capability cap = null;
        byte[] ps = null;

        // Not configurable with public interfaces, but is a part of
        // MoreDrbgParameters
        EntropySource es = null;
        byte[] nonce = null;

        // Can be configured with a security property

        @SuppressWarnings("removal")
        String config = AccessController.doPrivileged((PrivilegedAction<String>)
                () -> Security.getProperty(PROP_NAME));

        if (config != null && !config.isEmpty()) {
            for (String part : config.split(",")) {
                part = part.trim();
                switch (part.toLowerCase(Locale.ROOT)) {
                    case "":
                        throw new IllegalArgumentException(
                                "aspect in " + PROP_NAME + " cannot be empty");
                    case "pr_and_reseed":
                        checkTwice(cap != null, "capability");
                        cap = PR_AND_RESEED;
                        break;
                    case "reseed_only":
                        checkTwice(cap != null, "capability");
                        cap = RESEED_ONLY;
                        break;
                    case "none":
                        checkTwice(cap != null, "capability");
                        cap = NONE;
                        break;
                    case "hash_drbg":
                    case "hmac_drbg":
                    case "ctr_drbg":
                        checkTwice(mech != null, "mechanism name");
                        mech = part;
                        break;
                    case "no_df":
                        checkTwice(usedf != null, "usedf flag");
                        usedf = false;
                        break;
                    case "use_df":
                        checkTwice(usedf != null, "usedf flag");
                        usedf = true;
                        break;
                    default:
                        // For all other parts of the property, it is
                        // either an algorithm name or a strength
                        try {
                            int tmp = Integer.parseInt(part);
                            if (tmp < 0) {
                                throw new IllegalArgumentException(
                                        "strength in " + PROP_NAME +
                                                " cannot be negative: " + part);
                            }
                            checkTwice(strength >= 0, "strength");
                            strength = tmp;
                        } catch (NumberFormatException e) {
                            checkTwice(algorithm != null, "algorithm name");
                            algorithm = part;
                        }
                }
            }
        }

        // Can be updated by params

        if (params != null) {
            // MoreDrbgParameters is used for testing.
            if (params instanceof MoreDrbgParameters) {
                MoreDrbgParameters m = (MoreDrbgParameters) params;
                params = DrbgParameters.instantiation(m.strength,
                        m.capability, m.personalizationString);

                // No need to check null for es and nonce, they are still null
                es = m.es;
                nonce = m.nonce;

                if (m.mech != null) {
                    mech = m.mech;
                }
                if (m.algorithm != null) {
                    algorithm = m.algorithm;
                }
                usedf = m.usedf;
            }
            if (params instanceof DrbgParameters.Instantiation) {
                DrbgParameters.Instantiation dp =
                        (DrbgParameters.Instantiation) params;

                // ps is still null by now
                ps = dp.getPersonalizationString();

                int tmp = dp.getStrength();
                if (tmp != -1) {
                    strength = tmp;
                }
                cap = dp.getCapability();
            } else {
                throw new IllegalArgumentException("Unsupported params: "
                        + params.getClass());
            }
        }

        // Hardcoded defaults.
        // Remember to sync with "securerandom.drbg.config" in java.security.

        if (cap == null) {
            cap = NONE;
        }
        if (mech == null) {
            mech = "Hash_DRBG";
        }
        if (usedf == null) {
            usedf = true;
        }

        mdp = new MoreDrbgParameters(
                es, mech, algorithm, nonce, usedf,
                DrbgParameters.instantiation(strength, cap, ps));

        createImpl();
    }

    private void createImpl() {
        switch (mdp.mech.toLowerCase(Locale.ROOT)) {
            case "hash_drbg":
                impl = new HashDrbg(mdp);
                break;
            case "hmac_drbg":
                impl = new HmacDrbg(mdp);
                break;
            case "ctr_drbg":
                impl = new CtrDrbg(mdp);
                break;
            default:
                throw new IllegalArgumentException("Unsupported mech: " + mdp.mech);
        }
    }

    @Override
    protected void engineSetSeed(byte[] seed) {
        impl.engineSetSeed(seed);
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        impl.engineNextBytes(bytes);
    }

    @Override
    protected byte[] engineGenerateSeed(int numBytes) {
        return impl.engineGenerateSeed(numBytes);
    }

    @Override
    protected void engineNextBytes(
            byte[] bytes, SecureRandomParameters params) {
        impl.engineNextBytes(bytes, params);
    }

    @Override
    protected void engineReseed(SecureRandomParameters params) {
        impl.engineReseed(params);
    }

    @Override
    protected SecureRandomParameters engineGetParameters() {
        return impl.engineGetParameters();
    }

    @Override
    public String toString() {
        return impl.toString();
    }

    /**
     * Ensures an aspect is not set more than once.
     *
     * @param flag true if set more than once
     * @param name the name of aspect shown in IAE
     * @throws IllegalArgumentException if it happens
     */
    private static void checkTwice(boolean flag, String name) {
        if (flag) {
            throw new IllegalArgumentException(name
                    + " cannot be provided more than once in " + PROP_NAME);
        }
    }

    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (mdp.mech == null) {
            throw new IllegalArgumentException("Input data is corrupted");
        }
        createImpl();
    }
}
