/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4091789 4173233
 * @summary Check if LineNumberReader will skip right number of characters and
 *          also check for negative values
 */

import java.io.*;

public class Skip {

    public static void main(String[] args) throws Exception {

        int linenum = 0;
        long nchars = 164 * 50;

        File f = new File(System.getProperty("test.src", "."),
            "SkipInput.txt");
        LineNumberReader reader = new LineNumberReader(new FileReader(f));

        boolean testFailed = false;
        try {
            reader.skip(-10);
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }
        catch (Exception e) {
            testFailed = true;
        }
        if (testFailed)
            throw new Exception("Failed test: Negative value for skip()");

        long realnum = reader.skip(nchars);
        linenum = reader.getLineNumber();

        if (linenum != 164) {
            throw new Exception("Failed test: Should skip 164, really skipped "
                                + linenum + "lines");
        }
    }
}
