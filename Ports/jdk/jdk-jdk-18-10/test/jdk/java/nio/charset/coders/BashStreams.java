/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Stochastic test of charset-based streams
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;


public class BashStreams {

    static final PrintStream log = System.err;


    static class CharacterGenerator {

        private final Random rand;
        private final int max;
        private final int limit;
        private int count = 0;

        CharacterGenerator(long seed, String csn, int limit) {
            rand = new Random(seed);
            this.max = Character.MAX_CODE_POINT + 1;
            this.limit = limit;
        }

        private char[] saved = new char[10];
        private int savedCount = 0;

        void push(char c) {
            saved[savedCount++] = c;
            count--;
        }

        int count() {
            return count;
        }

        boolean hasNext() {
            return count < limit;
        }

        char next() {
            if (count >= limit)
                throw new RuntimeException("EOF");
            if (savedCount > 0) {
                savedCount--;
                count++;
                return saved[savedCount];
            }
            int c;
            for (;;) {
                c = rand.nextInt(max);
                if ((Character.isBmpCodePoint(c)
                     && (Character.isSurrogate((char) c)
                         || (c == 0xfffe) || (c == 0xffff))))
                    continue;
                if (Character.isSupplementaryCodePoint(c)
                        && (count == limit - 1))
                    continue;
                break;
            }
            count++;
            if (Character.isSupplementaryCodePoint(c)) {
                count++;
                push(Character.lowSurrogate(c));
                return Character.highSurrogate(c);
            }
            return (char)c;
        }

    }


    static void mismatch(String csn, int count, char c, char d) {
        throw new RuntimeException(csn + ": Mismatch at count "
                                   + count
                                   + ": " + Integer.toHexString(c)
                                   + " != "
                                   + Integer.toHexString(d));
    }

    static void mismatchedEOF(String csn, int count, int cgCount) {
        throw new RuntimeException(csn + ": Mismatched EOFs: "
                                   + count
                                   + " != "
                                   + cgCount);
    }


    static class Sink                   // One abomination...
        extends OutputStream
        implements WritableByteChannel
    {

        private final String csn;
        private final CharacterGenerator cg;
        private int count = 0;

        Sink(String csn, long seed) {
            this.csn = csn;
            this.cg = new CharacterGenerator(seed, csn, Integer.MAX_VALUE);
        }

        public void write(int b) throws IOException {
            write (new byte[] { (byte)b }, 0, 1);
        }

        private int check(byte[] ba, int off, int len) throws IOException {
            String s = new String(ba, off, len, csn);
            int n = s.length();
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                char d = cg.next();
                if (c != d) {
                    if (c == '?') {
                        if (Character.isHighSurrogate(d))
                            cg.next();
                        continue;
                    }
                    mismatch(csn, count + i, c, d);
                }
            }
            count += n;
            return len;
        }

        public void write(byte[] ba, int off, int len) throws IOException {
            check(ba, off, len);
        }

        public int write(ByteBuffer bb) throws IOException {
            int n = check(bb.array(),
                          bb.arrayOffset() + bb.position(),
                          bb.remaining());
            bb.position(bb.position() + n);
            return n;
        }

        public void close() {
            count = -1;
        }

