/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231387
 * @library ../testlibrary
 * @summary make sure getService() avoids a race
 * @author Tianmin Shi
 */

import java.security.Provider;

public class GetServiceRace {

    private static final Provider testProvider;
    static {
        testProvider = new Provider("MyProvider", 1.0, "test") {
        };
        testProvider.put("CertificateFactory.Fixed", "MyCertificateFactory");
    }

    private static final int NUMBER_OF_RETRIEVERS = 3;
    private static final int TEST_TIME_MS = 1000;

    public static boolean testFailed = false;

    public static void main(String[] args) throws Exception {
        Updater updater = new Updater();
        updater.start();
        Retriever [] retrievers = new Retriever[NUMBER_OF_RETRIEVERS];
        for (int i=0; i<retrievers.length; i++) {
            retrievers[i] = new Retriever();
            retrievers[i].start();
        }
        Thread.sleep(TEST_TIME_MS);
        System.out.println("Interrupt");
        updater.interrupt();
        updater.join();
        for (int i=0; i<retrievers.length; i++) {
            retrievers[i].interrupt();
            retrievers[i].join();
        }
        System.out.println("Done");
        if (testFailed) {
            throw new Exception("Test Failed");
        }
        System.out.println("Test Passed");
    }

    private static class Updater extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                testProvider.put("CertificateFactory.Added", "MyCertificateFactory");
            }
            System.out.println("Updater stopped");
        }
    }

    private static class Retriever extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                Provider.Service service = testProvider.getService("CertificateFactory", "Fixed");
                if (service == null) {
                    if (!testFailed) {
                        System.err.println("CertificateFactory.Fixed is NULL");
                        testFailed = true;
                    }
                } else {
                    //System.out.println("CertificateFactory.Fixed is good");
                }
            }
            System.out.println("Retriever stopped");
        }
    }
}
