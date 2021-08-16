/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6989440
 * @summary Verify ConcurrentModificationException is not thrown with multiple
 *     thread accesses.
 * @modules java.base/sun.util.locale.provider
 * @compile -XDignore.symbol.file=true Bug6989440.java
 * @run main Bug6989440
 */
import java.text.spi.DateFormatProvider;
import java.util.spi.LocaleNameProvider;
import java.util.spi.LocaleServiceProvider;
import java.util.spi.TimeZoneNameProvider;

import sun.util.locale.provider.LocaleServiceProviderPool;

public class Bug6989440 {
    static volatile boolean failed;  // false
    static final int THREADS = 50;

    public static void main(String[] args) throws Exception {
        Thread[] threads = new Thread[THREADS];
        for (int i=0; i<threads.length; i++)
            threads[i] = new TestThread();
        for (int i=0; i<threads.length; i++)
            threads[i].start();
        for (int i=0; i<threads.length; i++)
            threads[i].join();

        if (failed)
            throw new RuntimeException("Failed: check output");
    }

    static class TestThread extends Thread {
        private Class<? extends LocaleServiceProvider> cls;
        private static int count;

        public TestThread(Class<? extends LocaleServiceProvider> providerClass) {
            cls = providerClass;
        }

        public TestThread() {
            int which = count++ % 3;
            switch (which) {
                case 0 : cls = LocaleNameProvider.class; break;
                case 1 : cls = TimeZoneNameProvider.class; break;
                case 2 : cls = DateFormatProvider.class; break;
                default : throw new AssertionError("Should not reach here");
            }
        }

        public void run() {
            try {
                LocaleServiceProviderPool pool = LocaleServiceProviderPool.getPool(cls);
                pool.getAvailableLocales();
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
                failed = true;
            }
        }
    }
}
