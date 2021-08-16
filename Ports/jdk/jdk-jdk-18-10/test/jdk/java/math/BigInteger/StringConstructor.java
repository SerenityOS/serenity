/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4489146 5017980
 * @summary tests String constructors of BigInteger
 * @author Joseph D. Darcy
 */
import java.math.*;

public class StringConstructor {


    public static void main(String [] argv) {
        // Good strings
        constructWithoutError("0", 0L);
        constructWithoutError("000000000000000000", 0L);
        constructWithoutError("1", 1L);
        constructWithoutError("-1", -1L);
        constructWithoutError("+1", +1L);
        constructWithoutError( "123456789123456789", 123456789123456789L);
        constructWithoutError("+123456789123456789", 123456789123456789L);
        constructWithoutError("-123456789123456789", -123456789123456789L);
        constructWithoutError(Integer.toString(Integer.MIN_VALUE),
                              (long)Integer.MIN_VALUE);
        constructWithoutError(Integer.toString(Integer.MAX_VALUE),
                              (long)Integer.MAX_VALUE);
        constructWithoutError(Long.toString(Long.MIN_VALUE),
                              Long.MIN_VALUE);
        constructWithoutError(Long.toString(Long.MAX_VALUE),
                              Long.MAX_VALUE);

        // Bad strings
        constructWithError("");
        constructWithError("-");
        constructWithError("+");
        constructWithError("--");
        constructWithError("++");
        constructWithError("-000-0");
        constructWithError("+000+0");
        constructWithError("+000-0");
        constructWithError("--1234567890");
        constructWithError("++1234567890");
        constructWithError("-0-12345678");
        constructWithError("+0+12345678");
        constructWithError("--12345678-12345678-12345678");
        constructWithError("++12345678+12345678+12345678");
        constructWithError("12345-");
        constructWithError("12345+");
    }

    // this method adapted from ../BigDecimal/StringConstructor.java
    private static void constructWithError(String badString) {
        try {
            BigInteger bi = new BigInteger(badString);
            throw new RuntimeException(badString + " accepted");
        } catch(NumberFormatException e) {
        }
    }

    private static void constructWithoutError(String goodString, long value) {
            BigInteger bi = new BigInteger(goodString);
            if(bi.longValue() != value) {
                System.err.printf("From ``%s'' expected %d, got %s.\n", goodString, value, bi);
                throw new RuntimeException();
            }
    }

}
