/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4679743
 * @summary Test basic functionality of DeflaterInputStream and InflaterOutputStream
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class DeflateIn_InflateOut {
    private static byte[] data = new byte[1024 * 1024];

    private static ByteArrayInputStream bais;
    private static DeflaterInputStream dis;

    private static ByteArrayOutputStream baos;
    private static InflaterOutputStream ios;

    private static Inflater reset(byte[] dict) {
        bais = new ByteArrayInputStream(data);
        if (dict == null) {
            dis = new DeflaterInputStream(bais);
        } else {
            Deflater def = new Deflater();
            def.setDictionary(dict);
            dis = new DeflaterInputStream(bais, def);
        }

        baos = new ByteArrayOutputStream();
        if (dict == null) {
            ios = new InflaterOutputStream(baos);
            return null;
        } else {
            Inflater inf = new Inflater();
            ios = new InflaterOutputStream(baos, inf);
            return inf;
        }
    }

    private static void reset() {
        reset(null);
    }

    /** Check byte arrays read/write. */
    private static void ArrayReadWrite() throws Throwable {
        byte[] buf = new byte[512];

        reset();
        check(dis.available() == 1);
        for (;;) {
            int len = dis.read(buf, 0, buf.length);
            if (len < 0) {
                break;
            } else {
                ios.write(buf, 0, len);
            }
        }
        check(dis.available() == 0);
        ios.close();
        check(Arrays.equals(data, baos.toByteArray()));
    }

    /** Check byte arrays read, single byte write */
    private static void ArrayReadByteWrite() throws Throwable {
        byte[] buf = new byte[512];

        reset();
        for (;;) {
            int len = dis.read(buf, 0, buf.length);
            if (len <= 0) {
                break;
            } else {
                for (int i = 0; i < len; i++) {
                    byte x = (byte) (buf[i] & 0xff);
                    ios.write(x);
                }
            }
        }
        check(dis.available() == 0);
        ios.close();
        check(Arrays.equals(data, baos.toByteArray()));
    }

    /** Check single byte read, byte array write.
     * <p>
     * Note that this test relies on the vaule from DeflaterInputStream.read()
     * to determine when to stop reading.
     */
    private static void ByteReadArrayWrite() throws Throwable {
        byte[] buf = new byte[8192];
        int off = 0;

        reset();
        int datum = dis.read();
        while (datum != -1) {
            if (off == 8192) {
                ios.write(buf, 0, off);
                off = 0;
            }
            buf[off++] = (byte) (datum & 0xff);
            datum = dis.read();
        }
        if (off > 0) {
            ios.write(buf, 0, off);
        }
        ios.close();
        check(Arrays.equals(data, baos.toByteArray()));
    }

    /** Check single byte read/write.
     * <p>
     * Note that this test relies on DeflaterInputStream.available() to
     * determine when to stop reading.
     */
    private static void ByteReadByteWrite() throws Throwable {
        byte[] buf = new byte[512];
        boolean reachEOF = false;

        reset();
        while (dis.available() == 1) {
            int datum = dis.read();
            if (datum == -1) {
                reachEOF = true;
            } else {
                if (datum < 0 || datum > 255) {
                    fail("datum out of range: " + datum);
                }
                ios.write(datum);
            }
        }
        dis.close();
        ios.close();
        check(data[0] == baos.toByteArray()[0]);
    }

    /** Check skip(). */
    private static void SkipBytes() throws Throwable {
        byte[] buf = new byte[512];
        int numReadable = 0;

        // Count number of bytes that are read
        reset();
        check(dis.available() == 1);
        for (;;) {
            int count = dis.read(buf, 0, buf.length);
            if (count < 0) {
                break;
            } else {
                numReadable += count;
            }
        }
        check(dis.available() == 0);

        // Verify that skipping the first several bytes works.
        reset();
        int numNotSkipped = 0;
        int numSkipBytes = 2053; // arbitrarily chosen prime
        check(dis.skip(numSkipBytes) == numSkipBytes);
        for (int i = 0; ; i++) {
            int count = dis.read(buf, 0, buf.length);
            if (count < 0) {
                break;
            } else {
                numNotSkipped += count;
            }
        }
        check(numNotSkipped + numSkipBytes == numReadable);

        // Verify that skipping some bytes mid-stream works.
        reset();
        numNotSkipped = 0;
        numSkipBytes = 8887; // arbitrarily chosen prime
        for (int i = 0; ; i++) {
            if (i == 13) { // Arbitrarily chosen
                check(dis.skip(numSkipBytes) == numSkipBytes);
            } else {
                int count = dis.read(buf, 0, buf.length);
                if (count < 0) {
                    break;
                } else {
                    numNotSkipped += count;
                }
            }
        }
        check(numNotSkipped + numSkipBytes == numReadable);

        // Verify that skipping the last N bytes works.
        reset();
        numNotSkipped = 0;
        numSkipBytes = 6449; // arbitrarily chosen prime
        for (int i = 0; ; i++) {
            if (numNotSkipped + numSkipBytes > numReadable) {
                numSkipBytes = numReadable - numNotSkipped;
                check(dis.skip(numSkipBytes) == numSkipBytes);
                check(dis.read(buf, 0, buf.length) == -1);
                check(dis.available() == 0);
            } else {
                int count = dis.read(buf, 0, buf.length);
                if (count < 0) {
                    break;
                } else {
                    numNotSkipped += count;
                }
            }
        }
        check(numNotSkipped + numSkipBytes == numReadable);
    }

    /** Check "needsDictionary()". */
    private static void NeedsDictionary() throws Throwable {
        byte[] dict = {1, 2, 3, 4};
        Adler32 adler32 = new Adler32();
        adler32.update(dict);
        long checksum = adler32.getValue();
        byte[] buf = new byte[512];

        Inflater inf = reset(dict);
        check(dis.available() == 1);
        boolean dictSet = false;
        for (;;) {
            int len = dis.read(buf, 0, buf.length);
            if (len < 0) {
                break;
            } else {
                try {
                    ios.write(buf, 0, len);
                    if (dictSet == false) {
                        check(false, "Must throw ZipException without dictionary");
                        return;
                    }
                } catch (ZipException ze) {
                    check(dictSet == false, "Dictonary must be set only once");
                    check(checksum == inf.getAdler(), "Incorrect dictionary");
                    inf.setDictionary(dict);
                    // After setting the dictionary, we have to flush the
                    // InflaterOutputStream now in order to consume all the
                    // pending input data from the last, failed call to "write()".
                    ios.flush();
                    dictSet = true;
                }
            }
        }
        check(dis.available() == 0);
        ios.close();
        check(Arrays.equals(data, baos.toByteArray()));
    }

    public static void realMain(String[] args) throws Throwable {
        new Random(new Date().getTime()).nextBytes(data);

        ArrayReadWrite();

        ArrayReadByteWrite();

        ByteReadArrayWrite();

        ByteReadByteWrite();

        SkipBytes();

        NeedsDictionary();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() { fail(null); }
    static void fail(String msg) {
        failed++;
        if (msg != null) {
            System.err.println(msg);
        }
        Thread.dumpStack();
    }
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void check(boolean cond, String msg) {if (cond) pass(); else fail(msg);}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
