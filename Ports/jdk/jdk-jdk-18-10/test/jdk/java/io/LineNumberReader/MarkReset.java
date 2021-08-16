/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4081733
   @summary Make sure LineNumberReader returns right line number
            when mark and reset are used
   */


import java.io.*;

public class MarkReset {

    /**
     * This program creates a LineNumberReader and tries to find all
     * the non-whitespace characters in the file.
     */
    public static void main(String[] args) throws Exception {
        int n, line;

        LineNumberReader reader = new LineNumberReader
                                      (new StringReader("0\r\n1\r2\n3\r\n\r5\r\r7\n\n9"));
        for (n = 0; n < 7; n++) {
            skipWhiteSpace(reader);     /* Skip all whitespace */
            int c = reader.read();      /* Read the non-whitespace character */
            if (c < 0) {                /* Might be eof */
                break;                  /* It is. Get out of the loop */
            }
            line = reader.getLineNumber();
            if(line != (c - 48)) {
                throw new Exception("Failed test : Line number expected "
                                    + (c - 48)  + " got " + line );
            }
        }
    }

    /**
     * Skip whitespace in the file. Mark and reset
     */
    private static void skipWhiteSpace(LineNumberReader reader) throws IOException {
        while (true) {
            /* Mark in case the character is not whitespace */
            reader.mark(10);
            /* Read the character */
            int c = reader.read();
            if (Character.isWhitespace((char) c)) {
                /* Loop while in whitespace */
                continue;
            }

            /* Return to the non-whitespace character */
            reader.reset();
            break;
        }
    }
}
