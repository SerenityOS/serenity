/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429358
 * @summary Remove illegal SI/SO char to byte mappings
 * @modules jdk.charsets
 */

public class TestCp93xSISO {
    public static void main ( String[] args) throws Exception {
        int exceptionCount = 0;
        String[] encName = {"Cp930", "Cp933", "Cp935", "Cp937", "Cp939" };

        String s = "\u000e\u000f" ;

        for ( int i=0; i < encName.length; i++) { // Test 2 converters.
            try {
                byte[] encoded = s.getBytes(encName[i]);
                for (int j=0 ; j<encoded.length; j++) {
                    if (encoded[j] != (byte)0x6f) // Expect to map to 0x6f
                        exceptionCount++;
                }
            } catch (Throwable t) {
                    System.err.println("error with converter " + encName[i]);
                    exceptionCount++;
            }
        }

        if (exceptionCount > 0)
           throw new Exception ("bug4429369: Cp93x SI/SO Ch->Byte mappings incorrect");
    }
}
