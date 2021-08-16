/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec;

import java.security.AccessController;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.ProviderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import sun.security.ec.ed.EdDSAAlgorithmParameters;
import sun.security.ec.ed.EdDSAKeyFactory;
import sun.security.ec.ed.EdDSAKeyPairGenerator;
import sun.security.ec.ed.EdDSASignature;
import sun.security.util.CurveDB;
import sun.security.util.KnownOIDs;
import sun.security.util.NamedCurve;

import static sun.security.util.SecurityConstants.PROVIDER_VER;
import static sun.security.util.SecurityProviderConstants.*;

/**
 * Provider class for the Elliptic Curve provider.
 */
public final class SunEC extends Provider {

    private static final long serialVersionUID = -2279741672933606418L;

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
                    ("constructorParameter not used with " + type + " engines");
            }

            String algo = getAlgorithm();
            try {
                if (type.equals("Signature")) {

                    if (algo.equalsIgnoreCase("EdDSA")) {
                        return new EdDSASignature();
                    } else if (algo.equalsIgnoreCase("Ed25519")) {
                        return new EdDSASignature.Ed25519();
                    } else if (algo.equalsIgnoreCase("Ed448")) {
                        return new EdDSASignature.Ed448();
                    }

                    boolean inP1363 = algo.endsWith("inP1363Format");
                    if (inP1363) {
                        algo = algo.substring(0, algo.length() - 13);
                    }
                    if (algo.equals("SHA1withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA1inP1363Format() :
                            new ECDSASignature.SHA1());
                    } else if (algo.equals("SHA224withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA224inP1363Format() :
                            new ECDSASignature.SHA224());
                    } else if (algo.equals("SHA256withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA256inP1363Format() :
                            new ECDSASignature.SHA256());
                    } else if (algo.equals("SHA384withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA384inP1363Format() :
                            new ECDSASignature.SHA384());
                    } else if (algo.equals("SHA512withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA512inP1363Format() :
                            new ECDSASignature.SHA512());
                    } else if (algo.equals("NONEwithECDSA")) {
                        return (inP1363? new ECDSASignature.RawinP1363Format() :
                            new ECDSASignature.Raw());
                    } else if (algo.equals("SHA3-224withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA3_224inP1363Format() :
                            new ECDSASignature.SHA3_224());
                    } else if (algo.equals("SHA3-256withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA3_256inP1363Format() :
                            new ECDSASignature.SHA3_256());
                    } else if (algo.equals("SHA3-384withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA3_384inP1363Format() :
                            new ECDSASignature.SHA3_384());
                    } else if (algo.equals("SHA3-512withECDSA")) {
                        return (inP1363? new ECDSASignature.SHA3_512inP1363Format() :
                            new ECDSASignature.SHA3_512());
                     }
                } else if (type.equals("KeyFactory")) {
                    if (algo.equals("EC")) {
                        return new ECKeyFactory();
                    } else if (algo.equals("XDH")) {
                        return new XDHKeyFactory();
                    } else if (algo.equals("X25519")) {
                        return new XDHKeyFactory.X25519();
                    } else if (algo.equals("X448")) {
                        return new XDHKeyFactory.X448();
                    } else if (algo.equalsIgnoreCase("EdDSA")) {
                        return new EdDSAKeyFactory();
                    } else if (algo.equalsIgnoreCase("Ed25519")) {
                        return new EdDSAKeyFactory.Ed25519();
                    } else if (algo.equalsIgnoreCase("Ed448")) {
                        return new EdDSAKeyFactory.Ed448();
                    }
                } else  if (type.equals("AlgorithmParameters")) {
                    if (algo.equals("EC")) {
                        return new sun.security.util.ECParameters();
                    }
                } else  if (type.equals("KeyPairGenerator")) {
                    if (algo.equals("EC")) {
                        return new ECKeyPairGenerator();
                    } else if (algo.equals("XDH")) {
                        return new XDHKeyPairGenerator();
                    } else if (algo.equals("X25519")) {
                        return new XDHKeyPairGenerator.X25519();
                    } else if (algo.equals("X448")) {
                        return new XDHKeyPairGenerator.X448();
                    } else if (algo.equalsIgnoreCase("EdDSA")) {
                        return new EdDSAKeyPairGenerator();
                    } else if (algo.equalsIgnoreCase("Ed25519")) {
                        return new EdDSAKeyPairGenerator.Ed25519();
                    } else if (algo.equalsIgnoreCase("Ed448")) {
                        return new EdDSAKeyPairGenerator.Ed448();
                    }
                } else  if (type.equals("KeyAgreement")) {
                    if (algo.equals("ECDH")) {
                        return new ECDHKeyAgreement();
                    } else if (algo.equals("XDH")) {
                        return new XDHKeyAgreement();
                    } else if (algo.equals("X25519")) {
                        return new XDHKeyAgreement.X25519();
                    } else if (algo.equals("X448")) {
                        return new XDHKeyAgreement.X448();
                    }
                }
            } catch (Exception ex) {
                throw new NoSuchAlgorithmException("Error constructing " +
                    type + " for " + algo + " using SunEC", ex);
            }
            throw new ProviderException("No impl for " + algo +
                " " + type);
        }
    }

    @SuppressWarnings("removal")
    public SunEC() {
        super("SunEC", PROVIDER_VER, "Sun Elliptic Curve provider");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                putEntries();
                return null;
            }
        });
    }

    void putEntries() {
        HashMap<String, String> ATTRS = new HashMap<>(3);
        ATTRS.put("ImplementedIn", "Software");
        String ecKeyClasses = "java.security.interfaces.ECPublicKey" +
                 "|java.security.interfaces.ECPrivateKey";
        ATTRS.put("SupportedKeyClasses", ecKeyClasses);
        ATTRS.put("KeySize", "256");

        /*
         *  Key Factory engine
         */
        putService(new ProviderService(this, "KeyFactory",
            "EC", "sun.security.ec.ECKeyFactory",
            List.of("EllipticCurve"), ATTRS));

        /*
         * Algorithm Parameter engine
         */
        // "AlgorithmParameters.EC SupportedCurves" prop used by unit test
        boolean firstCurve = true;
        StringBuilder names = new StringBuilder();

        for (NamedCurve namedCurve :
            List.of(
                CurveDB.lookup("secp256r1"),
                CurveDB.lookup("secp384r1"),
                CurveDB.lookup("secp521r1"))) {
            if (!firstCurve) {
                names.append("|");
            } else {
                firstCurve = false;
            }

            names.append("[");
            String[] commonNames = namedCurve.getNameAndAliases();
            for (String commonName : commonNames) {
                names.append(commonName);
                names.append(",");
            }

            names.append(namedCurve.getObjectId());
            names.append("]");
        }

        HashMap<String, String> apAttrs = new HashMap<>(ATTRS);
        apAttrs.put("SupportedCurves", names.toString());

        putService(new ProviderServiceA(this, "AlgorithmParameters",
            "EC", "sun.security.util.ECParameters", apAttrs));

        putXDHEntries();
        putEdDSAEntries();

        /*
         * Signature engines
         */
        putService(new ProviderService(this, "Signature",
            "NONEwithECDSA", "sun.security.ec.ECDSASignature$Raw",
            null, ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA1withECDSA", "sun.security.ec.ECDSASignature$SHA1",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA224withECDSA", "sun.security.ec.ECDSASignature$SHA224",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA256withECDSA", "sun.security.ec.ECDSASignature$SHA256",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA384withECDSA", "sun.security.ec.ECDSASignature$SHA384",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA512withECDSA", "sun.security.ec.ECDSASignature$SHA512",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA3-224withECDSA", "sun.security.ec.ECDSASignature$SHA3_224",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA3-256withECDSA", "sun.security.ec.ECDSASignature$SHA3_256",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA3-384withECDSA", "sun.security.ec.ECDSASignature$SHA3_384",
            ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "SHA3-512withECDSA", "sun.security.ec.ECDSASignature$SHA3_512",
            ATTRS));

        putService(new ProviderService(this, "Signature",
             "NONEwithECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$RawinP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA1withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA1inP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA224withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA224inP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA256withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA256inP1363Format"));
        putService(new ProviderService(this, "Signature",
            "SHA384withECDSAinP1363Format",
            "sun.security.ec.ECDSASignature$SHA384inP1363Format"));
        putService(new ProviderService(this, "Signature",
            "SHA512withECDSAinP1363Format",
            "sun.security.ec.ECDSASignature$SHA512inP1363Format"));

        putService(new ProviderService(this, "Signature",
             "SHA3-224withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA3_224inP1363Format"));
        putService(new ProviderService(this, "Signature",
             "SHA3-256withECDSAinP1363Format",
             "sun.security.ec.ECDSASignature$SHA3_256inP1363Format"));
        putService(new ProviderService(this, "Signature",
            "SHA3-384withECDSAinP1363Format",
            "sun.security.ec.ECDSASignature$SHA3_384inP1363Format"));
        putService(new ProviderService(this, "Signature",
            "SHA3-512withECDSAinP1363Format",
            "sun.security.ec.ECDSASignature$SHA3_512inP1363Format"));

        /*
         *  Key Pair Generator engine
         */
        putService(new ProviderService(this, "KeyPairGenerator",
            "EC", "sun.security.ec.ECKeyPairGenerator",
            List.of("EllipticCurve"), ATTRS));

        /*
         * Key Agreement engine
         */
        putService(new ProviderService(this, "KeyAgreement",
            "ECDH", "sun.security.ec.ECDHKeyAgreement", null, ATTRS));
    }

    private void putXDHEntries() {

        HashMap<String, String> ATTRS = new HashMap<>(1);
        ATTRS.put("ImplementedIn", "Software");

        putService(new ProviderService(this, "KeyFactory",
            "XDH", "sun.security.ec.XDHKeyFactory", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyFactory",
            "X25519", "sun.security.ec.XDHKeyFactory.X25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyFactory",
            "X448", "sun.security.ec.XDHKeyFactory.X448",
            ATTRS));

        putService(new ProviderService(this, "KeyPairGenerator",
            "XDH", "sun.security.ec.XDHKeyPairGenerator", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyPairGenerator",
            "X25519", "sun.security.ec.XDHKeyPairGenerator.X25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyPairGenerator",
            "X448", "sun.security.ec.XDHKeyPairGenerator.X448",
            ATTRS));

        putService(new ProviderService(this, "KeyAgreement",
            "XDH", "sun.security.ec.XDHKeyAgreement", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyAgreement",
            "X25519", "sun.security.ec.XDHKeyAgreement.X25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyAgreement",
            "X448", "sun.security.ec.XDHKeyAgreement.X448",
            ATTRS));
    }

    private void putEdDSAEntries() {

        HashMap<String, String> ATTRS = new HashMap<>(1);
        ATTRS.put("ImplementedIn", "Software");

        putService(new ProviderService(this, "KeyFactory",
            "EdDSA", "sun.security.ec.ed.EdDSAKeyFactory", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyFactory",
            "Ed25519", "sun.security.ec.ed.EdDSAKeyFactory.Ed25519", ATTRS));
        putService(new ProviderServiceA(this, "KeyFactory",
            "Ed448", "sun.security.ec.ed.EdDSAKeyFactory.Ed448", ATTRS));

        putService(new ProviderService(this, "KeyPairGenerator",
            "EdDSA", "sun.security.ec.ed.EdDSAKeyPairGenerator", null, ATTRS));
        putService(new ProviderServiceA(this, "KeyPairGenerator",
            "Ed25519", "sun.security.ec.ed.EdDSAKeyPairGenerator.Ed25519",
            ATTRS));
        putService(new ProviderServiceA(this, "KeyPairGenerator",
            "Ed448", "sun.security.ec.ed.EdDSAKeyPairGenerator.Ed448",
            ATTRS));

        putService(new ProviderService(this, "Signature",
            "EdDSA", "sun.security.ec.ed.EdDSASignature", null, ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "Ed25519", "sun.security.ec.ed.EdDSASignature.Ed25519", ATTRS));
        putService(new ProviderServiceA(this, "Signature",
            "Ed448", "sun.security.ec.ed.EdDSASignature.Ed448", ATTRS));

    }
}
