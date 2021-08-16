/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6910473
 * @summary Test that bitLength() is not negative
 * @author Dmitry Nadezhin
 */
import java.math.BigInteger;

public class BitLengthOverflow {

    public static void main(String[] args) {
        try {
            BigInteger x = BigInteger.ONE.shiftLeft(Integer.MAX_VALUE); // x = pow(2,Integer.MAX_VALUE)
            if (x.bitLength() != (1L << 31)) {
                throw new RuntimeException("Incorrect bitLength() " + x.bitLength());
            }
            System.out.println("Surprisingly passed with correct bitLength() " + x.bitLength());
        } catch (ArithmeticException e) {
            // expected
            System.out.println("Overflow is reported by ArithmeticException, as expected");
        } catch (OutOfMemoryError e) {
            // possible
            System.err.println("BitLengthOverflow skipped: OutOfMemoryError");
            System.err.println("Run jtreg with -javaoption:-Xmx8g");
        }
    }
}
