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

import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.security.Provider;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;

/**
 * @test
 * @bug 8076359 8133151 8145344 8150512 8155847
 * @summary Test the value for new jdk.security.provider.preferred
 *          security property
 * @run main/othervm PreferredProviderTest
 */
public class PreferredProviderTest {

    public void RunTest(String type, String os)
            throws NoSuchAlgorithmException, NoSuchPaddingException {

        String actualProvider = null;
        String preferredProp
                = "AES/GCM/NoPadding:SunJCE, MessageDigest.SHA-256:SUN";
        System.out.printf("%nExecuting test for the platform '%s'%n", os);

        // Try to set the preferred algorithm and Provider and verify
        // the usage of it.
        Security.setProperty(
                "jdk.security.provider.preferred", preferredProp);
        verifyPreferredProviderProperty(os, type, preferredProp);

        verifyDigestProvider(os, type, Arrays.asList(
                new DataTuple("SHA-256", "SUN")));

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        actualProvider = cipher.getProvider().getName();
        if (!actualProvider.equals("SunJCE")) {
            throw new RuntimeException(String.format("Test Failed:Got wrong "
                    + "provider from %s-%s platform, Expected Provider: SunJCE,"
                    + " Returned Provider: %s", os, type, actualProvider));
        }
    }

    private static void verifyPreferredProviderProperty(String os, String arch,
            String preferred) {
        String preferredProvider
                = Security.getProperty("jdk.security.provider.preferred");
        if (!preferredProvider.equals(preferred)) {
            System.out.println("Expected: " + preferred + "\nResult: " +
                    preferredProvider);
            throw new RuntimeException(String.format(
                    "Test Failed: wrong jdk.security.provider.preferred value "
                    + "on %s-%s", os, arch));
        }
        System.out.println(
                "Preferred provider security property verification complete.");
    }

    private static void verifyDigestProvider(String os, String arch,
            List<DataTuple> algoProviders) throws NoSuchAlgorithmException {
        for (DataTuple dataTuple : algoProviders) {
            System.out.printf(
                    "Verifying MessageDigest for '%s'%n", dataTuple.algorithm);
            MessageDigest md = MessageDigest.getInstance(dataTuple.algorithm);
            matchProvider(md.getProvider(), dataTuple.provider,
                    dataTuple.algorithm, os, arch);
        }
        System.out.println(
                "Preferred MessageDigest algorithm verification successful.");
    }

    private static void verifyMacProvider(String os, String arch,
            List<DataTuple> algoProviders) throws NoSuchAlgorithmException {
        for (DataTuple dataTuple : algoProviders) {
            System.out.printf(
                    "Verifying Mac for '%s'%n", dataTuple.algorithm);
            Mac mac = Mac.getInstance(dataTuple.algorithm);
            matchProvider(mac.getProvider(), dataTuple.provider,
                    dataTuple.algorithm, os, arch);
        }
        System.out.println(
                "Preferred Mac algorithm verification successful.");
    }

    private static void verifyKeyFactoryProvider(String os, String arch,
            List<DataTuple> algoProviders) throws NoSuchAlgorithmException {
        for (DataTuple dataTuple : algoProviders) {
            System.out.printf(
                    "Verifying KeyFactory for '%s'%n", dataTuple.algorithm);
            KeyFactory kf = KeyFactory.getInstance(dataTuple.algorithm);
            matchProvider(kf.getProvider(), dataTuple.provider,
                    dataTuple.algorithm, os, arch);
        }
        System.out.println(
                "Preferred KeyFactory algorithm verification successful.");
    }

    private static void verifySignatureProvider(String os, String arch,
            List<DataTuple> algoProviders) throws NoSuchAlgorithmException {
        for (DataTuple dataTuple : algoProviders) {
            System.out.printf(
                    "Verifying Signature for '%s'%n", dataTuple.algorithm);
            Signature si = Signature.getInstance(dataTuple.algorithm);
            matchProvider(si.getProvider(), dataTuple.provider,
                    dataTuple.algorithm, os, arch);
        }
        System.out.println(
                "Preferred Signature algorithm verification successful.");
    }

    private static void matchProvider(Provider provider, String expected,
            String algo, String os, String arch) {
        if (!provider.getName().equals(expected)) {
            throw new RuntimeException(String.format(
                    "Test Failed:Got wrong provider from %s-%s platform, "
                    + "for algorithm %s. Expected Provider: %s,"
                    + " Returned Provider: %s", os, arch, algo,
                    expected, provider.getName()));
        }
    }

    private static class DataTuple {

        private final String provider;
        private final String algorithm;

        private DataTuple(String algorithm, String provider) {
            this.algorithm = algorithm;
            this.provider = provider;
        }
    }

    public static void main(String[] args)
            throws NoSuchAlgorithmException, NoSuchPaddingException {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        PreferredProviderTest pp = new PreferredProviderTest();
        pp.RunTest(arch, os);
    }
}
