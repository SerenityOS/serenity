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

import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * @test
 * @bug 7004967
 * @run main/othervm NoSync
 * @summary SecureRandom should be more explicit about threading
 */
public class NoSync {
    public static void main(String[] args) throws Exception {
        for (Provider p : Security.getProviders()) {
            for (Provider.Service s : p.getServices()) {
                if (s.getType().equals("SecureRandom") &&
                        !s.getAlgorithm().contains("Block")) {
                    test(SecureRandom.getInstance(s.getAlgorithm(), p));
                }
            }
        }
        Security.setProperty("securerandom.drbg.config", "HMAC_DRBG");
        test(SecureRandom.getInstance("DRBG"));
        Security.setProperty("securerandom.drbg.config", "CTR_DRBG");
        test(SecureRandom.getInstance("DRBG"));
    }

    static void test(SecureRandom sr) throws Exception {
        test(sr, 20, 3000);
        // All out-of-box impl should have the ThreadSafe attribute
        String attr = sr.getProvider().getProperty("SecureRandom."
                + sr.getAlgorithm() + " ThreadSafe");
        if (!"true".equals(attr)) {
            throw new Exception("Not ThreadSafe: " + attr);
        }
    }

    public static void test(SecureRandom sr, int tnum, int rnum)
            throws Exception {

        System.out.println(sr);
        System.out.println(sr.getAlgorithm() + " " + sr.getProvider().getName());

        System.out.println(new Date());
        boolean reseed = sr.getParameters() != null;
        Thread[] threads = new Thread[tnum];
        AtomicBoolean failed = new AtomicBoolean(false);
        Thread.UncaughtExceptionHandler h = (t, e) -> {
            failed.set(true);
            e.printStackTrace();
        };
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    for (int j = 0; j < rnum; j++) {
                        sr.nextBytes(new byte[j%100+100]);
                        sr.setSeed((long)j);
                        if (reseed) {
                            sr.reseed();
                        }
                    }
                }
            };
            threads[i].setUncaughtExceptionHandler(h);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        System.out.println(new Date());
        System.out.println();
        if (failed.get()) {
            throw new RuntimeException("Failed");
        }
    }
}
