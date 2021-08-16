/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8021204
 * @summary Test constructor BigInteger(String val, int radix) on very long string
 * @requires (sun.arch.data.model == "64" & os.maxMemory > 8g)
 * @run main/othervm -Xshare:off -Xmx8g StringConstructorOverflow
 * @author Dmitry Nadezhin
 */
import java.math.BigInteger;

public class StringConstructorOverflow {

    // String with hexadecimal value pow(2,pow(2,34))+1
    private static String makeLongHexString() {
        StringBuilder sb = new StringBuilder();
        sb.append('1');
        for (int i = 0; i < (1 << 30) - 1; i++) {
            sb.append('0');
        }
        sb.append('1');
        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            BigInteger bi = new BigInteger(makeLongHexString(), 16);
            if (bi.compareTo(BigInteger.ONE) <= 0) {
                throw new RuntimeException("Incorrect result " + bi.toString());
            }
        } catch (ArithmeticException e) {
            // expected
            System.out.println("Overflow is reported by ArithmeticException, as expected");
        } catch (OutOfMemoryError e) {
            // possible
            System.err.println("StringConstructorOverflow skipped: OutOfMemoryError");
            System.err.println("Run jtreg with -javaoption:-Xmx8g");
        }
    }
}
