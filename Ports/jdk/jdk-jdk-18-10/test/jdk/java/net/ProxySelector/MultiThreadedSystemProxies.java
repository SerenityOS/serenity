/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7188755
 * @run main/othervm MultiThreadedSystemProxies
 * @summary Crash due to missing synchronization on gconf_client in
 *          DefaultProxySelector.c
 */
import java.net.ProxySelector;
import java.net.URI;

/* Racey test, not guaranteed to fail, but if it does we have a problem. */

public class MultiThreadedSystemProxies {
    static final int NUM_THREADS = 100;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.useSystemProxies", "true");
        final ProxySelector ps = ProxySelector.getDefault();
        final URI uri = new URI("http://ubuntu.com");
        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ps.select(uri);
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                }
            });
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].start();
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].join();
        }
    }
}
