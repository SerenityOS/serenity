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
   @bug 4085938
   @summary Check for the correct behaviour of DataInputStream.skipBytes
   */

import java.io.*;

public class SkipBytes{

    private static void dotest(DataInputStream dis, int pos, int total,
                               int toskip, int expected) {

        try {
            System.err.println("\n\nTotal bytes in the stream = " + total);
            System.err.println("Currently at position = " + pos);
            System.err.println("Bytes to skip = " + toskip);
            System.err.println("Expected result = " + expected);
            int skipped = dis.skipBytes(toskip);
            System.err.println("Actual skipped = " + skipped);
            if (skipped != expected) {
                throw new RuntimeException
                    ("DataInputStream.skipBytes does not return expected value");
            }
        } catch(EOFException e){
            throw new RuntimeException
                ("DataInputStream.skipBytes throws unexpected EOFException");
        } catch (IOException e) {
            System.err.println("IOException is thrown - possible result");
        }


    }



    public static void main(String args[]) throws Exception {

        DataInputStream dis = new DataInputStream(new MyInputStream());
        dotest(dis, 0, 11, -1, 0);
        dotest(dis, 0, 11, 5, 5);
        System.err.println("\n***CAUTION**** - may go into an infinite loop");
        dotest(dis, 5, 11, 20, 6);
    }
}


class MyInputStream extends InputStream {

    private int readctr = 0;

    public int read() {

        if (readctr > 10){
            return -1;
        }
        else{
            readctr++;
            return 0;
        }

    }

    public int available() { return 0; }
}
