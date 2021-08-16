/*
 * Copyright 2009 Google, Inc.  All Rights Reserved.
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
 * @bug 4206909 4813885 8191918
 * @summary Test basic functionality of DeflaterOutputStream/InflaterInputStream
 *          and GZIPOutputStream/GZIPInputStream, including flush
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class InflateIn_DeflateOut {

    private static class PairedInputStream extends ByteArrayInputStream {
        private PairedOutputStream out = null;
        private Random random;

        public PairedInputStream() {
            // The ByteArrayInputStream needs to start with a buffer, but we
            // need to set it to have no data
            super(new byte[1]);
            count = 0;
            pos = 0;
            random = new Random(new Date().getTime());
        }

        public void setPairedOutputStream(PairedOutputStream out) {
            this.out = out;
        }

        private void maybeFlushPair() {
            if (random.nextInt(100) < 10) {
                out.flush();
            }
        }

        public int read() {
            maybeFlushPair();
            return super.read();
        }

        public int read(byte b[], int off, int len) {
            maybeFlushPair();
            return super.read(b, off, len);
        }

        public void addBytes(byte[] bytes, int len) {
            int oldavail = count - pos;
            int newcount = oldavail + len;
            byte[] newbuf = new byte[newcount];
            System.arraycopy(buf, pos, newbuf, 0, oldavail);
            System.arraycopy(bytes, 0, newbuf, oldavail, len);
            pos = 0;
            count = newcount;
            buf = newbuf;
        }
    }

    private static class PairedOutputStream extends ByteArrayOutputStream {
        private PairedInputStream pairedStream = null;

        public PairedOutputStream(PairedInputStream inputPair) {
            super();
            this.pairedStream = inputPair;
        }

        public void flush() {
            if (count > 0) {
                pairedStream.addBytes(buf, count);
                reset();
            }
        }

        public void close() {
            flush();
        }
    }

    private static boolean readFully(InputStream in, byte[] buf, int length)
            throws IOException {
        int pos = 0;
        int n;
        while ((n = in.read(buf, pos, length - pos)) > 0) {
            pos += n;
            if (pos == length) return true;
        }
        return false;
    }

    private static boolean readLineIfAvailable(InputStream in, StringBuilder sb)
            throws IOException {
        try {
            while (in.available() > 0) {
                int i = in.read();
                if (i < 0) break;
                char c = (char) (((byte) i) & 0xff);
                sb.append(c);
                if (c == '\n') return true;
            }
        } catch (EOFException e) {
          // empty
        }
        return false;
    }

    /** Check that written, closed and read */
    private static void WriteCloseRead() throws Throwable {
        Random random = new Random(new Date().getTime());

        PairedInputStream pis = new PairedInputStream();
        InflaterInputStream iis = new InflaterInputStream(pis);

        PairedOutputStream pos = new PairedOutputStream(pis);
        pis.setPairedOutputStream(pos);

        byte[] data = new byte[random.nextInt(1024 * 1024)];
        byte[] buf = new byte[data.length];
        random.nextBytes(data);

        try (DeflaterOutputStream dos = new DeflaterOutputStream(pos, true)) {
            dos.write(data);
        }
        check(readFully(iis, buf, buf.length));
        check(Arrays.equals(data, buf));
    }

    private static void TestFlushableGZIPOutputStream() throws Throwable {
        var random = new Random(new Date().getTime());

        var byteOutStream = new ByteArrayOutputStream();
        var output = new FlushableGZIPOutputStream(byteOutStream);

        var data = new byte[random.nextInt(1024 * 1024)];
        var buf = new byte[data.length];
        random.nextBytes(data);

        output.write(data);
        for (int i=0; i<data.length; i++) {
            output.write(data[i]);
        }
        output.flush();
        for (int i=0; i<data.length; i++) {
            output.write(data[i]);
        }
        output.write(data);
        output.close();

        var baos = new ByteArrayOutputStream();
        try (var gzis = new GZIPInputStream(new
                ByteArrayInputStream(byteOutStream.toByteArray()))) {
            gzis.transferTo(baos);
        }
        var decompressedBytes = baos.toByteArray();
        check(decompressedBytes.length == data.length * 4);
    }

    private static void check(InputStream is, OutputStream os)
        throws Throwable
    {
        Random random = new Random(new Date().getTime());
       // Large writes
        for (int x = 0; x < 200 ; x++) {
            // byte[] data = new byte[random.nextInt(1024 * 1024)];
            byte[] data = new byte[1024];
            byte[] buf = new byte[data.length];
            random.nextBytes(data);

            os.write(data);
            os.flush();
            check(readFully(is, buf, buf.length));
            check(Arrays.equals(data, buf));
        }

        // Small writes
        for (int x = 0; x < 2000 ; x++) {
            byte[] data = new byte[random.nextInt(20) + 10];
            byte[] buf = new byte[data.length];
            random.nextBytes(data);

            os.write(data);
            os.flush();
            if (!readFully(is, buf, buf.length)) {
                fail("Didn't read full buffer of " + buf.length);
            }
            check(Arrays.equals(data, buf));
        }

        String quit = "QUIT\r\n";

        // Close it out
        os.write(quit.getBytes());
        os.close();

        StringBuilder sb = new StringBuilder();
        check(readLineIfAvailable(is, sb));
        equal(sb.toString(), quit);
    }

    /** Check that written, flushed and read */
    private static void WriteFlushRead() throws Throwable {
        PairedInputStream pis = new PairedInputStream();
        InflaterInputStream iis = new InflaterInputStream(pis);

        PairedOutputStream pos = new PairedOutputStream(pis);
        pis.setPairedOutputStream(pos);
        DeflaterOutputStream dos = new DeflaterOutputStream(pos, true);

        check(iis, dos);
    }

    private static void GZWriteFlushRead() throws Throwable {
        PairedInputStream pis = new PairedInputStream();
        PairedOutputStream pos = new PairedOutputStream(pis);
        pis.setPairedOutputStream(pos);

        GZIPOutputStream gos = new GZIPOutputStream(pos, true);
        gos.flush();  // flush the head out, so gis can read
        GZIPInputStream gis = new GZIPInputStream(pis);

        check(gis, gos);
    }

    private static void checkLOP(InputStream is, OutputStream os)
        throws Throwable
    {
        boolean flushed = false;
        int count = 0;

        // Do at least a certain number of lines, but too many without a
        // flush means this test isn't testing anything
        while ((count < 10 && flushed) || (count < 1000 && !flushed)) {
            String command = "PING " + count + "\r\n";
            os.write(command.getBytes());

            StringBuilder buf = new StringBuilder();
            if (!readLineIfAvailable(is, buf)) {
                flushed = true;
                os.flush();
                check(readLineIfAvailable(is, buf));
            }
            equal(buf.toString(), command);
            count++;
        }
        check(flushed);
    }

    /** Validate that we need to use flush at least once on a line
     * oriented protocol */
    private static void LineOrientedProtocol() throws Throwable {
        PairedInputStream pis = new PairedInputStream();
        InflaterInputStream iis = new InflaterInputStream(pis);

        PairedOutputStream pos = new PairedOutputStream(pis);
        pis.setPairedOutputStream(pos);
        DeflaterOutputStream dos = new DeflaterOutputStream(pos, true);

        checkLOP(iis, dos);
    }

    private static void GZLineOrientedProtocol() throws Throwable {
        PairedInputStream pis = new PairedInputStream();
        PairedOutputStream pos = new PairedOutputStream(pis);
        pis.setPairedOutputStream(pos);

        GZIPOutputStream gos = new GZIPOutputStream(pos, true);
        gos.flush();  // flush the head out, so gis can read
        GZIPInputStream gis = new GZIPInputStream(pis);

        checkLOP(gis, gos);
    }

    public static void realMain(String[] args) throws Throwable {
        WriteCloseRead();
        WriteFlushRead();
        LineOrientedProtocol();
        GZWriteFlushRead();
        GZLineOrientedProtocol();
        TestFlushableGZIPOutputStream();
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}

