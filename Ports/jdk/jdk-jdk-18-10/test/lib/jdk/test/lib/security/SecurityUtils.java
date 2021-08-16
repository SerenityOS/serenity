/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.security;

import java.io.File;
import java.security.KeyStore;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common library for various security test helper functions.
 */
public final class SecurityUtils {

    private static String getCacerts() {
        String sep = File.separator;
        return System.getProperty("java.home") + sep
                + "lib" + sep + "security" + sep + "cacerts";
    }

    /**
     * Returns the cacerts keystore with the configured CA certificates.
     */
    public static KeyStore getCacertsKeyStore() throws Exception {
        File file = new File(getCacerts());
        if (!file.exists()) {
            return null;
        }
        return KeyStore.getInstance(file, (char[])null);
    }

    /**
     * Removes the specified protocols from the jdk.tls.disabledAlgorithms
     * security property.
     */
    public static void removeFromDisabledTlsAlgs(String... protocols) {
        removeFromDisabledAlgs("jdk.tls.disabledAlgorithms",
                               List.<String>of(protocols));
    }

    private static void removeFromDisabledAlgs(String prop, List<String> algs) {
        String value = Security.getProperty(prop);
        value = Arrays.stream(value.split(","))
                      .map(s -> s.trim())
                      .filter(s -> !algs.contains(s))
                      .collect(Collectors.joining(","));
        Security.setProperty(prop, value);
    }

    /**
     * Removes the specified algorithms from the
     * jdk.xml.dsig.secureValidationPolicy security property. Matches any
     * part of the algorithm URI.
     */
    public static void removeAlgsFromDSigPolicy(String... algs) {
        removeFromDSigPolicy("disallowAlg", List.<String>of(algs));
    }

    private static void removeFromDSigPolicy(String rule, List<String> algs) {
        String value = Security.getProperty("jdk.xml.dsig.secureValidationPolicy");
        value = Arrays.stream(value.split(","))
                      .filter(v -> !v.contains(rule) ||
                              !anyMatch(v, algs))
                      .collect(Collectors.joining(","));
        Security.setProperty("jdk.xml.dsig.secureValidationPolicy", value);
    }

    private static boolean anyMatch(String value, List<String> algs) {
        for (String alg : algs) {
           if (value.contains(alg)) {
               return true;
           }
        }
        return false;
    }

    private SecurityUtils() {}
}
