/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @summary When random number is generated through the a SecureRandom instance
 *          as well from it's serialized instance in the same time then the
 *          generated random numbers should be different when one or both are
 *          reseeded.
 * @run main/othervm -Djava.security.egd=file:/dev/urandom SerializedSeedTest
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import jdk.test.lib.Asserts;

public class SerializedSeedTest {

    private static final byte[] SEED = "seed".getBytes();
    private static final String DRBG_CONFIG = "securerandom.drbg.config";
    private static final String DRBG_CONFIG_VALUE
            = Security.getProperty(DRBG_CONFIG);

    public static void main(String[] args) {
        boolean success = true;

        for (String mech : new String[]{
            "SHA1PRNG", "Hash_DRBG", "HMAC_DRBG", "CTR_DRBG"}) {
            System.out.printf(
                    "%nRunning test for SecureRandom mechanism: '%s'", mech);
            try {
                // Serialize without seed and compare generated random numbers
                // produced through original and serialized instances.
                SecureRandom orig = getSRInstance(mech);
                SecureRandom copy = deserializedCopy(orig);
                System.out.printf("%nSerialize without seed. Generated random"
                        + " numbers should be different.");
                check(orig, copy, false, mech);

                // Serialize after default seed and compare generated random
                // numbers produced through original and serialized instances.
                orig = getSRInstance(mech);
                orig.nextInt(); // Default seeded
                copy = deserializedCopy(orig);
                System.out.printf("%nSerialize after default seed. Generated"
                        + " random numbers should be same till 20-bytes.");
                check(orig, copy, !isDRBG(mech), mech);

                // Serialize after explicit seed and compare generated random
                // numbers produced through original and serialized instances.
                orig = getSRInstance(mech);
                orig.setSeed(SEED); // Explicitly seeded
                copy = deserializedCopy(orig);
                System.out.printf("%nSerialize after explicit seed. Generated "
                        + "random numbers should be same till 20-bytes.");
                check(orig, copy, !isDRBG(mech), mech);

                // Serialize without seed but original is explicitly seeded
                // before generating any random number. Then compare generated
                // random numbers produced through original and serialized
                // instances.
                orig = getSRInstance(mech);
                copy = deserializedCopy(orig);
                orig.setSeed(SEED); // Explicitly seeded
                System.out.printf("%nSerialize without seed. When original is "
                        + "explicitly seeded before generating random numbers,"
                        + " Generated random numbers should be different.");
                check(orig, copy, false, mech);

                // Serialize after default seed but original is explicitly
                // seeded before generating any random number. Then compare
                // generated random numbers produced through original and
                // serialized instances.
                orig = getSRInstance(mech);
                orig.nextInt(); // Default seeded
                copy = deserializedCopy(orig);
                orig.setSeed(SEED); // Explicitly seeded
                System.out.printf("%nSerialize after default seed but original "
                        + "is explicitly seeded before generating random number"
                        + ". Generated random numbers should be different.");
                check(orig, copy, false, mech);

                // Serialize after explicit seed but original is explicitly
                // seeded again before generating random number. Then compare
                // generated random numbers produced through original and
                // serialized instances.
                orig = getSRInstance(mech);
                orig.setSeed(SEED); // Explicitly seeded
                copy = deserializedCopy(orig);
                orig.setSeed(SEED); // Explicitly seeded
                System.out.printf("%nSerialize after explicit seed but "
                        + "original is explicitly seeded again before "
                        + "generating random number. Generated random "
                        + "numbers should be different.");
                check(orig, copy, false, mech);

            } catch (Exception e) {
                e.printStackTrace(System.out);
                success = false;
            } finally {
                Security.setProperty(DRBG_CONFIG, DRBG_CONFIG_VALUE);
            }
            System.out.printf("%n------Completed Test for %s------", mech);
        }

        if (!success) {
            throw new RuntimeException("At least one test failed.");
        }
    }

    /**
     * Find if the mechanism is a DRBG mechanism.
     * @param mech Mechanism name
     * @return True for DRBG mechanism else False
     */
    private static boolean isDRBG(String mech) {
        return mech.contains("_DRBG");
    }

    /**
     * Verify the similarity of random numbers generated though both original
     * as well as deserialized instance.
     */
    private static void check(SecureRandom orig, SecureRandom copy,
            boolean equal, String mech) {
        int o = orig.nextInt();
        int c = copy.nextInt();
        System.out.printf("%nRandom number generated for mechanism: '%s' "
                + "from original instance as: '%s' and from serialized "
                + "instance as: '%s'", mech, o, c);
        if (equal) {
            Asserts.assertEquals(o, c, mech);
        } else {
            Asserts.assertNotEquals(o, c, mech);
        }
    }

    /**
     * Get a copy of SecureRandom instance through deserialization.
     * @param orig Original SecureRandom instance
     * @return Deserialized SecureRandom instance
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static SecureRandom deserializedCopy(SecureRandom orig)
            throws IOException, ClassNotFoundException {
        return deserialize(serialize(orig));
    }

    /**
     * Deserialize the SecureRandom object.
     */
    private static SecureRandom deserialize(byte[] serialized)
            throws IOException, ClassNotFoundException {
        SecureRandom sr = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            sr = (SecureRandom) ois.readObject();
        }
        return sr;
    }

    /**
     * Serialize the given SecureRandom object.
     */
    private static byte[] serialize(SecureRandom sr) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(sr);
            return bos.toByteArray();
        }
    }

    /**
     * Create a SecureRandom instance for a given mechanism.
     */
    private static SecureRandom getSRInstance(String mech)
            throws NoSuchAlgorithmException {
        if (!isDRBG(mech)) {
            return SecureRandom.getInstance(mech);
        } else {
            Security.setProperty(DRBG_CONFIG, mech);
            return SecureRandom.getInstance("DRBG");
        }
    }

}
