/*
 * Copyright (c) 2016 Google Inc. All rights reserved.
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
 * @bug 8156584
 * @modules java.base/sun.security.x509
 * @summary AlgorithmId.get initialization thread safety
 * @run main/othervm OidTableInit
 */

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import sun.security.x509.AlgorithmId;

public class OidTableInit {
    public static void main(String[] args) throws Throwable {
        final String[] algorithmNames = {
            "PBKDF2WITHHMACSHA1",
            "PBEWITHMD5ANDDES",
            "DSA",
            "SHA384WITHRSA",
            "RSA",
            "SHA1WITHDSA",
            "SHA512WITHRSA",
            "MD2WITHRSA",
            "PBEWITHSHA1ANDDESEDE",
            "SHA1WITHRSA",
            "DIFFIEHELLMAN",
            "MD5WITHRSA",
            "PBEWITHSHA1ANDRC2_40",
            "SHA256WITHRSA",
        };

        final int THREADS = 2;
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        final CountDownLatch startingGate = new CountDownLatch(THREADS);
        final Runnable r = new Runnable() { public void run() {
            startingGate.countDown();
            do {} while (startingGate.getCount() > 0);
            try {
                for (String algorithmName : algorithmNames)
                    AlgorithmId.get(algorithmName);
            } catch (Throwable fail) {
                throw new AssertionError(fail);
            }
        }};
        final ArrayList<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++)
            futures.add(pool.submit(r));
        pool.shutdown();
        for (Future<?> future : futures) future.get();
    }
}
