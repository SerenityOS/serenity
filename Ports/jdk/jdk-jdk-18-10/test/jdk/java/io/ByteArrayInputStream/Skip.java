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


/* @test
 * @bug 6720170
 * @summary check for ByteArrayInputStream.skip
 */

import java.io.*;

public class Skip {
    private static void dotest(InputStream in, int curpos, long total,
                               long toskip, long expected)
        throws Exception
    {
        System.err.println("\nCurrently at pos = " + curpos +
                           "\nTotal bytes in the stream = " + total +
                           "\nNumber of bytes to skip = " + toskip +
                           "\nNumber of bytes that should be skipped = " +
                           expected);

        // position to curpos; EOF if negative
        in.reset();
        int avail = curpos >= 0 ? curpos : in.available();
        long n = in.skip(avail);
        if (n != avail) {
            throw new RuntimeException("Unexpected number of bytes skipped = " + n);
        }

        long skipped = in.skip(toskip);
        System.err.println("actual number skipped: "+ skipped);

        if (skipped != expected) {
            throw new RuntimeException("Unexpected number of bytes skipped = " + skipped);
        }
    }

    public static void main(String argv[]) throws Exception {
        int total = 1024;
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[total]);

        /* test for skip */
        dotest(in,  0, total, 23, 23);
        dotest(in,  10, total, 23, 23);

        /* test for negative skip */
        dotest(in,  0, total, -23,  0);

        /* check for skip after EOF */
        dotest(in, -1, total,  20,  0);

        /* check for skip beyond EOF starting from before EOF */
        dotest(in,  0, total, total+20, total);

        /* check for skip if the pos + toskip causes integer overflow */
        dotest(in, 10, total, Long.MAX_VALUE, total-10);
    }
}
