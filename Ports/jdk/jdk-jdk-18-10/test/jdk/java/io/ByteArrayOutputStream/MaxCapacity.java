/*
 * Copyright (c) 2014, Google Inc. All rights reserved.
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

/*
 * @test
 * @bug 8055949
 * @summary Check that we can write (almost) Integer.MAX_VALUE bytes
 *          to a ByteArrayOutputStream.
 * @requires (sun.arch.data.model == "64" & os.maxMemory >= 10g)
 * @run main/timeout=1800/othervm -Xmx8g MaxCapacity
 * @author Martin Buchholz
 */
import java.io.ByteArrayOutputStream;

public class MaxCapacity {
    public static void main(String[] args) {
        long maxHeap = Runtime.getRuntime().maxMemory();
        if (maxHeap < 3L * Integer.MAX_VALUE) {
            System.out.printf("Skipping test; max memory %sM too small%n",
                              maxHeap/(1024*1024));
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (long n = 0; ; n++) {
            try {
                baos.write((byte)'x');
            } catch (Throwable t) {
                // check data integrity while we're here
                byte[] bytes = baos.toByteArray();
                if (bytes.length != n)
                    throw new AssertionError("wrong length");
                if (bytes[0] != 'x' ||
                    bytes[bytes.length - 1] != 'x')
                    throw new AssertionError("wrong contents");

                long gap = Integer.MAX_VALUE - n;
                System.out.printf("gap=%dM %d%n", gap/(1024*1024), gap);
                if (gap > 1024)
                    throw t;
                // t.printStackTrace();
                break;
            }
        }
    }
}
