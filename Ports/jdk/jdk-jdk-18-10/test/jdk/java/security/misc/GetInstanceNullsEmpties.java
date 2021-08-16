/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.*;
import javax.crypto.*;
import javax.net.ssl.*;
import javax.security.auth.login.*;
import java.lang.reflect.*;
import java.util.Arrays;

/*
 * @test
 * @bug 4985694
 * @summary Incomplete spec for most of the getInstances
 */
/**
 * A simple test to see what is being thrown when null Strings are passed
 * to the various getInstance() methods.
 *
 * These tests use various algorithm names that don't exist (e.g. "FOO"
 * Ciphers). Just need something non-null for testing, as the tests will throw
 * exceptions before trying to instantiate a real object.
 */
public class GetInstanceNullsEmpties {

    private static final Provider SUN = Security.getProvider("SUN");

    /*
     * See if there are more than "expected" number of getInstance() methods,
     * which will indicate to developers that this test needs an update.
     */
    private static void checkNewMethods(Class<?> clazz, int expected)
            throws Exception {

        long found = Arrays.stream(clazz.getMethods())
                .filter(name -> name.getName().equals("getInstance"))
                .count();

        if (found != expected) {
            throw new Exception("Number of getInstance() mismatch: "
                    + expected + " expected, " + found + " found");
        }
    }

    /**
     * Main loop.
     */
    public static void main(String[] args) throws Exception {

        /*
         * JCA
         */
        testAlgorithmParameterGenerator();
        testAlgorithmParameters();
        testCertificateFactory();
        testCertPathBuilder();
        testCertPathValidator();
        testCertStore();
        testKeyFactory();
        testKeyPairGenerator();
        testKeyStore();
        testMessageDigest();
        testPolicy();
        testSecureRandom();
        testSignature();

        /*
         * JCE
         */
        testCipher();
        testExemptionMechanism();
        testKeyAgreement();
        testKeyGenerator();
        testMac();
        testSecretKeyFactory();

        /*
         * JSSE
         */
        testKeyManagerFactory();
        testSSLContext();
        testTrustManagerFactory();

        /*
         * JGSS
         *
         * KeyTab.getInstance doesn't take algorithm names, so we'll
         * ignore this one.
         */
        testConfiguration();

        System.out.println("\nTEST PASSED!");
    }

    private static Method getInstance(Class clazz, Class... args)
            throws Exception {
        boolean firstPrinted = false;

        System.out.print("\n" + clazz.getName() + "(");
        for (Class c : args) {
            System.out.print(firstPrinted
                    ? ", " + c.getName() : c.getName());
            firstPrinted = true;
        }
        System.out.println("):");

        return clazz.getMethod("getInstance", args);
    }

    private static void run(Method m, Class expectedException,
            Object... args) throws Exception {

        try {
            m.invoke(null, args);
            throw new Exception("Didn't throw exception");
        } catch (InvocationTargetException ite) {
            Throwable root = ite.getCause();
            if (root instanceof Exception) {
                Exception e = (Exception) root;
                if (expectedException.isInstance(e)) {
                    System.out.print("OK ");
                    return;
                } else {
                    System.out.println(
                        "Unexpected InvocationTargetException!");
                    throw e;
                }
            }
            throw ite;
        }
    }

    /*
     * Constants so lines aren't so long.
     */
    private static final Class STRING = String.class;
    private static final Class PROVIDER = Provider.class;

