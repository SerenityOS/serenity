/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.util;

import java.lang.ref.Cleaner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Utility class to invoke System.gc()
 */
public class ForceGC {
    private final CountDownLatch cleanerInvoked = new CountDownLatch(1);
    private final Cleaner cleaner = Cleaner.create();
    private Object o;

    public ForceGC() {
        this.o = new Object();
        cleaner.register(o, () -> cleanerInvoked.countDown());
    }

    private void doit(int iter) {
        try {
            for (int i = 0; i < 10; i++) {
                System.gc();
                System.out.println("doit() iter: " + iter + ", gc " + i);
                if (cleanerInvoked.await(1L, TimeUnit.SECONDS)) {
                    return;
                }
            }
        } catch (InterruptedException unexpected) {
            throw new AssertionError("unexpected InterruptedException");
        }
    }

    /**
     * Causes the current thread to wait until the {@code BooleanSupplier} returns true,
     * unless the thread is interrupted or a predefined waiting time elapses.
     *
     * @param s boolean supplier
     * @return true if the {@code BooleanSupplier} returns true and false if
     *         the predefined waiting time elapsed before the count reaches zero.
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean await(BooleanSupplier s) {
        o = null; // Keep reference to Object until now, to ensure the Cleaner
                  // doesn't count down the latch before await() is called.
        for (int i = 0; i < 10; i++) {
            if (s.getAsBoolean()) return true;
            doit(i);
            try { Thread.sleep(1000); } catch (InterruptedException e) {
                throw new AssertionError("unexpected interrupted sleep", e);
            }
        }
        return false;
    }
}