class FlushableGZIPOutputStream extends GZIPOutputStream {
    public FlushableGZIPOutputStream(OutputStream os) throws IOException {
        super(os);
    }

    private static final byte[] EMPTYBYTEARRAY = new byte[0];
    private boolean hasData = false;

    /**
     * Here we make sure we have received data, so that the header has been for
     * sure written to the output stream already.
     */
    @Override
    public synchronized void write(byte[] bytes, int i, int i1)
            throws IOException {
        super.write(bytes, i, i1);
        hasData = true;
    }

    @Override
    public synchronized void write(int i) throws IOException {
        super.write(i);
        hasData = true;
    }

    @Override
    public synchronized void write(byte[] bytes) throws IOException {
        super.write(bytes);
        hasData = true;
    }

    @Override
    public synchronized void flush() throws IOException {
        if (!hasData) {
            return; // do not allow the gzip header to be flushed on its own
        }

        // trick the deflater to flush
        /**
         * Now this is tricky: We force the Deflater to flush its data by
         * switching compression level. As yet, a perplexingly simple workaround
         * for
         * http://developer.java.sun.com/developer/bugParade/bugs/4255743.html
         */
        if (!def.finished()) {
            def.setInput(EMPTYBYTEARRAY, 0, 0);

            def.setLevel(Deflater.NO_COMPRESSION);
            deflate();

            def.setLevel(Deflater.DEFAULT_COMPRESSION);
            deflate();

            out.flush();
        }

        hasData = false; // no more data to flush
    }

    /*
     * Keep on calling deflate until it runs dry. The default implementation
     * only does it once and can therefore hold onto data when they need to be
     * flushed out.
     */
    @Override
    protected void deflate() throws IOException {
        int len;
        do {
            len = def.deflate(buf, 0, buf.length);
            if (len > 0) {
                out.write(buf, 0, len);
            }
        } while (len != 0);
    }

}
