/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.validator;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.EnumSet;

import sun.security.util.Debug;

/**
 * Policies for distrusting a certificate authority (CA). See the
 * jdk.security.caDistrustPolicies security property for more information.
 */
enum CADistrustPolicy {
    /**
     * Distrust TLS Server certificates anchored by a Symantec root CA and
     * issued after April 16, 2019 (with exceptions for a couple of subordinate
     * CAs, see the jdk.security.caDistrustPolicies definition in the
     * java.security file for more details). If enabled, this policy is
     * currently enforced by the PKIX and SunX509 TrustManager implementations
     * of the SunJSSE provider implementation.
     */
    SYMANTEC_TLS {
        void checkDistrust(String variant, X509Certificate[] chain)
                           throws ValidatorException {
            if (!variant.equals(Validator.VAR_TLS_SERVER)) {
                return;
            }
            SymantecTLSPolicy.checkDistrust(chain);
        }
    };

    /**
     * Checks if the end-entity certificate is distrusted.
     *
     * @param variant the type of certificate being checked
     * @param chain the end-entity's certificate chain. The end entity cert
     *              is at index 0, the trust anchor at index n-1.
     * @throws ValidatorException if the end-entity certificate is distrusted
     */
    abstract void checkDistrust(String variant,
                                X509Certificate[] chain)
                                throws ValidatorException;

    // The policies set in the jdk.security.caDistrustPolicies property.
    static final EnumSet<CADistrustPolicy> POLICIES = parseProperty();
    private static EnumSet<CADistrustPolicy> parseProperty() {
        @SuppressWarnings("removal")
        String property = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                @Override
                public String run() {
                    return Security.getProperty(
                        "jdk.security.caDistrustPolicies");
                }
            });
        EnumSet<CADistrustPolicy> set = EnumSet.noneOf(CADistrustPolicy.class);
        // if property is null or empty, the restrictions are not enforced
        if (property == null || property.isEmpty()) {
            return set;
        }
        String[] policies = property.split(",");
        for (String policy : policies) {
            policy = policy.trim();
            try {
                CADistrustPolicy caPolicy =
                    Enum.valueOf(CADistrustPolicy.class, policy);
                set.add(caPolicy);
            } catch (IllegalArgumentException iae) {
                // ignore unknown values but log it
                Debug debug = Debug.getInstance("certpath");
                if (debug != null) {
                    debug.println("Unknown value for the " +
                                  "jdk.security.caDistrustPolicies property: "
                                  + policy);
                }
            }
        }
        return set;
    }
}
