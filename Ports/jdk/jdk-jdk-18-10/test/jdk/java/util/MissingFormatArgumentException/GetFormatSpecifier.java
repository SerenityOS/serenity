/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5063455
 * @summary Unit test for MissingFormatArgumentException.getFormatSpecifier
 * @author  Brandon Passanisi
 */
import java.util.MissingFormatArgumentException;

public class GetFormatSpecifier {

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    public static void main(String[] args) {

        // Use the format specifier below, which should throw a
        // MissingFormatArgumentException. Then, use getFormatSpecifier()
        // to make sure the returned value equals the original format string.
        final String formatSpecifier = "%1$5.3s";
        try {
            String formatResult = String.format(formatSpecifier);
            fail("MissingFormatArgumentException not thrown.");
        } catch (MissingFormatArgumentException ex) {
            final String returnedFormatSpecifier = ex.getFormatSpecifier();
            if (!returnedFormatSpecifier.equals(formatSpecifier)) {
                fail("The specified format specifier: " + formatSpecifier
                        + " does not match the value from getFormatSpecifier(): "
                        + returnedFormatSpecifier);
            }
        }
    }
}
