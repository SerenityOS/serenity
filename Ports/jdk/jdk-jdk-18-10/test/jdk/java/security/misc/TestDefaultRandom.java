/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8260274
 * @run main/othervm TestDefaultRandom APG 1
 * @run main/othervm TestDefaultRandom APG 2
 * @run main/othervm TestDefaultRandom KPG 1
 * @run main/othervm TestDefaultRandom KPG 2
 * @run main/othervm TestDefaultRandom CIP 1
 * @run main/othervm TestDefaultRandom CIP 2
 * @run main/othervm TestDefaultRandom CIP 3
 * @run main/othervm TestDefaultRandom CIP 4
 * @run main/othervm TestDefaultRandom KA 1
 * @run main/othervm TestDefaultRandom KA 2
 * @run main/othervm TestDefaultRandom KG 1
 * @run main/othervm TestDefaultRandom KG 2
 * @summary Ensure the default SecureRandom impl is used as the javadoc
 * spec stated when none supplied.
 */
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class TestDefaultRandom {

    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            throw new RuntimeException("Error: missing test parameters");
        }

        switch (argv[0]) {
            case "APG" ->
                check(AlgorithmParameterGenerator.getInstance("DSA"), argv[1]);
            case "KPG" ->
                check(KeyPairGenerator.getInstance("RSA"), argv[1]);
            case "CIP" ->
                check(Cipher.getInstance("AES/CBC/NoPadding"), argv[1]);
            case "KA" -> check(KeyAgreement.getInstance("DH"), argv[1]);
            case "KG" -> check(KeyGenerator.getInstance("AES"), argv[1]);
            default -> throw new RuntimeException
                    ("Error: unsupported test type");
        }
    }

    private static void check(Object obj, String testNum) {
        if (obj == null) throw new NullPointerException();

        SampleProvider prov = new SampleProvider();
        Security.insertProviderAt(prov, 1);

        int b4Cnt = SampleProvider.count;

        System.out.println("before, count = " + b4Cnt);
        // Note that the arguments may not be valid, they just need to be
        // non-null to trigger the call for checking if the default
        // SecureRandom impl is used
        try {
            if (obj instanceof AlgorithmParameterGenerator) {
                AlgorithmParameterGenerator apg =
                        (AlgorithmParameterGenerator) obj;
                switch (testNum) {
                    case "1" -> apg.init(123);
                    case "2" -> apg.init((AlgorithmParameterSpec) null);
                    default -> throw new RuntimeException
                            ("Error: invalid test#");
                }
            } else if (obj instanceof KeyPairGenerator) {
                KeyPairGenerator kpg = (KeyPairGenerator) obj;
                switch (testNum) {
                    case "1" -> kpg.initialize(123);
                    case "2" -> kpg.initialize((AlgorithmParameterSpec) null);
                    default -> throw new RuntimeException
                            ("Error: invalid test#");
                }
            } else if (obj instanceof Cipher) {
                Cipher c = (Cipher) obj;
                switch (testNum) {
                    case "1" -> c.init(Cipher.ENCRYPT_MODE, (Key) null);
                    case "2" -> c.init(Cipher.ENCRYPT_MODE, (Key) null,
                        (AlgorithmParameterSpec) null);
                    case "3" -> c.init(Cipher.ENCRYPT_MODE, (Key) null,
                        (AlgorithmParameters) null);
                    case "4" -> c.init(Cipher.ENCRYPT_MODE, (Certificate)null);
                    default -> throw new RuntimeException
                            ("Error: invalid test#");
                }
            } else if (obj instanceof KeyAgreement) {
                KeyAgreement ka = (KeyAgreement) obj;
                switch (testNum) {
                    case "1" -> ka.init((Key) null);
                    case "2" -> ka.init((Key) null, (AlgorithmParameterSpec)
                            null);
                    default -> throw new RuntimeException
                            ("Error: invalid test#");
                }
            } else if (obj instanceof KeyGenerator) {
                KeyGenerator kg = (KeyGenerator) obj;
                switch (testNum) {
                    case "1" -> kg.init(123);
                    case "2" -> kg.init((AlgorithmParameterSpec) null);
                    default -> throw new RuntimeException
                            ("Error: invalid test#");
                }
            } else {
                throw new RuntimeException("Error: Unsupported type");
            }
        } catch (GeneralSecurityException | InvalidParameterException e) {
            // expected; ignore
        }
        System.out.println("after, count = " + SampleProvider.count);
        if (SampleProvider.count == b4Cnt) {
            throw new RuntimeException("Test Failed");
        }
    }

    private static class SampleProvider extends Provider {

        static int count = 0;
        static String SR_ALGO = "Custom";

        SampleProvider() {
            super("Sample", "1.0", "test provider with custom SR impl");
            putService(new SampleService(this, "SecureRandom", SR_ALGO,
                    "SampleSecureRandom.class" /* stub class name */,
                    null, null));
        }

        private static class SampleService extends Service {

            SampleService(Provider p, String type, String alg, String cn,
                    List<String> aliases, Map<String,String> attrs) {
                super(p, type, alg, cn, aliases, attrs);
            }

            @Override
            public Object newInstance(Object param)
                    throws NoSuchAlgorithmException {
                String alg = getAlgorithm();
                String type = getType();

                if (type.equals("SecureRandom") && alg.equals(SR_ALGO)) {
                    SampleProvider.count++;
                    return new CustomSR();
                } else {
                    // should never happen
                    throw new NoSuchAlgorithmException("No support for " + alg);
                }
            }
        }

        private static class CustomSR extends SecureRandomSpi {
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
    }
}
