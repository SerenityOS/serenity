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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import static java.lang.Math.*;

/*
 * @test
 * @bug 8141039
 * @library /lib/testlibrary
 * @summary Test behavior of a shared SecureRandom object when it is operated
 *          by multiple threads concurrently.
 * @run main/othervm -Djava.security.egd=file:/dev/urandom MultiThreadTest
 */
public class MultiThreadTest {

    private static final byte[] GEN_RND_BYTES = {1};
    private static final String DRBG_CONFIG = "securerandom.drbg.config";
    private static final String DRBG_CONFIG_VALUE
            = Security.getProperty(DRBG_CONFIG);

    private enum SEED {

        NONE, RESEED, SETSEED
    }

    public static void main(String[] args) {

        boolean success = true;
        for (int byteLen : GEN_RND_BYTES) {
            for (SEED reSeed : SEED.values()) {
                for (String mech : new String[]{
                    "SHA1PRNG", "Hash_DRBG", "HMAC_DRBG", "CTR_DRBG"}) {
                    try {
                        forEachMech(mech, byteLen, reSeed);
                    } catch (Exception e) {
                        success = false;
                        e.printStackTrace(System.out);
                    } finally {
                        Security.setProperty(DRBG_CONFIG, DRBG_CONFIG_VALUE);
                    }
                }
            }
        }

        if (!success) {
            throw new RuntimeException("At least one test failed.");
        }
    }

    /**
     * Generate a number of threads to fetch random numbers of certain bits
     * generated through a shared SecureRandom instance.
     * @param mech Mechanism name
     * @param byteLen Number of bytes of random number to produce
     * @param reSeed Call reseed() before generating random numbers
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void forEachMech(String mech, int byteLen, SEED reSeed)
            throws NoSuchAlgorithmException, InterruptedException,
            ExecutionException {

        if ("SHA1PRNG".equals(mech) && SEED.RESEED.equals(reSeed)) {
            System.out.printf(
                    "%nreseed() api is not supported for '%s'", mech);
            return;
        }
        System.out.printf("%nTest SecureRandom mechanism: '%s' with support of"
                + " reseed: '%s'", mech, reSeed);
        int threadCount = (int) pow(2, 8 * byteLen);
        System.out.printf("%nCreating %s number of threads to generate secure "
                + "random numbers concurrently.", threadCount);

        ExecutorService executor
                = Executors.newCachedThreadPool(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory()
                        .newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });
        CompletionService<Integer> completionService
                = new ExecutorCompletionService<Integer>(executor);

        CountDownLatch latch = new CountDownLatch(1);
        SecureRandom rnd = null;
        if (!mech.contains("_DRBG")) {
            rnd = SecureRandom.getInstance(mech);
        } else {
            Security.setProperty(DRBG_CONFIG, mech);
            rnd = SecureRandom.getInstance("DRBG");
        }
        try {
            for (int i = 0; i < threadCount; i++) {
                completionService.submit(new Task(rnd, latch, byteLen, reSeed));
            }
            latch.countDown();

            for (int i = 0; i < threadCount; i++) {
                completionService.take();
            }
        } finally {
            executor.shutdown();
        }
        System.out.printf("%nCompleted Test for algorithm '%s' with thread "
                + "counts to '%s' using reseeding '%s'",
                mech, threadCount, reSeed);

    }

    /**
     * Define a Task to be executed by multiple thread to produce random numbers
     * from a shared SecureRandom instance.
     */
    private static class Task implements Callable<Integer> {

        private final SecureRandom random;
        private final CountDownLatch latch;
        private final SEED reSeed;
        private final int byteSize;

        public Task(SecureRandom random, CountDownLatch latch, int byteSize,
                SEED reSeed) {
            this.random = random;
            this.latch = latch;
            this.byteSize = byteSize;
            this.reSeed = reSeed;
        }

        @Override
        public Integer call() throws Exception {
            latch.await();
            switch (this.reSeed) {
                case RESEED:
                    this.random.reseed();
                    break;
                case SETSEED:
                    this.random.setSeed(1l);
                    break;
            }
            byte[] bytes = new byte[byteSize];
            random.nextBytes(bytes);
            return new BigInteger(bytes).intValue();
        }
    }

}
