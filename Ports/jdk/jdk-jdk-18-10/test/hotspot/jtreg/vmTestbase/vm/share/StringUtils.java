/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.function.Predicate;

public class StringUtils {

    public static byte[] binaryReplace(final byte[] src, String search,
            String replacement) {
        if (search.length() == 0)
            return src;

        int nReplaced = 0;

        try {
            final byte[] bSrch = search.getBytes("ASCII");
            final byte[] bRepl = replacement.getBytes("ASCII");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                searching: for (int i = 0; i < src.length; i++) {
                    if (src[i] == bSrch[0]) {
                        replacing: do {
                            for (int ii = 1; ii < Math.min(bSrch.length,
                                    src.length - i); ii++)
                                if (src[i + ii] != bSrch[ii])
                                    break replacing;

                            out.write(bRepl);
                            i += bSrch.length - 1;
                            nReplaced++;
                            continue searching;
                        } while (false);
                    }

                    out.write(src[i]);
                }

            return out.toByteArray();

            } finally {
                out.close();
            }
        } catch (Exception e) {
            RuntimeException t = new RuntimeException("Test internal error");
            t.initCause(e);
            throw t;
        }
    }

    public static String generateString(Random rng, int length,
            Predicate<Character> predicate) {
        if (length <= 0) {
            throw new IllegalArgumentException("length <= 0");
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            char tmp;
            do {
                tmp = (char) rng.nextInt(Character.MAX_VALUE);
            } while (!predicate.test(tmp));
            builder.append(tmp);
        }
        return builder.toString();
    }
}
