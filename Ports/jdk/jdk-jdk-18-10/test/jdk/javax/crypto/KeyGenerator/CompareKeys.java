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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.crypto.KeyGenerator;

/*
 * @test
 * @bug 8185127
 * @summary Test key comparison for the Keys generated through KeyGenerator
 * @run main CompareKeys
 */
public class CompareKeys {

    public static void main(String[] args) throws Exception {

        for (KeygenAlgo alg : getSupportedAlgo("KeyGenerator")) {
            System.out.printf("Verifying provider %s and algorithm %s%n",
                    alg.provider().getName(), alg.algoName());
            SecretKey k = genSecretKey(alg.algoName(), alg.provider());
            checkKeyEquality(k, copy(alg.algoName(), k));
        }

        for (KeygenAlgo alg : getSupportedAlgo("KeyPairGenerator")) {
            System.out.printf("Verifying provider %s and algorithm %s%n",
                    alg.provider().getName(), alg.algoName());
            KeyPair kp = genKeyPair(alg.algoName(), alg.provider());
            checkKeyPairEquality(kp, copy(alg.algoName(), kp));
        }
        System.out.println("Done!");
    }

    @SuppressWarnings("preview")
    private record KeygenAlgo(String algoName, Provider provider) {

    }

    private static void checkKeyPairEquality(KeyPair origkp, KeyPair copykp)
            throws Exception {

        checkKeyEquality(origkp.getPrivate(), copykp.getPrivate());
        checkKeyEquality(origkp.getPublic(), copykp.getPublic());
    }

    /**
     * Compare original Key with another copy.
     */
    private static void checkKeyEquality(Key origKey, Key copyKey) {

        if ((origKey.equals(copyKey)
                && origKey.hashCode() == copyKey.hashCode()
                && Arrays.equals(origKey.getEncoded(), copyKey.getEncoded())
                && origKey.getFormat().equals(copyKey.getFormat()))) {
            System.out.printf("%s equality check Passed%n",
                    origKey.getClass().getName());
        } else {
            System.out.println("Result- equals: "
                    + origKey.equals(copyKey));
            System.out.println("Result- hashCode: "
                    + (origKey.hashCode() == copyKey.hashCode()));
            System.out.println("Result- encoded check: " + Arrays.equals(
                    origKey.getEncoded(), copyKey.getEncoded()));
            System.out.println("Result- format check: "
                    + origKey.getFormat().equals(copyKey.getFormat()));
            throw new RuntimeException("Key inequality found");
        }
    }

    private static Key copy(String algo, Key key) throws Exception {

        return new SecretKeySpec(key.getEncoded(), algo);
    }

    private static KeyPair copy(String algo, KeyPair kp) throws Exception {

        KeyFactory kf = KeyFactory.getInstance(algo);
        return new KeyPair(
                kf.generatePublic(
                        new X509EncodedKeySpec(kp.getPublic().getEncoded())),
                kf.generatePrivate(
                        new PKCS8EncodedKeySpec(kp.getPrivate().getEncoded())));
    }

    private static List<KeygenAlgo> getSupportedAlgo(String type)
            throws Exception {

        List<KeygenAlgo> kgs = new LinkedList<>();
        for (Provider p : Security.getProviders()) {
            for (Provider.Service s : p.getServices()) {
                // Ignore the algorithms from the list which require
                // pre-initialization to make the Test generic across algorithms.
                // SunMSCAPI provider is ignored too because of incompatibilty
                // for serialization and with PKCS8EncodedKeySpec for certain
                // algorithms like RSA.
                if (s.getType().contains(type)
                        && !((s.getAlgorithm().startsWith("SunTls"))
                        || s.getProvider().getName().equals("SunMSCAPI"))) {
                    kgs.add(new KeygenAlgo(s.getAlgorithm(), s.getProvider()));
                }
            }
        }
        return kgs;
    }

    public static SecretKey genSecretKey(String algoName, Provider provider)
            throws Exception {

        return KeyGenerator.getInstance(algoName, provider).generateKey();
    }

    public static KeyPair genKeyPair(String algoName, Provider provider)
            throws Exception {

        return KeyPairGenerator.getInstance(algoName, provider)
                .generateKeyPair();
    }
}
