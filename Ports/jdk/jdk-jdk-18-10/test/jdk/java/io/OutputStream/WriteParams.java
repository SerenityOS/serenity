/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 1267039 1267043 4193729 4358774
 * @summary Check for correct handling of parameters to
 *          XXXXOutputStream.write(b, off, len).
 *
 */

import java.io.*;
import java.util.zip.DeflaterOutputStream;

public class WriteParams {

    /* check for correct handling of different values of off and len */
    public static void doTest(OutputStream out) throws Exception {

        int off[] = {-1, -1,  0, 0, 33, 33, 0, 32, 32, 4, 1, 0,  -1,
                     Integer.MAX_VALUE, 1, Integer.MIN_VALUE,
                     Integer.MIN_VALUE, 1};
        int len[] = {-1,  0, -1, 33, 0, 4, 32, 0, 4, 16, 31, 0,
                     Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                     1, -1, Integer.MIN_VALUE};
        boolean results[] = { false,  false,  false, false, false, false,
                              true, true, false, true, true, true,  false,
                              false, false, false, false, false};
        int numCases = off.length;
        byte b[] = new byte[32];
        int numBad = 0;

        for(int i = 0; i < numCases; i++) {
            try {
                out.write(b , off[i] , len[i]);
            } catch (IndexOutOfBoundsException aiobe) {
                if (results[i]) {
                    System.err.println("Error:IndexOutOfBoundsException thrown"+
                                       " for write(b, " + off[i] + " " + len[i] +
                                       " ) on " + out + "\nb.length = 32");
                    numBad++;
                } else {
                    /* System.err.println("PassE: " + off[i] + " " + len[i]); */
                }
                continue;
            } catch (OutOfMemoryError ome) {
                System.err.println("Error: OutOfMemoryError in write(b, " +
                                   off[i] + " " + len[i] + " ) on " + out +
                                   "\nb.length = 32");
                numBad++;
                continue;
            }
            if (!results[i]) {
                System.err.println("Error:No IndexOutOfBoundsException thrown"+
                                   " for write(b, " + off[i] + " " + len[i] +
                                   " ) on " + out + "\nb.length = 32");
                numBad++;
            } else {
                /* System.err.println("Pass: " + off[i] + " " + len[i]); */
            }
        }

        if (numBad > 0) {
            throw new RuntimeException(out + " Failed " + numBad + " cases");
        } else {
            System.err.println("Successfully completed bounds tests on " + out);
        }
    }

    /* check for correct handling of null b */
    public static void doTest1(OutputStream out) throws Exception {
        byte b[] = null;
        try {
            out.write(b, 0, 32);
        } catch (NullPointerException npe) {
            System.err.println("SuccessFully completed null b test on " + out);
            return;
        }
        throw new RuntimeException(out + " Failed null b test");
    }

    public static void main(String args[]) throws Exception{
        /* initialise stuff here */
        File fn = new File("x.WriteBounds");
        FileOutputStream fout = new FileOutputStream(fn);
        for (int i = 0; i < 32; i++) {
            fout.write(i);
        }
        fout.close();

        byte b[] = new byte[64];
        for(int i = 0; i < 64; i++) {
            b[i] = 1;
        }

        /* test for different output streams */
        FileOutputStream fos = new FileOutputStream(fn);
        doTest(fos);
        doTest1(fos);
        fos.close();

        ObjectOutputStream oos = new ObjectOutputStream(new MyOutputStream());
        doTest(oos);
        doTest1(oos);
        oos.close();

        BufferedOutputStream bos =
            new BufferedOutputStream(new MyOutputStream());
        doTest(bos);
        doTest1(bos);
        bos.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doTest(baos);
        doTest1(baos);
        baos.close();

        DataOutputStream dos = new DataOutputStream(new MyOutputStream());
        doTest(dos);
        doTest1(dos);
        dos.close();

        PipedInputStream pis = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream();
        pos.connect(pis);
        doTest(pos);
        doTest1(pos);
        pos.close();

        DeflaterOutputStream dfos = new DeflaterOutputStream(new MyOutputStream());
        doTest(dfos);
        doTest1(dfos);
        dfos.close();

        OutputStream nos = OutputStream.nullOutputStream();
        doTest(nos);
        doTest1(nos);
        nos.close();

        /* cleanup */
        fn.delete();

    }
}

/* An OutputStream class used in the above tests */
class MyOutputStream extends OutputStream {

    public MyOutputStream() {
    }

    public void write(int b) throws IOException {
    }
}
