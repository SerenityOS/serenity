/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
   @summary General tests for BufferedReader.mark
 */

import java.io.*;


public class BufferedReaderMark {

    static int bufferSize = -1;
    static int inputSize = -1;

    static String d2(int n) {
        String s = Integer.toString(n);
        if (s.length() < 2)
            return s + " ";
        else
            return s;
    }

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    public static void main(String[] args) throws Exception {
        PrintStream log = System.err;

        if (bufferSize == -1)
            bufferSize = 7;
        if (inputSize == -1)
            inputSize = bufferSize * 3;

        try {
            Reader in = new BufferedReader(new ABCReader(inputSize), bufferSize);
            char buf[] = new char[bufferSize];
            if (in.read(buf) != bufferSize)
                fail("Read failed");
            in.reset();
            fail("reset() didn't throw");
        }
        catch (IOException x) {
            if (x.getMessage().equals("Stream not marked"))
                log.println("Unmarked-stream test OK");
            else
                fail(x.toString());
        }

        boolean err = false;
        try {
            Reader in = new BufferedReader(new ABCReader(inputSize), bufferSize);
            char buf[] = new char[bufferSize];
            if (in.read() == -1)
                fail("Read 1 failed");
            in.mark(bufferSize);
            while (in.read() != -1);
            in.reset();
            fail("reset() didn't throw");
        }
        catch (IOException x) {
            if (x.getMessage().equals("Mark invalid")) {
                err = true;
                log.println("Invalid-mark test OK");
            }
            else {
                log.println("Invalid-mark test failed: " + x);
                fail(x.toString());
            }
        }
        if (! err)
            fail("Invalid-mark test failed: Exception not thrown");

        int c;
        for (int off = 0; off < bufferSize * 2; off++) {
            for (int ra = 0; ra <= inputSize + 2; ra++) {
                Reader in = new BufferedReader(new ABCReader(inputSize), bufferSize);

                log.print(d2(off) + ", " + d2(ra) + "  mark: ");
                for (int i = 0; i < off; i++) {
                    if ((c = in.read()) == -1) {
                        log.print("<EOF>");
                        break;
                    }
                    log.print((char) c);
                }
                in.mark(ra);
                for (int i = 0; i < ra; i++) {
                    if ((c = in.read()) == -1) {
                        log.print("<EOF>");
                        break;
                    }
                    log.print((char) c);
                }
                log.println();

                log.print(d2(off) + ", " + d2(ra) + " reset: ");
                in.reset();
                for (int i = 0; i < off; i++)
                    log.print(' ');
                for (;;) {
                    if ((c = in.read()) == -1) {
                        log.print("<EOF>");
                        break;
                    }
                    log.print((char) c);
                }
                log.println();

            }
        }
    }

}
