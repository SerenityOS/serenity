/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @test
 * @bug 8048356
 * @summary Assert default provider used on all OS for SecureRandom
 */
public class DefaultProvider {

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String WINDOWS = "Windows";

    public static void main(String[] args) throws NoSuchAlgorithmException {
        out.println("Operating System: " + OS_NAME);

        /* Test default provider used with constructor */
        out.println("TEST: Default provider with constructor");
        SecureRandom secureRandom = new SecureRandom();
        String provider = secureRandom.getProvider().getName();
        if (!provider.equals("SUN")) {
            throw new RuntimeException("Unexpected provider name: "
                    + provider);
        }
        out.println("Passed, default provider with constructor: " + provider);

        /* Test default provider with getInstance(String algorithm) */
        out.println("TEST: SHA1PRNG supported on all platforms by SUN provider");
        String algorithm = "SHA1PRNG";
        provider = "SUN";

        SecureRandom instance = SecureRandom.getInstance(algorithm);
        assertInstance(instance, algorithm, provider);
        out.println("Passed.");

        if (!OS_NAME.startsWith(WINDOWS)) {
            out.println("TEST: NativePRNG supported on all platforms"
                    + "(except Windows), by SUN provider");
            algorithm = "NativePRNG";
            provider = "SUN";
        } else {
            out.println(
                    "TEST: Windows-PRNG supported on windows by SunMSCAPI provider");
            algorithm = "Windows-PRNG";
            provider = "SunMSCAPI";
        }
        instance = SecureRandom.getInstance(algorithm);
        assertInstance(instance, algorithm, provider);
        out.println("Passed.");
    }

    private static void assertInstance(SecureRandom instance,
            String expectedAlgorithm,
            String expectedProvider) {
        if (instance != null) {
            if (!expectedAlgorithm.equalsIgnoreCase(instance.getAlgorithm())) {
                throw new RuntimeException("Expected algorithm:"
                        + expectedAlgorithm + " actual: " + instance.getAlgorithm());
            }

            if (!expectedProvider.equalsIgnoreCase(instance.getProvider().getName())) {
                throw new RuntimeException("Expected provider: "
                        + expectedProvider + " actual: "
                        + instance.getProvider().getName());
            }
        } else {
            throw new RuntimeException("Secure instance is not created");
        }
    }
}
