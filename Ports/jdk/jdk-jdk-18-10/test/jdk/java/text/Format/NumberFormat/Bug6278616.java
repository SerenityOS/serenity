/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Confirm that AtomicInteger and AtomicLong are formatted correctly.
 * @bug 6278616
 */

import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import java.util.Locale;

public class Bug6278616 {

    static final int[] ints = {
        Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE
    };

    static final long[] longs = {
        Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE
    };

    public static void main(String[] args) {
        NumberFormat nf = NumberFormat.getInstance();

        for (int j = 0; j < ints.length; j++) {
            String s_i = nf.format(ints[j]);
            String s_ai = nf.format(new AtomicInteger(ints[j]));
            if (!s_i.equals(s_ai)) {
                throw new RuntimeException("format(AtomicInteger " + s_ai +
                                           ") doesn't equal format(Integer " +
                                           s_i + ")");
            }
        }

        for (int j = 0; j < longs.length; j++) {
            String s_l = nf.format(longs[j]);
            String s_al = nf.format(new AtomicLong(longs[j]));
            if (!s_l.equals(s_al)) {
                throw new RuntimeException("format(AtomicLong " + s_al +
                                           ") doesn't equal format(Long " +
                                           s_l + ")");
            }
        }
    }
}
