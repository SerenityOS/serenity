/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4687909
 * @summary Check Inet6Address.hashCode returns a reasonable spread of hash
 *          codes.
 * @key randomness
 */
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class HashSpread {

    static Random r = new Random();

    /**
     * Generate and return a random IPv6 address.
     */
    static InetAddress randomIPv6Adress() {
        StringBuffer sb = new StringBuffer();

        for (int i=0; i<8; i++) {

            if (i > 0)
                sb.append(":");

            for (int j=0; j<4; j++) {
                int v = r.nextInt(16);
                if (v < 10) {
                    sb.append(Integer.toString(v));
                } else {
                    char c = (char) ('A' + v - 10);
                    sb.append(c);
                }
            }
        }

        try {
            return InetAddress.getByName(sb.toString());
        } catch (UnknownHostException x) {
            throw new Error("Internal error in test");
        }
    }

    public static void main(String args[]) throws Exception {

        int iterations = 10000;
        if (args.length > 0) {
            iterations = Integer.parseInt(args[0]);
        }

        int MIN_SHORT = (int)Short.MIN_VALUE;
        int MAX_SHORT = (int)Short.MAX_VALUE;

        /*
         * Iterate through 10k hash codes and count the number
         * in the MIN_SHORT-MAX_SHORT range.
         */
        int narrow = 0;
        for (int i=0; i<iterations; i++) {
            int hc = randomIPv6Adress().hashCode();
            if (hc >= MIN_SHORT && hc <= MAX_SHORT) {
                narrow++;
            }
        }

        /*
         * If >85% of hash codes in the range then fail.
         */
        double percent = (double)narrow / (double)iterations * 100.0;
        if (percent > 85.0) {
            throw new RuntimeException(percent + " of hash codes were in " +
                MIN_SHORT + " to " + MAX_SHORT  + " range.");
        }

    }

}
