/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7003643
 * @summary Make sure MessageFormat.toPattern produces correct quoting. (SPI part is tested in PluggableLocale tests.)
 * @key randomness
 */

import java.text.*;
import java.util.*;

public class Bug7003643 {
    private static final int N = 5;

    private static final String[] elements = {
        "'{'", "'{", "{", "''", "}", "a", "'",
    };

    public static void main(String[] args) {
        Random rand = new Random();
        int count = 0;
        int max = (int) (Math.pow((double)elements.length, (double)N)/0.52);
        while (count < max) {
            // Create a random pattern. If the produced pattern is
            // valid, then proceed with the round-trip testing.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < N; i++) {
                sb.append(elements[rand.nextInt(elements.length)]);
            }
            String pattern = sb.toString();
            MessageFormat mf = null;
            try {
                mf = new MessageFormat(pattern);
            } catch (IllegalArgumentException e) {
                // bad pattern data
            }
            if (mf == null) {
                continue;
            }
            count++;
            String res1 = MessageFormat.format(pattern, 123);
            String toPattern = mf.toPattern();
            String res2 = MessageFormat.format(toPattern, 123);
            if (!res1.equals(res2)) {
                String s = String.format("Failed%n      pattern=\"%s\"  =>  result=\"%s\"%n"
                                         + "  toPattern()=\"%s\"  =>  result=\"%s\"%n",
                                         pattern, res1, toPattern, res2);
                throw new RuntimeException(s);
            }
        }
    }
}
