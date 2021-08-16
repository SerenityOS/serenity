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

import java.security.*;
import java.security.spec.*;
import java.util.*;
import jdk.test.lib.SigTestUtil;
import static jdk.test.lib.SigTestUtil.SignatureType;

/*
 * @test
 * @bug 8050374 8181048 8146293
 * @summary Verify a chain of signed objects
 * @library /test/lib
 * @build jdk.test.lib.SigTestUtil
 * @run main Chain
 */
public class Chain {

    static enum KeyAlg {
        RSA("RSA"),
        DSA("DSA"),
        EC("EC");

        final String name;

        KeyAlg(String alg) {
            this.name = alg;
        }
    }

    static enum Provider {
        Default("default"),
        SunRsaSign("SunRsaSign"),
        Sun("SUN"),
        SunEC("SunEC"),
        SunJSSE("SunJSSE"),
        SunMSCAPI("SunMSCAPI");

        final String name;

        Provider(String name) {
            this.name = name;
        }
    }

    static enum SigAlg {
        MD2withRSA("MD2withRSA"),
        MD5withRSA("md5withRSA"),

        SHA1withDSA("SHA1withDSA"),
        SHA224withDSA("SHA224withDSA"),
        SHA256withDSA("SHA256withDSA"),
        SHA384withDSA("SHA384withDSA"),
        SHA512withDSA("SHA512withDSA"),

        SHA3_224withDSA("SHA3-224withDSA"),
        SHA3_256withDSA("SHA3-256withDSA"),
        SHA3_384withDSA("SHA3-384withDSA"),
        SHA3_512withDSA("SHA3-512withDSA"),

        SHA1withRSA("Sha1withrSA"),
        SHA224withRSA("SHA224withRSA"),
        SHA256withRSA("SHA256withRSA"),
        SHA384withRSA("SHA384withRSA"),
        SHA512withRSA("SHA512withRSA"),
        SHA512_224withRSA("SHA512/224withRSA"),
        SHA512_256withRSA("SHA512/256withRSA"),
        SHA3_224withRSA("SHA3-224withRSA"),
        SHA3_256withRSA("SHA3-256withRSA"),
        SHA3_384withRSA("SHA3-384withRSA"),
        SHA3_512withRSA("SHA3-512withRSA"),

        SHA1withECDSA("SHA1withECDSA"),
        SHA224withECDSA("SHA224withECDSA"),
        SHA256withECDSA("SHA256withECDSA"),
        SHA384withECDSA("SHA384withECDSA"),
        SHA512withECDSA("SHA512withECDSA"),
        SHA3_224withECDSA("SHA3-224withECDSA"),
        SHA3_256withECDSA("SHA3-256withECDSA"),
        SHA3_384withECDSA("SHA3-384withECDSA"),
        SHA3_512withECDSA("SHA3-512withECDSA"),

        MD5andSHA1withRSA("MD5andSHA1withRSA"),

        RSASSA_PSS("RSASSA-PSS");

        final String name;

        SigAlg(String name) {
            this.name = name;
        }
    }

    static class Test {
        final Provider provider;
        final KeyAlg keyAlg;
        final SigAlg sigAlg;
        final int keySize;
        final AlgorithmParameterSpec sigParams;

        Test(SigAlg sigAlg, KeyAlg keyAlg, Provider provider) {
            this(sigAlg, keyAlg, provider, -1, null);
        }

        Test(SigAlg sigAlg, KeyAlg keyAlg, Provider provider, int keySize) {
            this(sigAlg, keyAlg, provider, keySize, null);
        }

        Test(SigAlg sigAlg, KeyAlg keyAlg, Provider provider, int keySize,
                AlgorithmParameterSpec sigParams) {
            this.provider = provider;
            this.keyAlg = keyAlg;
            this.sigAlg = sigAlg;
            this.keySize = keySize;
            this.sigParams = sigParams;
        }

        private static String formatParams(AlgorithmParameterSpec aps) {
            if (aps == null) return "null";
            if (aps instanceof PSSParameterSpec) {
                PSSParameterSpec p = (PSSParameterSpec) aps;
                return String.format("PSSParameterSpec (%s, %s, %s, %s)",
                    p.getDigestAlgorithm(), formatParams(p.getMGFParameters()),
                    p.getSaltLength(), p.getTrailerField());
            } else if (aps instanceof MGF1ParameterSpec) {
                return "MGF1" +
                    ((MGF1ParameterSpec)aps).getDigestAlgorithm();
            } else {
                return aps.toString();
            }
        }

        public String toString() {
            return String.format("Test: provider = %s, signature alg = %s, "
                + " w/ %s, key alg = %s", provider, sigAlg,
                formatParams(sigParams), keyAlg);
        }
    }

