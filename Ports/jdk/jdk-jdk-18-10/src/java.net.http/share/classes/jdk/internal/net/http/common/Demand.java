/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates operations with demand (Reactive Streams).
 *
 * <p> Demand is the aggregated number of elements requested by a Subscriber
 * which is yet to be delivered (fulfilled) by the Publisher.
 */
public final class Demand {

    private final AtomicLong val = new AtomicLong();

    /**
     * Increases this demand by the specified positive value.
     *
     * @param n
     *         increment {@code > 0}
     *
     * @return {@code true} iff prior to this operation this demand was fulfilled
     */
    public boolean increase(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("non-positive subscription request: " + String.valueOf(n));
        }
        long prev = val.getAndAccumulate(n, (p, i) -> p + i < 0 ? Long.MAX_VALUE : p + i);
        return prev == 0;
    }

    /**
     * Increases this demand by 1 but only if it is fulfilled.
     * @return true if the demand was increased, false otherwise.
     */
    public boolean increaseIfFulfilled() {
        return val.compareAndSet(0, 1);
    }

    /**
     * Tries to decrease this demand by the specified positive value.
     *
     * <p> The actual value this demand has been decreased by might be less than
     * {@code n}, including {@code 0} (no decrease at all).
     *
     * @param n
     *         decrement {@code > 0}
     *
     * @return a value {@code m} ({@code 0 <= m <= n}) this demand has been
     *         actually decreased by
     */
    public long decreaseAndGet(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException(String.valueOf(n));
        }
        long p, d;
        do {
            d = val.get();
            p = Math.min(d, n);
        } while (!val.compareAndSet(d, d - p));
        return p;
    }

    /**
     * Tries to decrease this demand by {@code 1}.
     *
     * @return {@code true} iff this demand has been decreased by {@code 1}
     */
    public boolean tryDecrement() {
        return decreaseAndGet(1) == 1;
    }

    /**
     * @return {@code true} iff there is no unfulfilled demand
     */
    public boolean isFulfilled() {
        return val.get() == 0;
    }

    /**
     * Resets this object to its initial state.
     */
    public void reset() {
        val.set(0);
    }

    /**
     * Returns the current value of this demand.
     *
     * @return the current value of this demand
     */
    public long get() {
        return val.get();
    }

    @Override
    public String toString() {
        return String.valueOf(val.get());
    }
}
