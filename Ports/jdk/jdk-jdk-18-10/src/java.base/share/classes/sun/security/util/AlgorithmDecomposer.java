/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * The class decomposes standard algorithms into sub-elements.
 */
public class AlgorithmDecomposer {

    // '(?<!padd)in': match 'in' but not preceded with 'padd'.
    private static final Pattern PATTERN =
            Pattern.compile("with|and|(?<!padd)in", Pattern.CASE_INSENSITIVE);

    private static Set<String> decomposeImpl(String algorithm) {
        Set<String> elements = new HashSet<>();

        // algorithm/mode/padding
        String[] transTokens = algorithm.split("/");

        for (String transToken : transTokens) {
            if (transToken == null || transToken.isEmpty()) {
                continue;
            }

            // PBEWith<digest>And<encryption>
            // PBEWith<prf>And<encryption>
            // OAEPWith<digest>And<mgf>Padding
            // <digest>with<encryption>
            // <digest>with<encryption>and<mgf>
            // <digest>with<encryption>in<format>
            String[] tokens = PATTERN.split(transToken);

            for (String token : tokens) {
                if (token == null || token.isEmpty()) {
                    continue;
                }

                elements.add(token);
            }
        }
        return elements;
    }

    /**
     * Decompose the standard algorithm name into sub-elements.
     * <p>
     * For example, we need to decompose "SHA1WithRSA" into "SHA1" and "RSA"
     * so that we can check the "SHA1" and "RSA" algorithm constraints
     * separately.
     * <p>
     * Please override the method if need to support more name pattern.
     */
    public Set<String> decompose(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> elements = decomposeImpl(algorithm);

        // In Java standard algorithm name specification, for different
        // purpose, the SHA-1 and SHA-2 algorithm names are different. For
        // example, for MessageDigest, the standard name is "SHA-256", while
        // for Signature, the digest algorithm component is "SHA256" for
        // signature algorithm "SHA256withRSA". So we need to check both
        // "SHA-256" and "SHA256" to make the right constraint checking.

        // handle special name: SHA-1 and SHA1
        if (elements.contains("SHA1") && !elements.contains("SHA-1")) {
            elements.add("SHA-1");
        }
        if (elements.contains("SHA-1") && !elements.contains("SHA1")) {
            elements.add("SHA1");
        }

        // handle special name: SHA-224 and SHA224
        if (elements.contains("SHA224") && !elements.contains("SHA-224")) {
            elements.add("SHA-224");
        }
        if (elements.contains("SHA-224") && !elements.contains("SHA224")) {
            elements.add("SHA224");
        }

        // handle special name: SHA-256 and SHA256
        if (elements.contains("SHA256") && !elements.contains("SHA-256")) {
            elements.add("SHA-256");
        }
        if (elements.contains("SHA-256") && !elements.contains("SHA256")) {
            elements.add("SHA256");
        }

        // handle special name: SHA-384 and SHA384
        if (elements.contains("SHA384") && !elements.contains("SHA-384")) {
            elements.add("SHA-384");
        }
        if (elements.contains("SHA-384") && !elements.contains("SHA384")) {
            elements.add("SHA384");
        }

        // handle special name: SHA-512 and SHA512
        if (elements.contains("SHA512") && !elements.contains("SHA-512")) {
            elements.add("SHA-512");
        }
        if (elements.contains("SHA-512") && !elements.contains("SHA512")) {
            elements.add("SHA512");
        }

        return elements;
    }

    /**
     * Get aliases of the specified algorithm.
     *
     * May support more algorithms in the future.
     */
    public static Collection<String> getAliases(String algorithm) {
        String[] aliases;
        if (algorithm.equalsIgnoreCase("DH") ||
                algorithm.equalsIgnoreCase("DiffieHellman")) {
            aliases = new String[] {"DH", "DiffieHellman"};
        } else {
            aliases = new String[] {algorithm};
        }

        return Arrays.asList(aliases);
    }

    private static void hasLoop(Set<String> elements, String find, String replace) {
        if (elements.contains(find)) {
            if (!elements.contains(replace)) {
                elements.add(replace);
            }
            elements.remove(find);
        }
    }

    /*
     * This decomposes a standard name into sub-elements with a consistent
     * message digest algorithm name to avoid overly complicated checking.
     */
    public static Set<String> decomposeOneHash(String algorithm) {
        if (algorithm == null || algorithm.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> elements = decomposeImpl(algorithm);

        hasLoop(elements, "SHA-1", "SHA1");
        hasLoop(elements, "SHA-224", "SHA224");
        hasLoop(elements, "SHA-256", "SHA256");
        hasLoop(elements, "SHA-384", "SHA384");
        hasLoop(elements, "SHA-512", "SHA512");

        return elements;
    }

    /*
     * The provided message digest algorithm name will return a consistent
     * naming scheme.
     */
    public static String hashName(String algorithm) {
        return algorithm.replace("-", "");
    }
}
