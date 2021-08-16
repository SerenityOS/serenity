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

/*
 * @test
 * @bug 8141039
 * @library /lib/testlibrary
 * @summary Check SecureRandom generate expected seed counts what the caller
 *          asked for.
 * @run main/othervm EnoughSeedTest
 */
import java.security.SecureRandom;
import java.security.Security;
import static java.lang.Math.*;

public class EnoughSeedTest {

    private static final String DRBG_CONFIG = "securerandom.drbg.config";
    private static final String DRBG_CONFIG_VALUE
            = Security.getProperty(DRBG_CONFIG);

    public static void main(String[] args) {
        System.setProperty("java.security.egd", "file:/dev/urandom");

        boolean success = true;
        for (String mech : new String[]{
            "SHA1PRNG", "Hash_DRBG", "HMAC_DRBG", "CTR_DRBG"}) {
            System.out.printf("%nTest for SecureRandom algorithm: '%s'", mech);
            try {
                SecureRandom sr = null;
                if (!mech.contains("_DRBG")) {
                    sr = SecureRandom.getInstance(mech);
                } else {
                    Security.setProperty(DRBG_CONFIG, mech);
                    sr = SecureRandom.getInstance("DRBG");
                }

                success &= forEachSeedBytes(sr);
                System.out.printf("%nCompleted test for SecureRandom "
                        + "mechanism: '%s'", mech);
            } catch (Exception e) {
                success &= false;
                e.printStackTrace(System.out);
            } finally {
                Security.setProperty(DRBG_CONFIG, DRBG_CONFIG_VALUE);
            }
        }
        if (!success) {
            throw new RuntimeException("At least one test failed.");
        }
    }

    /**
     * Generates fixed number of seed bytes through a SecureRandom instance
     * to verify it's seed generation status.
     * @param sr SecureRandom instance
     * @return The test success indicator
     */
    private static boolean forEachSeedBytes(SecureRandom sr) {
        boolean success = true;
        sr.setSeed(1l);
        for (int seedByte : new int[]{Integer.MIN_VALUE, -1, 0, 1, 256, 1024,
            Short.MAX_VALUE, (int) pow(2, 20)}) {
            try {
                byte[] seed = sr.generateSeed(seedByte);
                if (seed.length != seedByte) {
                    throw new RuntimeException("Not able to produce expected "
                            + "seed size.");
                }
            } catch (IllegalArgumentException e) {
                if (seedByte >= 0) {
                    throw new RuntimeException("Unknown Exception occured.", e);
                }
                System.out.printf("%nPASS - Exception expected when required "
                        + "seed size requested is negative: %s", seedByte);
            }
        }
        return success;
    }

}