    private static void testAlgorithmParameterGenerator() throws Exception {
        Class clazz = AlgorithmParameterGenerator.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testAlgorithmParameters() throws Exception {
        Class clazz = AlgorithmParameters.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testCertPathBuilder() throws Exception {
        Class clazz = CertPathBuilder.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testCertPathValidator() throws Exception {
        Class clazz = CertPathValidator.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testCertStore() throws Exception {
        Class clazz = CertStore.class;
        Method m;
        CertStoreParameters csp = () -> null;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING, CertStoreParameters.class);
        run(m, NullPointerException.class, (Object) null, csp);
        run(m, NoSuchAlgorithmException.class, "", csp);

        m = getInstance(clazz, STRING, CertStoreParameters.class, STRING);
        run(m, NullPointerException.class, null, csp, "SUN");
        run(m, NoSuchAlgorithmException.class, "", csp, "SUN");
        run(m, IllegalArgumentException.class, "FOO", csp, null);
        run(m, IllegalArgumentException.class, "FOO", csp, "");

        m = getInstance(clazz, STRING, CertStoreParameters.class, PROVIDER);
        run(m, NullPointerException.class, null, csp, SUN);
        run(m, NoSuchAlgorithmException.class, "", csp, SUN);
        run(m, IllegalArgumentException.class, "FOO", csp, null);
    }

    private static void testCertificateFactory() throws Exception {
        Class clazz = CertificateFactory.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, CertificateException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, CertificateException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, CertificateException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testCipher() throws Exception {
        Class clazz = Cipher.class;
        Method m;

        checkNewMethods(clazz, 3);

        /*
         * Note the Cipher API is spec'd to throw a NoSuchAlgorithmException
         * for a null transformation.
         */
        m = getInstance(clazz, STRING);
        run(m, NoSuchAlgorithmException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NoSuchAlgorithmException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NoSuchAlgorithmException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testConfiguration() throws Exception {
        Class clazz = Configuration.class;
        Method m;
        Configuration.Parameters cp = new Configuration.Parameters() {
        };

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING, Configuration.Parameters.class);
        run(m, NullPointerException.class, (Object) null, cp);
        run(m, NoSuchAlgorithmException.class, "", cp);

        m = getInstance(clazz, STRING, Configuration.Parameters.class, STRING);
        run(m, NullPointerException.class, null, cp, "SUN");
        run(m, NoSuchAlgorithmException.class, "", cp, "SUN");
        run(m, IllegalArgumentException.class, "FOO", cp, null);
        run(m, IllegalArgumentException.class, "FOO", cp, "");

        m = getInstance(clazz, STRING, Configuration.Parameters.class,
                PROVIDER);
        run(m, NullPointerException.class, null, cp, SUN);
        run(m, NoSuchAlgorithmException.class, "", cp, SUN);
        run(m, IllegalArgumentException.class, "FOO", cp, null);
    }

    private static void testExemptionMechanism() throws Exception {
        Class clazz = ExemptionMechanism.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testKeyAgreement() throws Exception {
        Class clazz = KeyAgreement.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testKeyFactory() throws Exception {
        Class clazz = KeyFactory.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testKeyGenerator() throws Exception {
        Class clazz = KeyGenerator.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testKeyManagerFactory() throws Exception {
        Class clazz = KeyManagerFactory.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testKeyPairGenerator() throws Exception {
        Class clazz = KeyPairGenerator.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testKeyStore() throws Exception {
        Class clazz = KeyStore.class;
        Method m;

        /*
         * There are actually two additional getInstance() methods with File
         * as the first parameter.
         */
        checkNewMethods(clazz, 5);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, KeyStoreException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, KeyStoreException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, KeyStoreException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testMac() throws Exception {
        Class clazz = Mac.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testMessageDigest() throws Exception {
        Class clazz = MessageDigest.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testPolicy() throws Exception {
        Class clazz = Policy.class;
        Method m;
        Policy.Parameters pp = new Policy.Parameters() {
        };

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING, Policy.Parameters.class);
        run(m, NullPointerException.class, (Object) null, pp);
        run(m, NoSuchAlgorithmException.class, "", pp);

        m = getInstance(clazz, STRING, Policy.Parameters.class, STRING);
        run(m, NullPointerException.class, null, pp, "SUN");
        run(m, NoSuchAlgorithmException.class, "", pp, "SUN");
        run(m, IllegalArgumentException.class, "FOO", pp, null);
        run(m, IllegalArgumentException.class, "FOO", pp, "");

        m = getInstance(clazz, STRING, Policy.Parameters.class, PROVIDER);
        run(m, NullPointerException.class, null, pp, SUN);
        run(m, NoSuchAlgorithmException.class, "", pp, SUN);
        run(m, IllegalArgumentException.class, "FOO", pp, null);
    }

    private static void testSSLContext() throws Exception {
        Class clazz = SSLContext.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testSecretKeyFactory() throws Exception {
        Class clazz = SecretKeyFactory.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testSecureRandom() throws Exception {
        Class clazz = SecureRandom.class;
        Method m;
        SecureRandomParameters srp = new SecureRandomParameters() {
        };

        checkNewMethods(clazz, 6);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);

        m = getInstance(clazz, STRING, SecureRandomParameters.class);
        run(m, NullPointerException.class, (Object) null, srp);
        run(m, NoSuchAlgorithmException.class, "", srp);

        m = getInstance(clazz, STRING, SecureRandomParameters.class, STRING);
        run(m, NullPointerException.class, null, srp, "SUN");
        run(m, NoSuchAlgorithmException.class, "", srp, "SUN");
        run(m, IllegalArgumentException.class, "FOO", srp, null);
        run(m, IllegalArgumentException.class, "FOO", srp, "");

        m = getInstance(clazz, STRING, SecureRandomParameters.class, PROVIDER);
        run(m, NullPointerException.class, null, srp, SUN);
        run(m, NoSuchAlgorithmException.class, "", srp, SUN);
        run(m, IllegalArgumentException.class, "FOO", srp, null);
    }

    private static void testSignature() throws Exception {
        Class clazz = Signature.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }

    private static void testTrustManagerFactory() throws Exception {
        Class clazz = TrustManagerFactory.class;
        Method m;

        checkNewMethods(clazz, 3);

        m = getInstance(clazz, STRING);
        run(m, NullPointerException.class, (Object) null);
        run(m, NoSuchAlgorithmException.class, "");

        m = getInstance(clazz, STRING, STRING);
        run(m, NullPointerException.class, null, "SUN");
        run(m, NoSuchAlgorithmException.class, "", "SUN");
        run(m, IllegalArgumentException.class, "FOO", null);
        run(m, IllegalArgumentException.class, "FOO", "");

        m = getInstance(clazz, STRING, PROVIDER);
        run(m, NullPointerException.class, null, SUN);
        run(m, NoSuchAlgorithmException.class, "", SUN);
        run(m, IllegalArgumentException.class, "FOO", null);
    }
}
