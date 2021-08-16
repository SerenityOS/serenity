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
 * @bug 4429369
 * @summary  ISO2022CN and ISO2022KR converters throw exception
 * @modules jdk.charsets
 */

import java.io.*;
import java.nio.charset.*;

public class TestIllegalISO2022Esc {

    public static void main ( String[] args) throws Exception {
        int exceptionCount = 0;
        String[] encName = {"ISO2022CN", "ISO2022KR" };
        byte[]b= {
                (byte)0x1b, //Illegal sequence for both converters.
                (byte)')',
                (byte)'x'
        };

        for ( int i=0; i < 2; i++) { // Test 2 converters.
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(b);
                    InputStreamReader isr =
                                new InputStreamReader(bais,encName[i]);
                    char cc[] = new char[1];
                    isr.read(cc,0,1); //attempt to read
            } catch (MalformedInputException e) { } // Passes if thrown
              catch (Throwable t) {
                    System.err.println("error with converter " + encName[i]);
                    exceptionCount++;
            }
        }

        if (exceptionCount > 0)
           throw new Exception ("Incorrect handling of illegal ISO2022 escapes");
    }
}
