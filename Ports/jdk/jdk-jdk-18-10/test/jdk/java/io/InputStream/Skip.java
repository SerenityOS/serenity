/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4016710 6516099
 * @summary check for correct implementation of InputStream.skip{NBytes}
 */

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;

public class Skip {
    private static final int EOF = -1;

    private static void dotest(InputStream in, int curpos, long total,
                               long toskip, long expected) throws Exception {
        try {
            System.err.println("\n\nCurrently at pos = " + curpos +
                               "\nTotal bytes in the Stream = " + total +
                               "\nNumber of bytes to skip = " + toskip +
                               "\nNumber of bytes that should be skipped = " +
                               expected);

            long skipped = in.skip(toskip);

            System.err.println("actual number skipped: "+ skipped);

            if ((skipped < 0) || (skipped > expected)) {
                throw new RuntimeException("Unexpected byte count skipped");
            }
        } catch (IOException e) {
            System.err.println("IOException is thrown: " + e);
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected " + e + " is thrown!");
        }
    }

    private static void dotestExact(MyInputStream in, long curpos, long total,
        long toskip, boolean expectIOE, boolean expectEOFE) {

        System.err.println("\n\nCurrently at pos = " + curpos +
                           "\nTotal bytes in the Stream = " + total +
                           "\nNumber of bytes to skip = " + toskip);

        try {
            long pos = in.position();
            assert pos == curpos : pos + " != " + curpos;
            in.skipNBytes(toskip);
            if (in.position() != pos + (toskip < 0 ? 0 : toskip)) {
                throw new RuntimeException((in.position() - pos) +
                    " bytes skipped; expected " + toskip);
            }
        } catch (EOFException eofe) {
            if (!expectEOFE) {
                throw new RuntimeException("Unexpected EOFException", eofe);
            }
            System.err.println("Caught expected EOFException");
        } catch (IOException ioe) {
            if (!expectIOE) {
                throw new RuntimeException("Unexpected IOException", ioe);
            }
            System.err.println("Caught expected IOException");
        }
    }

    public static void main( String argv[] ) throws Exception {
        MyInputStream in = new MyInputStream(11);

        // test for negative skip
        dotest(in,  0, 11, -23,  0);

        // check for skip beyond EOF starting from before EOF
        dotest(in,  0, 11,  20, 11);

        // check for skip after EOF
        dotest(in, EOF, 11,  20,  0);

        in = new MyInputStream(9000);

        // check for skip equal to the read chunk size in InputStream.java
        dotest(in,  0, 9000, 2048, 2048);

        // check for skip larger than the read chunk size in InputStream.java
        dotest(in, 2048, 9000, 5000, 5000);

        // check for skip beyond EOF starting from before EOF
        dotest(in, 7048, 9000, 5000, 1952);

        in = new MyInputStream(5000);

        // check for multiple chunk reads
        dotest(in, 0, 5000, 6000, 5000);

        /*
         * check for skip larger than Integer.MAX_VALUE
         * (Takes about 2 hrs on a sparc ultra-1)
         * long total = (long)Integer.MAX_VALUE + (long)10;
         * long toskip = total - (long)6;
         * in = new MyInputStream(total);
         * dotest(in, 0, total, toskip, toskip);
         */

        // tests for skipping an exact number of bytes

        final long streamLength = Long.MAX_VALUE;
        in = new MyInputStream(streamLength);

        // negative skip: OK
        dotestExact(in, 0, streamLength, -1, false, false);

        // negative skip at EOF: OK
        in.position(streamLength);
        dotestExact(in, streamLength, streamLength, -1, false, false);
        in.position(0);

        // zero skip: OK
        dotestExact(in, 0, streamLength, 0, false, false);

        // zero skip at EOF: OK
        in.position(streamLength);
        dotestExact(in, streamLength, streamLength, 0, false, false);

        // skip(1) at EOF: EOFE
        dotestExact(in, streamLength, streamLength, 1, false, true);
        in.position(0);

        final long n = 31; // skip count
        long pos = 0;

        // skip(n) returns negative value: IOE
        in.setState(-1, 100);
        dotestExact(in, pos, streamLength, n, true, false);

        // skip(n) returns n + 1: IOE
        in.setState(n + 1, 100);
        dotestExact(in, pos, streamLength, n, true, false);
        pos += n + 1;

        // skip(n) returns n/2 but only n/4 subsequent reads succeed: EOFE
        in.setState(n/2, n/2 + n/4);
        dotestExact(in, pos, streamLength, n, false, true);
        pos += n/2 + n/4;

        // skip(n) returns n/2 but n - n/2 subsequent reads succeed: OK
        in.setState(n/2, n);
        dotestExact(in, pos, streamLength, n, false, false);
        pos += n;
    }
}

class MyInputStream extends InputStream {
    private static final int EOF = -1;

    private final long endoffile;

    private long readctr = 0;

    private boolean isStateSet = false;
    private long skipReturn;
    private long readLimit;

    public MyInputStream(long endoffile) {
        this.endoffile = endoffile;
    }

    /**
     * Limits the behavior of skip() and read().
     *
     * @param skipReturn the value to be returned by skip()
     * @param maxReads   the maximum number of reads past the current position
     *                   before EOF is reached
     */
    public void setState(long skipReturn, long maxReads) {
        this.skipReturn = skipReturn;
        this.readLimit = readctr + maxReads;
        isStateSet = true;
    }

    public int read() {
        if (readctr == endoffile ||
            (isStateSet && readctr >= readLimit)) {
            return EOF;
        }
        else {
            readctr++;
            return 0;
        }
    }

    public int available() { return 0; }

    public long position() { return readctr; }

    public void position(long pos) {
        readctr = pos < 0 ? 0 : Math.min(pos, endoffile);
    }

    public long skip(long n) throws IOException {
        if (isStateSet) {
            return skipReturn < 0 ? skipReturn : super.skip(skipReturn);
        }

        // InputStream skip implementation.
        return super.skip(n); // readctr is implicitly incremented
    }
}