        public boolean isOpen() {
            return count >= 0;
        }

    }

    static void testWrite(String csn, int limit, long seed, Writer w)
        throws IOException
    {
        Random rand = new Random(seed);
        CharacterGenerator cg = new CharacterGenerator(seed, csn,
                                                       Integer.MAX_VALUE);
        int count = 0;
        char[] ca = new char[16384];

        int n = 0;
        while (count < limit) {
            n = rand.nextInt(ca.length);
            for (int i = 0; i < n; i++)
                ca[i] = cg.next();
            w.write(ca, 0, n);
            count += n;
        }
        if (Character.isHighSurrogate(ca[n - 1]))
            w.write(cg.next());
        w.close();
    }

    static void testStreamWrite(String csn, int limit, long seed)
        throws IOException
    {
        log.println("  write stream");
        testWrite(csn, limit, seed,
                  new OutputStreamWriter(new Sink(csn, seed), csn));
    }

    static void testChannelWrite(String csn, int limit, long seed)
        throws IOException
    {
        log.println("  write channel");
        testWrite(csn, limit, seed,
                  Channels.newWriter(new Sink(csn, seed),
                                     Charset.forName(csn)
                                     .newEncoder()
                                     .onMalformedInput(CodingErrorAction.REPLACE)
                                     .onUnmappableCharacter(CodingErrorAction.REPLACE),
                                     8192));
    }


    static class Source                 // ... and another
        extends InputStream
        implements ReadableByteChannel
    {

        private final String csn;
        private final CharsetEncoder enc;
        private final CharacterGenerator cg;
        private int count = 0;

        Source(String csn, long seed, int limit) {
            this.csn = csn.startsWith("\1") ? csn.substring(1) : csn;
            this.enc = Charset.forName(this.csn).newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.cg = new CharacterGenerator(seed, csn, limit);
        }

        public int read() throws IOException {
            byte[] b = new byte[1];
            read(b);
            return b[0];
        }

        private CharBuffer cb = CharBuffer.allocate(8192);
        private ByteBuffer bb = null;

        public int read(byte[] ba, int off, int len) throws IOException {
            if (!cg.hasNext())
                return -1;
            int end = off + len;
            int i = off;
            while (i < end) {
                if ((bb == null) || !bb.hasRemaining()) {
                    cb.clear();
                    while (cb.hasRemaining()) {
                        if (!cg.hasNext())
                            break;
                        char c = cg.next();
                        if (Character.isHighSurrogate(c)
                                && cb.remaining() == 1) {
                            cg.push(c);
                            break;
                        }
                        cb.put(c);
                    }
                    cb.flip();
                    if (!cb.hasRemaining())
                        break;
                    bb = enc.encode(cb);
                }
                int d = Math.min(bb.remaining(), end - i);
                bb.get(ba, i, d);
                i += d;
            }
            return i - off;
        }

        public int read(ByteBuffer bb) throws IOException {
            int n = read(bb.array(),
                         bb.arrayOffset() + bb.position(),
                         bb.remaining());
            if (n >= 0)
                bb.position(bb.position() + n);
            return n;
        }

        public void close() {
            count = -1;
        }

        public boolean isOpen() {
            return count != -1;
        }

    }

    static void testRead(String csn, int limit, long seed, int max,
                         Reader rd)
        throws IOException
    {
        Random rand = new Random(seed);
        CharacterGenerator cg = new CharacterGenerator(seed, csn, limit);
        int count = 0;
        char[] ca = new char[16384];

        int n = 0;
        while (count < limit) {
            int rn = rand.nextInt(ca.length);
            n = rd.read(ca, 0, rn);
            if (n < 0)
                break;
            for (int i = 0; i < n; i++) {
                char c = ca[i];
                if (!cg.hasNext())
                    mismatchedEOF(csn, count + i, cg.count());
                char d = cg.next();
                if (c == '?') {
                    if (Character.isHighSurrogate(d)) {
                        cg.next();
                        continue;
                    }
                    if (d > max)
                        continue;
                }
                if (c != d)
                    mismatch(csn, count + i, c, d);
            }
            count += n;
        }
        if (cg.hasNext())
            mismatchedEOF(csn, count, cg.count());
        rd.close();
    }

    static void testStreamRead(String csn, int limit, long seed, int max)
        throws IOException
    {
        log.println("  read stream");
        testRead(csn, limit, seed, max,
                 new InputStreamReader(new Source(csn, seed, limit), csn));
    }

    static void testChannelRead(String csn, int limit, long seed, int max)
        throws IOException
    {
        log.println("  read channel");
        testRead(csn, limit, seed, max,
                 Channels.newReader(new Source(csn, seed, limit), csn));
    }


    static final int LIMIT = 1 << 21;

    static void test(String csn, int limit, long seed, int max)
        throws Exception
    {
        log.println();
        log.println(csn);

        testStreamWrite(csn, limit, seed);
        testChannelWrite(csn, limit, seed);
        testStreamRead(csn, limit, seed, max);
        testChannelRead(csn, limit, seed, max);
    }

    public static void main(String[] args) throws Exception {

        if (System.getProperty("os.arch").equals("ia64")) {
            // This test requires 54 minutes on an Itanium-1 processor
            return;
        }

        int ai = 0, an = args.length;

        int limit;
        if (ai < an)
            limit = 1 << Integer.parseInt(args[ai++]);
        else
            limit = LIMIT;
        log.println("limit = " + limit);

        long seed;
        if (ai < an)
            seed = Long.parseLong(args[ai++]);
        else
            seed = System.currentTimeMillis();
        log.println("seed = " + seed);

        test("UTF-8", limit, seed, Integer.MAX_VALUE);
        test("US-ASCII", limit, seed, 0x7f);
        test("ISO-8859-1", limit, seed, 0xff);

    }

}
