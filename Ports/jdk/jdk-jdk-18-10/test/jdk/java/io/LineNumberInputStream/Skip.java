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
   @bug 4085939
   @summary Check for the correct behaviour of LineNumberInputStream.skip
   */

import java.io.*;


public class Skip{

    private static void dotest(LineNumberInputStream in , int curpos ,
                               long total , long toskip , long expected)
        throws Exception
    {

        try {

            System.err.println("\n\nCurrently at pos = " + curpos +
                               "\nTotal bytes in the Stream = " + total +
                               "\nNumber of bytes to skip = " + toskip +
                               "\nNumber of bytes that should be skipped = " +
                               expected);

            long skipped = in.skip(toskip);

            System.err.println("actual number skipped: "+ skipped);

            if ((skipped < 0) || (skipped > expected)) {
                throw new RuntimeException("Unexpected number of bytes skipped");
            }

        } catch (IOException e) {
            System.err.println("IOException is thrown - possible result");
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected "+e+" is thrown!");
        }

    }

    public static void main( String argv[] ) throws Exception {

        LineNumberInputStream in = new LineNumberInputStream(new MyInputStream(11));

        /* test for negative skip */
        dotest(in,  0, 11, -23,  0);

        /* check for skip beyond EOF starting from before EOF */
        dotest(in,  0, 11,  20, 11);

        /* check for skip after EOF */
        dotest(in, -1, 11,  20,  0);

    }

}

class MyInputStream extends InputStream {

    private int readctr = 0;
    private long endoffile;

    public MyInputStream(long endoffile) {
        this.endoffile = endoffile;
    }

    public int read() {

        if (readctr == endoffile) {
            return -1;
        }
        else {
            readctr++;
            return 0;
        }
    }

    public int available() { return 0; }
}
