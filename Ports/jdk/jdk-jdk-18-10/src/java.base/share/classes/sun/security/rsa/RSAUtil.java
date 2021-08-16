/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.rsa;

import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

/**
 * Utility class for SunRsaSign provider.
 * Currently used by RSAKeyPairGenerator and RSAKeyFactory.
 *
 * @since   11
 */
public class RSAUtil {

    public enum KeyType {
        RSA ("RSA", AlgorithmId.RSAEncryption_oid, null),
        PSS ("RSASSA-PSS", AlgorithmId.RSASSA_PSS_oid, PSSParameterSpec.class)
        ;

        final String keyAlgo;
        final ObjectIdentifier oid;
        final Class<? extends AlgorithmParameterSpec> paramSpecCls;

        KeyType(String keyAlgo, ObjectIdentifier oid,
                Class<? extends AlgorithmParameterSpec> paramSpecCls) {
            this.keyAlgo = keyAlgo;
            this.oid = oid;
            this.paramSpecCls = paramSpecCls;
        }

        public static KeyType lookup(String name) throws ProviderException {

            requireNonNull(name, "Key algorithm should not be null");

            // match loosely in order to work with 3rd party providers which
            // may not follow the standard names
            if (name.indexOf("PSS") != -1) {
                return PSS;
            } else if (name.indexOf("RSA") != -1) {
                return RSA;
            } else { // no match
                throw new ProviderException("Unsupported algorithm " + name);
            }
        }
    }

    private static void requireNonNull(Object obj, String msg) {
        if (obj == null) throw new ProviderException(msg);
    }

    public static AlgorithmParameterSpec checkParamsAgainstType(KeyType type,
            AlgorithmParameterSpec paramSpec) throws ProviderException {

        // currently no check for null parameter spec
        // assumption is parameter spec is optional and can be null
        if (paramSpec == null) return null;

        Class<? extends AlgorithmParameterSpec> expCls = type.paramSpecCls;
        if (expCls == null) {
            throw new ProviderException("null params expected for " +
                    type.keyAlgo);
        } else if (!expCls.isInstance(paramSpec)) {
            throw new ProviderException
                    (expCls + " expected for " + type.keyAlgo);
        }
        return paramSpec;
    }

    public static AlgorithmParameters getParams(KeyType type,
            AlgorithmParameterSpec spec) throws ProviderException {

        if (spec == null) return null;

        try {
            AlgorithmParameters params =
                    AlgorithmParameters.getInstance(type.keyAlgo);
            params.init(spec);
            return params;
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException ex) {
            throw new ProviderException(ex);
        }
    }

    public static AlgorithmId createAlgorithmId(KeyType type,
            AlgorithmParameterSpec paramSpec) throws ProviderException {

        checkParamsAgainstType(type, paramSpec);

        ObjectIdentifier oid = type.oid;
        AlgorithmParameters params = getParams(type, paramSpec);
        return new AlgorithmId(oid, params);
    }

    public static AlgorithmParameterSpec getParamSpec(
            AlgorithmParameters params) throws ProviderException {

        if (params == null) return null;

        String algName = params.getAlgorithm();

        KeyType type = KeyType.lookup(algName);
        Class<? extends AlgorithmParameterSpec> specCls = type.paramSpecCls;
        if (specCls == null) {
            throw new ProviderException("No params accepted for " +
                    type.keyAlgo);
        }
        try {
            return params.getParameterSpec(specCls);
        } catch (InvalidParameterSpecException ex) {
            throw new ProviderException(ex);
        }
    }

    public static Object[] getTypeAndParamSpec(AlgorithmId algid)
            throws ProviderException {

        requireNonNull(algid, "AlgorithmId should not be null");

        Object[] result = new Object[2];

        String algName = algid.getName();
        try {
            result[0] = KeyType.lookup(algName);
        } catch (ProviderException pe) {
            // accommodate RSA keys encoded with various RSA signature oids
            // for backward compatibility
            if (algName.indexOf("RSA") != -1) {
                result[0] = KeyType.RSA;
            } else {
                // pass it up
                throw pe;
            }
        }

        result[1] = getParamSpec(algid.getParameters());
        return result;
    }
}
