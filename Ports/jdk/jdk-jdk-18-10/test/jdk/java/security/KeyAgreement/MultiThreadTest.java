/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8184359
 * @summary KeyPairGenerator Test with multiple threads.
 *  Arguments order <KeyExchangeAlgorithm> <Provider> <KeyGenAlgorithm> <Curve*>
 * @run main MultiThreadTest DiffieHellman SunJCE DiffieHellman
 * @run main MultiThreadTest ECDH SunEC EC
 * @run main MultiThreadTest XDH SunEC XDH X25519
 * @run main MultiThreadTest XDH SunEC XDH X448
 */
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.crypto.KeyAgreement;

/**
 * This test targets KeyPairGenerator API related issue in a multi threaded
 * context.
 */
public class MultiThreadTest {

    // Tested a shared KeyPairGenerator with 100 number of threads.
    private static final int THREAD_COUNT = 100;

    public static void main(String[] args) throws Exception {

        String kaAlgo = args[0];
        String provider = args[1];
        String kpgAlgo = args[2];
        KeyPairGenerator kpg = genKeyGenerator(provider, kpgAlgo,
                (args.length > 3) ? args[3] : kpgAlgo);
        new MultiThreadTest().runTest(provider, kaAlgo, kpg);
    }

    /**
     * Initialize KeyPairGenerator based on different algorithm names.
     */
    private static KeyPairGenerator genKeyGenerator(String provider,
            String kpgAlgo, String kpgInit) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(kpgAlgo, provider);
        switch (kpgInit) {
            case "DiffieHellman":
                kpg.initialize(512);
                break;
            case "EC":
                kpg.initialize(256);
                break;
            case "X25519":
                kpg.initialize(255);
                break;
            case "X448":
                kpg.initialize(448);
                break;
            default:
                throw new RuntimeException("Invalid Algo name " + kpgInit);
        }
        return kpg;
    }

    private void runTest(String provider, String kaAlgo, KeyPairGenerator kpg)
            throws Exception {

        ExecutorService executor = null;
        try {
            executor = Executors.newCachedThreadPool(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            testKeyAgreement(provider, kaAlgo, kpg);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            // Indicate a task completed.
                            latch.countDown();
                        }
                    }
                });
            }
            // Wait till all tasks get complete.
            latch.await();
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    /**
     * Perform KeyAgreement operation with a shared KeyPairGenerator instance.
     */
    private static void testKeyAgreement(String provider, String kaAlgo,
            KeyPairGenerator kpg) throws Exception {

        KeyPair kp1 = kpg.generateKeyPair();
        KeyPair kp2 = kpg.generateKeyPair();

        KeyAgreement ka1 = KeyAgreement.getInstance(kaAlgo, provider);
        ka1.init(kp1.getPrivate());
        ka1.doPhase(kp2.getPublic(), true);
        byte[] secret1 = ka1.generateSecret();
        KeyAgreement ka2 = KeyAgreement.getInstance(kaAlgo, provider);
        ka2.init(kp2.getPrivate());
        ka2.doPhase(kp1.getPublic(), true);
        byte[] secret2 = ka2.generateSecret();

        // With related keypairs, generated KeyAgreement secret should be same.
        if (!Arrays.equals(secret1, secret2)) {
            throw new Exception("KeyAgreement secret mismatch.");
        }
    }
}
