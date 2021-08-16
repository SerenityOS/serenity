/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4074875 4063511 8235792
   @summary Make sure LineNumberReader.read(char, int , int) will increase
            the linenumber correctly.
   */

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.function.Consumer;

public class Read {

    public static void main(String[] args) throws Exception {
        testReadChars();
        testEofs();
    }

    private static void testReadChars() throws Exception {
        String s = "aaaa\nbbb\n";
        char[] buf = new char[5];
        int n = 0;
        int line = 0;

        LineNumberReader r = new LineNumberReader(new StringReader(s));

        do {
            n = r.read(buf, 0, buf.length);
        } while (n > 0);

        line = r.getLineNumber();

        if (line != 2)
            throw new Exception("Failed test: Expected line number: 2, got "
                                + line);
    }

    private static void testEofs() throws Exception {
        String string = "first \n second";

        Consumer<LineNumberReader> c = (LineNumberReader r) -> {
            try {
                while (r.read() != -1)
                    continue;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        testEof(c, string, 2);

        c = (LineNumberReader r) -> {
            try {
                char[] buf = new char[128];
                while (r.read(buf) != -1)
                    continue;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        testEof(c, string, 2);

        c = (LineNumberReader r) -> {
            try {
                while (r.readLine() != null)
                    continue;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        testEof(c, string, 2);
    }

    private static void testEof(Consumer<LineNumberReader> c, String s, int n)
        throws Exception {
        LineNumberReader r = new LineNumberReader(new StringReader(s));
        c.accept(r);
        int line;
        if ((line = r.getLineNumber()) != n)
            throw new Exception("Failed test: Expected line number: " + n  +
                " , got " + line);
    }
}
