/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jndi.ldap.pool;

/**
 * Thread that wakes up periodically and closes expired, unused connections.
 *
 * @author Rosanna Lee
 */
public final class PoolCleaner implements Runnable {
    private final Pool[] pools;
    private final long period;

    /**
     * @param period ms to wait between cleaning
     * @param pools non-null array of Pools to clean
     */
    public PoolCleaner(long period, Pool[] pools) {
        super();
        this.period = period;
        this.pools = pools.clone();
    }

    @Override
    public void run() {
        long threshold;
        while (true) {
            synchronized (this) {
                // Wait for duration of period ms
                try {
                    wait(period);
                } catch (InterruptedException ignore) {
                }

                // Connections idle since threshold have expired
                threshold = System.currentTimeMillis() - period;
                for (int i = 0; i < pools.length; i++) {
                    if (pools[i] != null) {
                        pools[i].expire(threshold);
                    }
                }
            }
        }
    }
}
