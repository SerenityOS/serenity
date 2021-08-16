/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Statistics as long values
 */
public class StatLong {

    public final String name;
    public long count = 0l;
    public long sum = 0l;
    public long min = Integer.MAX_VALUE;
    public long max = Integer.MIN_VALUE;

    public StatLong(final String name) {
        this.name = name;
    }

    public void reset() {
        count = 0l;
        sum = 0l;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
    }

    public void add(final int val) {
        count++;
        sum += val;
        if (val < min) {
            min = val;
        }
        if (val > max) {
            max = val;
        }
    }

    public void add(final long val) {
        count++;
        sum += val;
        if (val < min) {
            min = val;
        }
        if (val > max) {
            max = val;
        }
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(128)).toString();
    }

    public final StringBuilder toString(final StringBuilder sb) {
        sb.append(name).append('[').append(count);
        sb.append("] sum: ").append(sum).append(" avg: ");
        sb.append(trimTo3Digits(((double) sum) / count));
        sb.append(" [").append(min).append(" | ").append(max).append("]");
        return sb;
    }

    /**
     * Adjust the given double value to keep only 3 decimal digits
     *
     * @param value value to adjust
     * @return double value with only 3 decimal digits
     */
    public static double trimTo3Digits(final double value) {
        return ((long) (1e3d * value)) / 1e3d;
    }
}

