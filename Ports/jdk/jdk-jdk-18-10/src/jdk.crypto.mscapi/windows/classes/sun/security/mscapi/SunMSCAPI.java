/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.mscapi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidParameterException;
import java.security.ProviderException;
import java.util.HashMap;
import java.util.List;

import static sun.security.util.SecurityConstants.PROVIDER_VER;
import static sun.security.util.SecurityProviderConstants.getAliases;

/**
 * A Cryptographic Service Provider for the Microsoft Crypto API.
 *
 * @since 1.6
 */

public final class SunMSCAPI extends Provider {

    private static final long serialVersionUID = 8622598936488630849L; //TODO

    private static final String INFO = "Sun's Microsoft Crypto API provider";

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                System.loadLibrary("sunmscapi");
                return null;
            }
        });
    }
    private static class ProviderServiceA extends ProviderService {
        ProviderServiceA(Provider p, String type, String algo, String cn,
                HashMap<String, String> attrs) {
            super(p, type, algo, cn, getAliases(algo), attrs);
        }
    }

    private static class ProviderService extends Provider.Service {
        ProviderService(Provider p, String type, String algo, String cn) {
            super(p, type, algo, cn, null, null);
        }

        ProviderService(Provider p, String type, String algo, String cn,
            List<String> aliases, HashMap<String, String> attrs) {
            super(p, type, algo, cn, aliases, attrs);
        }

        @Override
        public Object newInstance(Object ctrParamObj)
            throws NoSuchAlgorithmException {
            String type = getType();
            if (ctrParamObj != null) {
                throw new InvalidParameterException
                    ("constructorParameter not used with " + type +
                     " engines");
            }
            String algo = getAlgorithm();
            try {
                if (type.equals("SecureRandom")) {
                    if (algo.equals("Windows-PRNG")) {
                        return new PRNG();
                    }
                } else if (type.equals("KeyStore")) {
                    if (algo.equals("Windows-MY")) {
                        return new CKeyStore.MY();
                    } else if (algo.equals("Windows-ROOT")) {
                        return new CKeyStore.ROOT();
                    }
                } else if (type.equals("Signature")) {
                    if (algo.equals("NONEwithRSA")) {
                        return new CSignature.NONEwithRSA();
                    } else if (algo.equals("SHA1withRSA")) {
                        return new CSignature.SHA1withRSA();
                    } else if (algo.equals("SHA256withRSA")) {
                        return new CSignature.SHA256withRSA();
                    } else if (algo.equals("SHA384withRSA")) {
                        return new CSignature.SHA384withRSA();
                    } else if (algo.equals("SHA512withRSA")) {
                        return new CSignature.SHA512withRSA();
                    } else if (algo.equals("MD5withRSA")) {
                        return new CSignature.MD5withRSA();
                    } else if (algo.equals("MD2withRSA")) {
                        return new CSignature.MD2withRSA();
                    } else if (algo.equals("RSASSA-PSS")) {
                        return new CSignature.PSS();
                    } else if (algo.equals("SHA1withECDSA")) {
                        return new CSignature.SHA1withECDSA();
                    } else if (algo.equals("SHA224withECDSA")) {
                        return new CSignature.SHA224withECDSA();
                    } else if (algo.equals("SHA256withECDSA")) {
                        return new CSignature.SHA256withECDSA();
                    } else if (algo.equals("SHA384withECDSA")) {
                        return new CSignature.SHA384withECDSA();
                    } else if (algo.equals("SHA512withECDSA")) {
                        return new CSignature.SHA512withECDSA();
                    }
                } else if (type.equals("KeyPairGenerator")) {
                    if (algo.equals("RSA")) {
                        return new CKeyPairGenerator.RSA();
                    }
                } else if (type.equals("Cipher")) {
                    if (algo.equals("RSA") ||
                        algo.equals("RSA/ECB/PKCS1Padding")) {
                        return new CRSACipher();
                    }
                }
            } catch (Exception ex) {
                throw new NoSuchAlgorithmException
                    ("Error constructing " + type + " for " +
                    algo + " using SunMSCAPI", ex);
            }
            throw new ProviderException("No impl for " + algo +
                " " + type);
        }
    }

    @SuppressWarnings("removal")
    public SunMSCAPI() {
        super("SunMSCAPI", PROVIDER_VER, INFO);

        final Provider p = this;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                /*
                 * Secure random
                 */
                HashMap<String, String> srattrs = new HashMap<>(1);
                srattrs.put("ThreadSafe", "true");
                putService(new ProviderService(p, "SecureRandom",
                           "Windows-PRNG", "sun.security.mscapi.PRNG",
                           null, srattrs));

                /*
                 * Key store
                 */
                putService(new ProviderService(p, "KeyStore",
                           "Windows-MY", "sun.security.mscapi.CKeyStore$MY"));
                putService(new ProviderService(p, "KeyStore",
                           "Windows-ROOT", "sun.security.mscapi.CKeyStore$ROOT"));

                /*
                 * Signature engines
                 */
                HashMap<String, String> attrs = new HashMap<>(1);
                attrs.put("SupportedKeyClasses", "sun.security.mscapi.CKey");

                // NONEwithRSA must be supplied with a pre-computed message digest.
                // Only the following digest algorithms are supported: MD5, SHA-1,
                // SHA-256, SHA-384, SHA-512 and a special-purpose digest
                // algorithm which is a concatenation of SHA-1 and MD5 digests.
                putService(new ProviderService(p, "Signature",
                           "NONEwithRSA", "sun.security.mscapi.CSignature$NONEwithRSA",
                           null, attrs));
                putService(new ProviderService(p, "Signature",
                           "SHA1withRSA", "sun.security.mscapi.CSignature$SHA1withRSA",
                           null, attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA256withRSA",
                           "sun.security.mscapi.CSignature$SHA256withRSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA384withRSA",
                           "sun.security.mscapi.CSignature$SHA384withRSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA512withRSA",
                           "sun.security.mscapi.CSignature$SHA512withRSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "RSASSA-PSS", "sun.security.mscapi.CSignature$PSS",
                           attrs));
                putService(new ProviderService(p, "Signature",
                           "MD5withRSA", "sun.security.mscapi.CSignature$MD5withRSA",
                           null, attrs));
                putService(new ProviderService(p, "Signature",
                           "MD2withRSA", "sun.security.mscapi.CSignature$MD2withRSA",
                           null, attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA1withECDSA",
                           "sun.security.mscapi.CSignature$SHA1withECDSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA224withECDSA",
                           "sun.security.mscapi.CSignature$SHA224withECDSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA256withECDSA",
                           "sun.security.mscapi.CSignature$SHA256withECDSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA384withECDSA",
                           "sun.security.mscapi.CSignature$SHA384withECDSA",
                           attrs));
                putService(new ProviderServiceA(p, "Signature",
                           "SHA512withECDSA",
                           "sun.security.mscapi.CSignature$SHA512withECDSA",
                           attrs));
                /*
                 * Key Pair Generator engines
                 */
                attrs.clear();
                attrs.put("KeySize", "16384");
                putService(new ProviderService(p, "KeyPairGenerator",
                           "RSA", "sun.security.mscapi.CKeyPairGenerator$RSA",
                           null, attrs));

                /*
                 * Cipher engines
                 */
                attrs.clear();
                attrs.put("SupportedModes", "ECB");
                attrs.put("SupportedPaddings", "PKCS1PADDING");
                attrs.put("SupportedKeyClasses", "sun.security.mscapi.CKey");
                putService(new ProviderService(p, "Cipher",
                           "RSA", "sun.security.mscapi.CRSACipher",
                           null, attrs));
                putService(new ProviderService(p, "Cipher",
                           "RSA/ECB/PKCS1Padding", "sun.security.mscapi.CRSACipher",
                           null, attrs));
                return null;
            }
        });
    }
}
