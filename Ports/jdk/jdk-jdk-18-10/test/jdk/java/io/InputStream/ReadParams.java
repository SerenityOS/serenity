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
 * @bug 4008296 4008293 4190090 4193729 4358774
 * @summary Check for correct handling of parameters to
 *          XXXXInputStream.read(b, off, len).
 *
 */

import java.io.*;
import java.util.zip.ZipInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

public class ReadParams {

    /* check for correct handling of different values of off and len */
    public static void doTest(InputStream in) throws Exception {

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
                in.read(b , off[i] , len[i]);
            } catch (IndexOutOfBoundsException aiobe) {
                if (results[i]) {
                    System.err.println("Error:IndexOutOfBoundsException thrown"+
                                       " for read(b, " + off[i] + " " + len[i] +
                                       " ) on " + in + "\nb.length = 32");
                    numBad++;
                } else {
                    /* System.err.println("PassE: " + off[i] + " " + len[i]); */
                }
                continue;
            } catch (OutOfMemoryError ome) {
                System.err.println("Error: OutOfMemoryError in read(b, " +
                                   off[i] + " " + len[i] + " ) on " + in +
                                   "\nb.length = 32");
                numBad++;
                continue;
            }
            if (!results[i]) {
                System.err.println("Error:No IndexOutOfBoundsException thrown"+
                                   " for read(b, " + off[i] + " " + len[i] +
                                   " ) on " + in + "\nb.length = 32");
                numBad++;
            } else {
                /* System.err.println("Pass: " + off[i] + " " + len[i]); */
            }
        }

        if (numBad > 0) {
            throw new RuntimeException(in + " Failed " + numBad + " cases");
        } else {
            System.err.println("Successfully completed bounds tests on " + in);
        }
    }

    /* check for correct handling of null b */
    public static void doTest1(InputStream in) throws Exception {
        byte b[] = null;
        try {
            in.read(b, 0, 32);
        } catch (NullPointerException npe) {
            System.err.println("SuccessFully completed null b test on " + in);
            return;
        }
        throw new RuntimeException(in + " Failed null b test");
    }

    public static void main(String args[]) throws Exception{
        /* initialise stuff */
        File fn = new File("x.ReadBounds");
        FileOutputStream fout = new FileOutputStream(fn);
        for (int i = 0; i < 32; i++) {
            fout.write(i);
        }
        fout.close();

        byte b[] = new byte[64];
        for(int i = 0; i < 64; i++) {
            b[i] = 1;
        }

        /* test all input streams */
        FileInputStream fis = new FileInputStream(fn);
        doTest(fis);
        doTest1(fis);
        fis.close();

        BufferedInputStream bis =
            new BufferedInputStream(new MyInputStream(1024));
        doTest(bis);
        doTest1(bis);
        bis.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        doTest(bais);
        doTest1(bais);
        bais.close();

        FileOutputStream fos = new FileOutputStream(fn);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeInt(12345);
        oos.writeObject("Today");
        oos.writeObject(new Integer(32));
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fn));
        doTest(ois);
        doTest1(ois);
        ois.close();

        DataInputStream dis = new DataInputStream(new MyInputStream(1024));
        doTest(dis);
        doTest1(dis);
        dis.close();

        LineNumberInputStream lis =
            new LineNumberInputStream(new MyInputStream(1024));
        doTest(lis);
        doTest1(lis);
        lis.close();

        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream();
        pos.connect(pis);
        pos.write(b, 0, 64);
        doTest(pis);
        doTest1(pis);
        pis.close();

        PushbackInputStream pbis =
            new PushbackInputStream(new MyInputStream(1024));
        doTest(pbis);
        doTest1(pbis);
        pbis.close();

        StringBufferInputStream sbis =
            new StringBufferInputStream(new String(b));
        doTest(sbis);
        doTest1(sbis);
        sbis.close();

        SequenceInputStream sis =
            new SequenceInputStream(new MyInputStream(1024),
                                    new MyInputStream(1024));
        doTest(sis);
        doTest1(sis);
        sis.close();

        ZipInputStream zis = new ZipInputStream(new FileInputStream(fn));
        doTest(zis);
        doTest1(zis);
        zis.close();

        byte[] data = new byte[256];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(bos);
        dos.write(data, 0, data.length);
        dos.close();
        InflaterInputStream ifs = new InflaterInputStream
            (new ByteArrayInputStream(bos.toByteArray()));
        doTest(ifs);
        doTest1(ifs);
        ifs.close();

        InputStream nis = InputStream.nullInputStream();
        doTest(nis);
        doTest1(nis);
        nis.close();

        /* cleanup */
        fn.delete();
    }
}

/* An InputStream class used in the above tests */
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
