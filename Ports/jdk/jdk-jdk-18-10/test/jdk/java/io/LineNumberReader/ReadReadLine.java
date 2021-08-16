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
   @bug 4103987
   @summary Tests LineNumberReader to see if the lineNumber is set correctly
            when mixed reads and readLines are used.
   */

import java.io.*;

public class ReadReadLine {

    public static void main(String[] args) throws Exception {
        test("\r\n", 1);
        test("\r\r\n", 2);
        test("\r\n\n\n", 3);
    }

    static void test(String s, int good) throws Exception {
        int c, line;

        LineNumberReader r = new LineNumberReader(new StringReader(s), 2);
        if ((c = r.read()) >= 0) {
            while (r.readLine() != null)
                ;
        }

        line = r.getLineNumber();

        if(line != good) {
            throw new Exception("Failed test: Expected line number "
                                + good + " Got: " + line);
        }
    }
}
