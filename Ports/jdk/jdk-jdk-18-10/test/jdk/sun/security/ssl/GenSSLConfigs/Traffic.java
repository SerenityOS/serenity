/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.security.*;

class Traffic
{
    private InputStream         in;
    private OutputStream        out;

    //
    // By default, traffic streams are predictable and what comes
    // in is compared with what it's expected to be.
    //
    static private byte fixedSeed [] = { 1, 2, 3, 4};

    private SecureRandom        prng;
    private boolean             compareRandom = true;


    Traffic (InputStream in, OutputStream out)
    {
        this.in = in;
        this.out = out;
        try {
            prng = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        prng.setSeed(fixedSeed);
    }

    // optionally provide PRNG for "truly" random data.
    public void setPRNG (SecureRandom prng)
    {
        this.prng = prng;
        compareRandom = false;
    }


    //
    // Basic half-duplex testing, as used for RPC-style systems like
    // HTTP, RMI, CORBA, ONC, etc.
    //
    // parameter 'n' is "0" for some fixed data tests, else is the
    // number of passes of random data to send.
    //

    public void initiate (int n)
    throws IOException
    {
        // System.out.println ("Initiating N = " + n);

        if (n == 0)
            initiateConst ();
        else if (n < 0)
            System.out.println ("** ERROR:  initiate forever ??");
        else
            for ( ; n > 0; n -= 1) {
                initiateRandom ();
            }
    }


    public void respond (int n)
    throws IOException
    {
        if (n == 0)
            respondConst ();
        else if (n < 0)                 // n < 0 == respond forever
            while (true)
                respondRandom ();
        else
            while (n-- > 0)
                respondRandom ();
    }


    //
    // Test passing of fixed size (and content) data.
    //
    // For SSL, one test goal is to ensure that all the basic
    // block cipher padding sizes get banged on.  SSLv3 ciphers
    // are all the same block size, but there are larger sizes
    // coming along.  (Big blocks in hardware can be fast!!)
    //

    private static final int MAX_BLOCKSIZE = 8 * 2;

    private void writeConstData (int n)
    throws IOException
    {
        if (n <= 0)
            return;

        byte buf [] = new byte [n];

        for (int i = 0; i < n; i++)
            buf [i] = (byte) i;

        out.write (buf);

        /*
        System.out.println (Thread.currentThread ().getName ()
            + " wrote const data size = " + n);
        */
    }

    private void readConstData (int n)
    throws IOException
    {
        if (n <= 0)
            return;

        byte buf [] = new byte [n];

        in.read (buf);

        for (int i = 0; i < n; i++)
            if (buf [i] != (byte) i)
                throw new IOException ("const data was incorrect, "
                    + "n = " + n + ", i = " + i);

        /*
        System.out.println (Thread.currentThread ().getName ()
            + " read const data size = " + n);
        */
    }

    private void initiateConst ()
    throws IOException
    {
        for (int i = 1; i <= MAX_BLOCKSIZE; i++) {
            writeConstData (i);
            readConstData (i);
        }

    }

    private void respondConst ()
    throws IOException
    {
        for (int i = 1; i <= MAX_BLOCKSIZE; i++) {
            readConstData (i);
            writeConstData (i);
        }
    }


    //
    // Test passing of random size (and content) data.
    //
    // For SSL, one test goal is to ensure that all the basic
    // record sizes get banged on.  Traffic will normally
    // be bimodal (small packets, and big ones) and we give
    // a half-hearted effort at emulating that -- no real
    // statistics to back up this particular distribution.
    //

    private static final int MAX_RECORDSIZE = 16384 * 2;

    private int nextRecordSize ()
    {
        double  d = prng.nextGaussian ();
        int     n;

        // assume 1/3 traffic is "big", less variance
        if ((prng.nextInt () % 3)  == 0) {
            n = (int) (d * 2048);
            n += 15 * 1024;

        // ... and the rest is smaller, much variance
        } else {
            n = (int) (d * 4096);
            n += 1024;
        }

        if (n < 0)
            return nextRecordSize ();
        else if (n > MAX_RECORDSIZE)
            return MAX_RECORDSIZE;
        else
            return n;
    }


    private void writeRandomData ()
    throws IOException
    {
        int n = nextRecordSize ();
        byte buf [] = new byte [n];

        // System.out.println ("write, size = " + n);

        prng.nextBytes (buf);

        writeInt (n);
        out.write (buf);
    }

    private void readRandomData ()
    throws IOException
    {
        int     n = readInt ();
        byte    actual [] = new byte [n];

        readFully (actual);

        if (compareRandom) {
            byte        expected [];

            if (n != nextRecordSize ())
                throw new IOException ("wrong record size");

            expected = new byte [n];
            prng.nextBytes (expected);

            for (int i = 0; i < n; i++)
                if (actual [i] != expected [i])
                    throw new IOException ("random data was incorrect, "
                        + "n = " + n + ", i = " + i);
        }
    }

    private void initiateRandom ()
    throws IOException
    {
        writeRandomData ();
        readRandomData ();

    }

    private void respondRandom ()
    throws IOException
    {
        readRandomData ();
        writeRandomData ();
    }


    private void readFully (byte buf [])
    throws IOException
    {
        int len = buf.length;
        int offset = 0;
        int value;

        while (len > 0) {
            value = in.read (buf, offset, len);
            if (value == -1)
                throw new EOFException ("read buffer");
            offset += value;
            len -= value;
        }
    }


    private int readInt ()
    throws IOException
    {
        int b0, b1, b2, b3;
        int n;

        b0 = in.read ();
        b1 = in.read ();
        b2 = in.read ();
        b3 = in.read ();

        if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
            throw new EOFException ();

        /*
        System.out.println ("READ:   b0 = " + b0 + ", b1 = " + b1
            + ", b2 = " + b2 + ", b3 = " + b3);
        */

        n =  (b3 & 0x0ff);
        n |= (b2 & 0x0ff) << 8;
        n |= (b1 & 0x0ff) << 16;
        n |= (b0 & 0x0ff) << 24;
        return n;
    }

    private void writeInt (int n)
    throws IOException
    {
        int b0, b1, b2, b3;

        b3 = n & 0x0ff;
        n >>= 8;
        b2 = n & 0x0ff;
        n >>= 8;
        b1 = n & 0x0ff;
        n >>= 8;
        b0 = n & 0x0ff;

        /*
        System.out.println ("WRITE:  b0 = " + b0 + ", b1 = " + b1
            + ", b2 = " + b2 + ", b3 = " + b3);
        */

        out.write (b0);
        out.write (b1);
        out.write (b2);
        out.write (b3);
    }
}
