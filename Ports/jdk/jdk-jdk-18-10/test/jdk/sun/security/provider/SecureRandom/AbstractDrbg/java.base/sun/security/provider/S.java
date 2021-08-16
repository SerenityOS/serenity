/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.security.SecureRandomParameters;
import java.security.SecureRandomSpi;

/**
 * Read ../../../../SpecTest.java for details.
 */
public class S extends SecureRandomSpi {

    protected AbstractDrbg impl;

    // This is not a DRBG.
    public static class S1 extends SecureRandomSpi {
        @Override
        protected void engineSetSeed(byte[] seed) {
        }

        @Override
        protected void engineNextBytes(byte[] bytes) {
        }

        @Override
        protected byte[] engineGenerateSeed(int numBytes) {
            return new byte[numBytes];
        }
    }

    // This is a weak DRBG. maximum strength is 128 and does
    // not support prediction resistance or reseed.
    public static class S2 extends S {
        public S2(SecureRandomParameters params) {
            impl = new Impl2(params);
        }
    }

    // This is a strong DRBG.
    public static class S3 extends S {
        public S3(SecureRandomParameters params) {
            impl = new Impl3(params);
        }
    }

    // AbstractDrbg Implementations

    static class Impl3 extends AbstractDrbg {

        public Impl3(SecureRandomParameters params) {
            supportPredictionResistance = true;
            supportReseeding = true;
            highestSupportedSecurityStrength = 192;
            mechName = "S3";
            algorithm = "SQUEEZE";
            configure(params);
        }

        protected void chooseAlgorithmAndStrength() {
            if (requestedInstantiationSecurityStrength < 0) {
                securityStrength = DEFAULT_STRENGTH;
            } else {
                securityStrength = requestedInstantiationSecurityStrength;
            }
            minLength = securityStrength / 8;
            maxAdditionalInputLength = maxPersonalizationStringLength = 100;
        }

        @Override
        protected void initEngine() {
        }

        @Override
        protected void instantiateAlgorithm(byte[] ei) {
        }

        @Override
        protected void generateAlgorithm(byte[] result, byte[] additionalInput) {
        }

        @Override
        protected void reseedAlgorithm(byte[] ei, byte[] additionalInput) {
        }
    }

    static class Impl2 extends Impl3 {
        public Impl2(SecureRandomParameters params) {
            super(null);
            mechName = "S2";
            highestSupportedSecurityStrength = 128;
            supportPredictionResistance = false;
            supportReseeding = false;
            configure(params);
        }
    }

    // Overridden SecureRandomSpi methods

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
}
