/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6425477 8141039
 * @summary Better support for generation of high entropy random numbers
 * @run main StrongSecureRandom
 */
import java.security.*;
import java.util.*;

/**
 * This test assumes that the standard Sun providers are installed.
 */
public class StrongSecureRandom {

    private static final String os = System.getProperty("os.name", "unknown");

    private static void testDefaultEgd() throws Exception {
        // No SecurityManager installed.
        String s = Security.getProperty("securerandom.source");

        System.out.println("Testing:  default EGD: " + s);
        if (!s.equals("file:/dev/random")) {
            throw new Exception("Default is not 'file:/dev/random'");
        }
    }

    private static void testNativePRNGImpls() throws Exception {
        SecureRandom sr;
        byte[] ba;

        System.out.println("Testing new NativePRNGImpls");

        if (os.startsWith("Windows")) {
            System.out.println("Skip windows testing.");
            return;
        }

        System.out.println("Testing regular");
        sr = SecureRandom.getInstance("NativePRNG");
        if (!sr.getAlgorithm().equals("NativePRNG")) {
            throw new Exception("sr.getAlgorithm(): " + sr.getAlgorithm());
        }
        ba = sr.generateSeed(1);
        sr.nextBytes(ba);
        sr.setSeed(ba);

        System.out.println("Testing NonBlocking");
        sr = SecureRandom.getInstance("NativePRNGNonBlocking");
        if (!sr.getAlgorithm().equals("NativePRNGNonBlocking")) {
            throw new Exception("sr.getAlgorithm(): " + sr.getAlgorithm());
        }
        ba = sr.generateSeed(1);
        sr.nextBytes(ba);
        sr.setSeed(ba);

        if (os.equals("Linux")) {
            System.out.println("Skip Linux blocking test.");
            return;
        }

        System.out.println("Testing Blocking");
        sr = SecureRandom.getInstance("NativePRNGBlocking");
        if (!sr.getAlgorithm().equals("NativePRNGBlocking")) {
            throw new Exception("sr.getAlgorithm(): " + sr.getAlgorithm());
        }
        ba = sr.generateSeed(1);
        sr.nextBytes(ba);
        sr.setSeed(ba);
    }

    private static void testStrongInstance(boolean expected) throws Exception {

        boolean result;

        try {
            SecureRandom.getInstanceStrong();
            result = true;
        } catch (NoSuchAlgorithmException e) {
            result = false;
        }

        if (expected != result) {
            throw new Exception("Received: " + result);
        }
    }

    /*
     * This test assumes that the standard providers are installed.
     */
    private static void testProperty(String property, boolean expected)
            throws Exception {

        System.out.println("Testing: '" + property + "' " + expected);
        final String origStrongAlgoProp
                = Security.getProperty("securerandom.strongAlgorithms");
        try {
            Security.setProperty("securerandom.strongAlgorithms", property);
            testStrongInstance(expected);
        } finally {
            Security.setProperty(
                    "securerandom.strongAlgorithms", origStrongAlgoProp);
        }
    }

    private static void testProperties() throws Exception {
        // Sets securerandom.strongAlgorithms, and then tests various combos.
        testProperty("", false);

        testProperty("SHA1PRNG", true);
        testProperty(" SHA1PRNG", true);
        testProperty("SHA1PRNG ", true);
        testProperty(" SHA1PRNG ", true);

        // Impls are case-insenstive, providers are sensitive.
        testProperty("SHA1PRNG:SUN", true);
        testProperty("Sha1PRNG:SUN", true);
        testProperty("SHA1PRNG:Sun", false);

        testProperty(" SHA1PRNG:SUN", true);
        testProperty("SHA1PRNG:SUN ", true);
        testProperty(" SHA1PRNG:SUN ", true);

        testProperty(" SHA1PRNG:SUn", false);
        testProperty("SHA1PRNG:SUn ", false);
        testProperty(" SHA1PRNG:SUn ", false);

        testProperty(",,,SHA1PRNG", true);
        testProperty(",,, SHA1PRNG", true);
        testProperty(" , , ,SHA1PRNG ", true);

        testProperty(",,,, SHA1PRNG ,,,", true);
        testProperty(",,,, SHA1PRNG:SUN ,,,", true);
        testProperty(",,,, SHA1PRNG:SUn ,,,", false);

        testProperty(",,,SHA1PRNG:Sun,, SHA1PRNG:SUN", true);
        testProperty(",,,Sha1PRNG:Sun, SHA1PRNG:SUN", true);
        testProperty(" SHA1PRNG:Sun, Sha1PRNG:Sun,,,,Sha1PRNG:SUN", true);

        testProperty(",,,SHA1PRNG:Sun,, SHA1PRNG:SUn", false);
        testProperty(",,,Sha1PRNG:Sun, SHA1PRNG:SUn", false);
        testProperty(" SHA1PRNG:Sun, Sha1PRNG:Sun,,,,Sha1PRNG:SUn", false);

        testProperty(
                " @#%,%$#:!%^, NativePRNG:Sun, Sha1PRNG:Sun,,Sha1PRNG:SUN",
                true);
        testProperty(" @#%,%$#!%^, NativePRNG:Sun, Sha1PRNG:Sun,,Sha1PRNG:SUn",
                false);
    }

    /*
     * Linux tends to block, so ignore anything that reads /dev/random.
     */
    private static void handleLinuxRead(SecureRandom sr) throws Exception {
        if (os.equals("Linux")) {
            if (!sr.getAlgorithm().equalsIgnoreCase("NativePRNGBlocking")) {
                sr.nextBytes(new byte[34]);
            }
        } else {
            sr.nextBytes(new byte[34]);
            sr.generateSeed(34);
            sr.setSeed(new byte[34]);
        }
    }

    /*
     * This is duplicating stuff above, but just iterate over all impls
     * just in case we missed something.
     */
    private static void testAllImpls() throws Exception {
        System.out.print("Testing:  AllImpls:  ");

        Iterator<String> i = Security.getAlgorithms("SecureRandom").iterator();

        while (i.hasNext()) {
            String s = i.next();
            System.out.print("/" + s);
            SecureRandom sr = SecureRandom.getInstance(s);

            handleLinuxRead(sr);
            handleLinuxRead(sr);
        }
        System.out.println("/");
    }

    public static void main(String args[]) throws Exception {
        testDefaultEgd();

        testNativePRNGImpls();
        testAllImpls();

        // test default.
        testStrongInstance(true);
        testProperties();
    }
}