    private static final Test[] tests = {
        new Test(SigAlg.SHA1withDSA, KeyAlg.DSA, Provider.Default, 1024),
        new Test(SigAlg.MD2withRSA, KeyAlg.RSA, Provider.Default),
        new Test(SigAlg.MD5withRSA, KeyAlg.RSA, Provider.Default),
        new Test(SigAlg.SHA3_224withRSA, KeyAlg.RSA, Provider.Default),
        new Test(SigAlg.SHA3_256withRSA, KeyAlg.RSA, Provider.Default),
        new Test(SigAlg.SHA3_384withRSA, KeyAlg.RSA, Provider.Default),
        new Test(SigAlg.SHA3_512withRSA, KeyAlg.RSA, Provider.Default),
        new Test(SigAlg.SHA1withDSA, KeyAlg.DSA, Provider.Sun, 1024),
        new Test(SigAlg.SHA224withDSA, KeyAlg.DSA, Provider.Sun, 2048),
        new Test(SigAlg.SHA256withDSA, KeyAlg.DSA, Provider.Sun, 2048),
    };

    private static final String str = "to-be-signed";
    private static final int N = 3;

    public static void main(String argv[]) {
        boolean result = Arrays.stream(tests).allMatch((test) -> runTest(test));
        result &= runTestPSS(2048);
        if (result) {
            System.out.println("All tests passed");
        } else {
            throw new RuntimeException("Some tests failed");
        }
    }

    private static boolean runTestPSS(int keysize) {
        boolean result = true;
        SigAlg pss = SigAlg.RSASSA_PSS;
        Iterator<String> mdAlgs = SigTestUtil.getDigestAlgorithms
            (SignatureType.RSASSA_PSS, keysize).iterator();
        while (mdAlgs.hasNext()) {
            result &= runTest(new Test(pss, KeyAlg.RSA, Provider.SunRsaSign,
                keysize, SigTestUtil.generateDefaultParameter
                    (SignatureType.RSASSA_PSS, mdAlgs.next())));
        }
        return result;
    }

    static boolean runTest(Test test) {
        System.out.println(test);
        try {
            // Generate all private/public key pairs
            PrivateKey[] privKeys = new PrivateKey[N];
            PublicKey[] pubKeys = new PublicKey[N];
            PublicKey[] anotherPubKeys = new PublicKey[N];
            Signature signature;
            KeyPairGenerator kpg;
            if (test.provider != Provider.Default) {
                signature = Signature.getInstance(test.sigAlg.name,
                        test.provider.name);
                // try using the same provider first, if not, fallback
                // to the first available impl
                try {
                    kpg = KeyPairGenerator.getInstance(
                        test.keyAlg.name, test.provider.name);
                } catch (NoSuchAlgorithmException nsae) {
                    kpg = KeyPairGenerator.getInstance(
                        test.keyAlg.name);
                }
            } else {
                signature = Signature.getInstance(test.sigAlg.name);
                kpg = KeyPairGenerator.getInstance(test.keyAlg.name);
            }
            if (test.sigParams != null) {
                signature.setParameter(test.sigParams);
            }

            for (int j=0; j < N; j++) {
                if (test.keySize != -1) {
                    kpg.initialize(test.keySize);
                }
                KeyPair kp = kpg.genKeyPair();
                KeyPair anotherKp = kpg.genKeyPair();
                privKeys[j] = kp.getPrivate();
                pubKeys[j] = kp.getPublic();
                anotherPubKeys[j] = anotherKp.getPublic();

                if (Arrays.equals(pubKeys[j].getEncoded(),
                        anotherPubKeys[j].getEncoded())) {
                    System.out.println("Failed: it should not get "
                            + "the same pair of public key");
                    return false;
                }
            }

            // Create a chain of signed objects
            SignedObject[] objects = new SignedObject[N];
            objects[0] = new SignedObject(str, privKeys[0], signature);
            for (int j = 1; j < N; j++) {
                objects[j] = new SignedObject(objects[j - 1], privKeys[j],
                        signature);
            }

            // Verify the chain
            int n = objects.length - 1;
            SignedObject object = objects[n];
            do {
                if (!object.verify(pubKeys[n], signature)) {
                    System.out.println("Failed: verification failed, n = " + n);
                    return false;
                }
                if (object.verify(anotherPubKeys[n], signature)) {
                    System.out.println("Failed: verification should not "
                            + "succeed with wrong public key, n = " + n);
                    return false;
                }

                object = (SignedObject) object.getObject();
                n--;
            } while (n > 0);

            System.out.println("signed data: " + object.getObject());
            if (!str.equals(object.getObject())) {
                System.out.println("Failed: signed data is not equal to "
                        + "original one");
                return false;
            }

            System.out.println("Test passed");
            return true;
        } catch (NoSuchProviderException nspe) {
            if (test.provider == Provider.SunMSCAPI
                    && !System.getProperty("os.name").startsWith("Windows")) {
                System.out.println("SunMSCAPI is available only on Windows: "
                        + nspe);
                return true;
            }
            System.out.println("Unexpected exception: " + nspe);
            return false;
        } catch (Exception e) {
            System.out.println("Unexpected exception: " + e);
            e.printStackTrace(System.out);
            return false;
        }
    }
}

