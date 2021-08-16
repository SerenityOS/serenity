/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.marlin.stats;

/**
 * Generic histogram based on long statistics
 */
public final class Histogram extends StatLong {

    static final int BUCKET = 2;
    static final int MAX = 20;
    static final int LAST = MAX - 1;
    static final int[] STEPS = new int[MAX];

    static {
            STEPS[0] = 0;
            STEPS[1] = 1;

            for (int i = 2; i < MAX; i++) {
                STEPS[i] = STEPS[i - 1] * BUCKET;
            }
    }

    static int bucket(int val) {
        for (int i = 1; i < MAX; i++) {
            if (val < STEPS[i]) {
                return i - 1;
            }
        }
        return LAST;
    }

    private final StatLong[] stats = new StatLong[MAX];

    public Histogram(final String name) {
        super(name);
        for (int i = 0; i < MAX; i++) {
            stats[i] = new StatLong(String.format("%5s .. %5s", STEPS[i],
                                    ((i + 1 < MAX) ? STEPS[i + 1] : "~")));
        }
    }

    @Override
    public void reset() {
        super.reset();
        for (int i = 0; i < MAX; i++) {
            stats[i].reset();
        }
    }

    @Override
    public void add(int val) {
        super.add(val);
        stats[bucket(val)].add(val);
    }

    @Override
    public void add(long val) {
        add((int) val);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(2048);
        super.toString(sb).append(" { ");

        for (int i = 0; i < MAX; i++) {
            if (stats[i].count != 0l) {
                sb.append("\n        ").append(stats[i].toString());
            }
        }

        return sb.append(" }").toString();
    }
}

