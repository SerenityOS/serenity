/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6831794 6229811
 * @summary Test EUC_TW charset
 * @modules java.base/sun.nio.cs
 */

import java.nio.charset.*;
import java.nio.*;
import java.util.*;

public class TestEUC_TW {

    static class Time {
        long t;
    }
    static int iteration = 100;

    static char[] decode(byte[] bb, Charset cs, boolean testDirect, Time t)
        throws Exception {
        String csn = cs.name();
        CharsetDecoder dec = cs.newDecoder();
        ByteBuffer bbf;
        CharBuffer cbf;
        if (testDirect) {
            bbf = ByteBuffer.allocateDirect(bb.length);
            cbf = ByteBuffer.allocateDirect(bb.length*2).asCharBuffer();
            bbf.put(bb);
        } else {
            bbf = ByteBuffer.wrap(bb);
            cbf = CharBuffer.allocate(bb.length);
        }
        CoderResult cr = null;
        long t1 = System.nanoTime()/1000;
        for (int i = 0; i < iteration; i++) {
            bbf.rewind();
            cbf.clear();
            dec.reset();
            cr = dec.decode(bbf, cbf, true);
        }
        long t2 = System.nanoTime()/1000;
        if (t != null)
        t.t = (t2 - t1)/iteration;
        if (cr != CoderResult.UNDERFLOW) {
            System.out.println("DEC-----------------");
            int pos = bbf.position();
            System.out.printf("  cr=%s, bbf.pos=%d, bb[pos]=%x,%x,%x,%x%n",
                              cr.toString(), pos,
                              bb[pos++]&0xff, bb[pos++]&0xff,bb[pos++]&0xff, bb[pos++]&0xff);
            throw new RuntimeException("Decoding err: " + csn);
        }
        char[] cc = new char[cbf.position()];
        cbf.flip(); cbf.get(cc);
        return cc;

    }

    static CoderResult decodeCR(byte[] bb, Charset cs, boolean testDirect)
        throws Exception {
        CharsetDecoder dec = cs.newDecoder();
        ByteBuffer bbf;
        CharBuffer cbf;
        if (testDirect) {
            bbf = ByteBuffer.allocateDirect(bb.length);
            cbf = ByteBuffer.allocateDirect(bb.length*2).asCharBuffer();
            bbf.put(bb).flip();
        } else {
            bbf = ByteBuffer.wrap(bb);
            cbf = CharBuffer.allocate(bb.length);
        }
        return dec.decode(bbf, cbf, true);
    }

    static byte[] encode(char[] cc, Charset cs, boolean testDirect, Time t)
        throws Exception {
        ByteBuffer bbf;
        CharBuffer cbf;
        CharsetEncoder enc = cs.newEncoder();
        String csn = cs.name();
        if (testDirect) {
            bbf = ByteBuffer.allocateDirect(cc.length * 4);
            cbf = ByteBuffer.allocateDirect(cc.length * 2).asCharBuffer();
            cbf.put(cc).flip();
        } else {
            bbf = ByteBuffer.allocate(cc.length * 4);
            cbf = CharBuffer.wrap(cc);
        }
        CoderResult cr = null;
        long t1 = System.nanoTime()/1000;
        for (int i = 0; i < iteration; i++) {
            cbf.rewind();
            bbf.clear();
            enc.reset();
            cr = enc.encode(cbf, bbf, true);
        }
        long t2 = System.nanoTime()/1000;
        if (t != null)
        t.t = (t2 - t1)/iteration;
        if (cr != CoderResult.UNDERFLOW) {
            System.out.println("ENC-----------------");
            int pos = cbf.position();
            System.out.printf("  cr=%s, cbf.pos=%d, cc[pos]=%x%n",
                              cr.toString(), pos, cc[pos]&0xffff);
            throw new RuntimeException("Encoding err: " + csn);
        }
        byte[] bb = new byte[bbf.position()];
        bbf.flip(); bbf.get(bb);
        return bb;
    }

    static CoderResult encodeCR(char[] cc, Charset cs, boolean testDirect)
        throws Exception {
        ByteBuffer bbf;
        CharBuffer cbf;
        CharsetEncoder enc = cs.newEncoder();
        if (testDirect) {
            bbf = ByteBuffer.allocateDirect(cc.length * 4);
            cbf = ByteBuffer.allocateDirect(cc.length * 2).asCharBuffer();
            cbf.put(cc).flip();
        } else {
            bbf = ByteBuffer.allocate(cc.length * 4);
            cbf = CharBuffer.wrap(cc);
        }
        return enc.encode(cbf, bbf, true);
    }

    static char[] getEUC_TWChars(boolean skipNR) {
        //CharsetEncoder encOLD = Charset.forName("EUC_TW_OLD").newEncoder();
        CharsetEncoder encOLD = new EUC_TW_OLD().newEncoder();
        CharsetEncoder enc = Charset.forName("EUC_TW").newEncoder();
        char[] cc = new char[0x20000];
        char[] c2 = new char[2];
        int pos = 0;
        int i = 0;
        //bmp
        for (i = 0; i < 0x10000; i++) {
            //SKIP these 3 NR codepoints if compared to EUC_TW
            if (skipNR && (i == 0x4ea0 || i == 0x51ab || i == 0x52f9))
                continue;
            if (encOLD.canEncode((char)i) != enc.canEncode((char)i)) {
                System.out.printf("  Err i=%x:  old=%b new=%b%n", i,
                                  encOLD.canEncode((char)i),
                                  enc.canEncode((char)i));
                throw new RuntimeException("canEncode() err!");
            }

            if (enc.canEncode((char)i)) {
                cc[pos++] = (char)i;
            }
        }

        //supp
        CharBuffer cb = CharBuffer.wrap(new char[2]);
        for (i = 0x20000; i < 0x30000; i++) {
            Character.toChars(i, c2, 0);
            cb.clear();cb.put(c2[0]);cb.put(c2[1]);cb.flip();

            if (encOLD.canEncode(cb) != enc.canEncode(cb)) {
                throw new RuntimeException("canEncode() err!");
            }

            if (enc.canEncode(cb)) {
                //System.out.printf("cp=%x,  (%x, %x) %n", i, c2[0] & 0xffff, c2[1] & 0xffff);
                cc[pos++] = c2[0];
                cc[pos++] = c2[1];
            }
        }

        return Arrays.copyOf(cc, pos);
    }

    static void checkRoundtrip(Charset cs) throws Exception {
        char[] cc = getEUC_TWChars(false);
        System.out.printf("Check roundtrip <%s>...", cs.name());
        byte[] bb = encode(cc, cs, false, null);
        char[] ccO = decode(bb, cs, false, null);

        if (!Arrays.equals(cc, ccO)) {
            System.out.printf("    non-direct failed");
        }
        bb = encode(cc, cs, true, null);
        ccO = decode(bb, cs, true, null);
        if (!Arrays.equals(cc, ccO)) {
            System.out.printf("    (direct) failed");
        }
        System.out.println();
    }

    static void checkInit(String csn) throws Exception {
        System.out.printf("Check init <%s>...%n", csn);
        Charset.forName("Big5");    // load in the ExtendedCharsets
        long t1 = System.nanoTime()/1000;
        Charset cs = Charset.forName(csn);
        long t2 = System.nanoTime()/1000;
        System.out.printf("    charset     :%d%n", t2 - t1);
        t1 = System.nanoTime()/1000;
            cs.newDecoder();
        t2 = System.nanoTime()/1000;
        System.out.printf("    new Decoder :%d%n", t2 - t1);

        t1 = System.nanoTime()/1000;
            cs.newEncoder();
        t2 = System.nanoTime()/1000;
        System.out.printf("    new Encoder :%d%n", t2 - t1);
    }

    static void compare(Charset cs1, Charset cs2) throws Exception {
        char[] cc = getEUC_TWChars(true);

        String csn1 = cs1.name();
        String csn2 = cs2.name();
        System.out.printf("Diff     <%s> <%s>...%n", csn1, csn2);

        Time t1 = new Time();
        Time t2 = new Time();

        byte[] bb1 = encode(cc, cs1, false, t1);
        byte[] bb2 = encode(cc, cs2, false, t2);

        System.out.printf("    Encoding TimeRatio %s/%s: %d,%d :%f%n",
                          csn2, csn1,
                          t2.t, t1.t,
                          (double)(t2.t)/(t1.t));
        if (!Arrays.equals(bb1, bb2)) {
            System.out.printf("        encoding failed%n");
        }

        char[] cc2 = decode(bb1, cs2, false, t2);
        char[] cc1 = decode(bb1, cs1, false, t1);
        System.out.printf("    Decoding TimeRatio %s/%s: %d,%d :%f%n",
                          csn2, csn1,
                          t2.t, t1.t,
                          (double)(t2.t)/(t1.t));
        if (!Arrays.equals(cc1, cc2)) {
            System.out.printf("        decoding failed%n");
        }

        bb1 = encode(cc, cs1, true, t1);
        bb2 = encode(cc, cs2, true, t2);

        System.out.printf("    Encoding(dir) TimeRatio %s/%s: %d,%d :%f%n",
                          csn2, csn1,
                          t2.t, t1.t,
                          (double)(t2.t)/(t1.t));

        if (!Arrays.equals(bb1, bb2))
            System.out.printf("        encoding (direct) failed%n");

        cc1 = decode(bb1, cs1, true, t1);
        cc2 = decode(bb1, cs2, true, t2);
        System.out.printf("    Decoding(dir) TimeRatio %s/%s: %d,%d :%f%n",
                          csn2, csn1,
                          t2.t, t1.t,
                          (double)(t2.t)/(t1.t));
        if (!Arrays.equals(cc1, cc2)) {
            System.out.printf("        decoding (direct) failed%n");
        }
    }

    // The first byte is the length of malformed bytes
    static byte[][] malformed = {
        //{5, (byte)0xF8, (byte)0x80, (byte)0x80, (byte)0x9F, (byte)0x80, (byte)0xC0 },
    };

    static void checkMalformed(Charset cs) throws Exception {
        boolean failed = false;
        String csn = cs.name();
        System.out.printf("Check malformed <%s>...%n", csn);
        for (boolean direct: new boolean[] {false, true}) {
            for (byte[] bins : malformed) {
                int mlen = bins[0];
                byte[] bin = Arrays.copyOfRange(bins, 1, bins.length);
                CoderResult cr = decodeCR(bin, cs, direct);
                String ashex = "";
                for (int i = 0; i < bin.length; i++) {
                    if (i > 0) ashex += " ";
                        ashex += Integer.toBinaryString((int)bin[i] & 0xff);
                }
                if (!cr.isMalformed()) {
                    System.out.printf("        FAIL(direct=%b): [%s] not malformed.\n", direct, ashex);
                    failed = true;
                } else if (cr.length() != mlen) {
                    System.out.printf("        FAIL(direct=%b): [%s] malformed[len=%d].\n", direct, ashex, cr.length());
                    failed = true;
                }
            }
        }
        if (failed)
            throw new RuntimeException("Check malformed failed " + csn);
    }

    static boolean check(CharsetDecoder dec, byte[] bytes, boolean direct, int[] flow) {
        int inPos = flow[0];
        int inLen = flow[1];
        int outPos = flow[2];
        int outLen = flow[3];
        int expedInPos = flow[4];
        int expedOutPos = flow[5];
        CoderResult expedCR = (flow[6]==0)?CoderResult.UNDERFLOW
                                          :CoderResult.OVERFLOW;
        ByteBuffer bbf;
        CharBuffer cbf;
        if (direct) {
            bbf = ByteBuffer.allocateDirect(inPos + bytes.length);
            cbf = ByteBuffer.allocateDirect((outPos + outLen)*2).asCharBuffer();
        } else {
            bbf = ByteBuffer.allocate(inPos + bytes.length);
            cbf = CharBuffer.allocate(outPos + outLen);
        }
        bbf.position(inPos);
        bbf.put(bytes).flip().position(inPos).limit(inPos + inLen);
        cbf.position(outPos);
        dec.reset();
        CoderResult cr = dec.decode(bbf, cbf, false);
        if (cr != expedCR ||
            bbf.position() != expedInPos ||
            cbf.position() != expedOutPos) {
            System.out.printf("Expected(direct=%5b): [", direct);
            for (int i:flow) System.out.print(" " + i);
            System.out.println("]  CR=" + cr +
                               ", inPos=" + bbf.position() +
                               ", outPos=" + cbf.position());
            return false;
        }
        return true;
    }

    static void checkUnderOverflow(Charset cs) throws Exception {
        String csn = cs.name();
        System.out.printf("Check under/overflow <%s>...%n", csn);
        CharsetDecoder dec = cs.newDecoder();
        boolean failed = false;
        //7f, a1a1, 8ea2a1a1, 8ea3a1a1, 8ea7a1a1
        //0   1 2   3         7         11
        byte[] bytes = new String("\u007f\u3000\u4e42\u4e28\ud840\udc55").getBytes("EUC_TW");
        int    inlen = bytes.length;

        int MAXOFF = 20;
        for (int inoff = 0; inoff < MAXOFF; inoff++) {
            for (int outoff = 0; outoff < MAXOFF; outoff++) {
        int[][] Flows = {
            //inpos, inLen, outPos,  outLen, inPosEP,    outposEP,   under(0)/over(1)
            //overflow
            {inoff,  inlen, outoff,  1,      inoff + 1,  outoff + 1, 1},
            {inoff,  inlen, outoff,  2,      inoff + 3,  outoff + 2, 1},
            {inoff,  inlen, outoff,  3,      inoff + 7,  outoff + 3, 1},
            {inoff,  inlen, outoff,  4,      inoff + 11, outoff + 4, 1},
            {inoff,  inlen, outoff,  5,      inoff + 11, outoff + 4, 1},
            {inoff,  inlen, outoff,  6,      inoff + 15, outoff + 6, 0},
            //underflow
            {inoff,  1,     outoff,  6,      inoff + 1,  outoff + 1, 0},
            {inoff,  2,     outoff,  6,      inoff + 1,  outoff + 1, 0},
            {inoff,  3,     outoff,  6,      inoff + 3,  outoff + 2, 0},
            {inoff,  4,     outoff,  6,      inoff + 3,  outoff + 2, 0},
            {inoff,  5,     outoff,  6,      inoff + 3,  outoff + 2, 0},
            {inoff,  8,     outoff,  6,      inoff + 7,  outoff + 3, 0},
            {inoff,  9,     outoff,  6,      inoff + 7,  outoff + 3, 0},
            {inoff, 10,     outoff,  6,      inoff + 7,  outoff + 3, 0},
            {inoff, 11,     outoff,  6,      inoff +11,  outoff + 4, 0},
            {inoff, 12,     outoff,  6,      inoff +11,  outoff + 4, 0},
            {inoff, 15,     outoff,  6,      inoff +15,  outoff + 6, 0},
            // 2-byte under/overflow
            {inoff,  2,     outoff,  1,      inoff + 1,  outoff + 1, 0},
            {inoff,  3,     outoff,  1,      inoff + 1,  outoff + 1, 1},
            {inoff,  3,     outoff,  2,      inoff + 3,  outoff + 2, 0},
            // 4-byte  under/overflow
            {inoff,  4,     outoff,  2,      inoff + 3,  outoff + 2, 0},
            {inoff,  5,     outoff,  2,      inoff + 3,  outoff + 2, 0},
            {inoff,  6,     outoff,  2,      inoff + 3,  outoff + 2, 0},
            {inoff,  7,     outoff,  2,      inoff + 3,  outoff + 2, 1},
            {inoff,  7,     outoff,  3,      inoff + 7,  outoff + 3, 0},
            // 4-byte  under/overflow
            {inoff,  8,     outoff,  3,      inoff + 7,  outoff + 3, 0},
            {inoff,  9,     outoff,  3,      inoff + 7,  outoff + 3, 0},
            {inoff, 10,     outoff,  3,      inoff + 7,  outoff + 3, 0},
            {inoff, 11,     outoff,  3,      inoff + 7,  outoff + 3, 1},
            {inoff, 11,     outoff,  4,      inoff +11,  outoff + 4, 0},
            // 4-byte/supp  under/overflow
            {inoff, 11,     outoff,  4,      inoff +11,  outoff + 4, 0},
            {inoff, 12,     outoff,  4,      inoff +11,  outoff + 4, 0},
            {inoff, 13,     outoff,  4,      inoff +11,  outoff + 4, 0},
            {inoff, 14,     outoff,  4,      inoff +11,  outoff + 4, 0},
            {inoff, 15,     outoff,  4,      inoff +11,  outoff + 4, 1},
            {inoff, 15,     outoff,  5,      inoff +11,  outoff + 4, 1},
            {inoff, 15,     outoff,  6,      inoff +15,  outoff + 6, 0},
        };
        for (boolean direct: new boolean[] {false, true}) {
            for (int[] flow: Flows) {
                if (!check(dec, bytes, direct, flow))
                    failed = true;
            }
        }}}
        if (failed)
            throw new RuntimeException("Check under/overflow failed " + csn);
    }

    public static void main(String[] args) throws Exception {
        // be the first one
        //checkInit("EUC_TW_OLD");
        checkInit("EUC_TW");
        Charset euctw = Charset.forName("EUC_TW");
        checkRoundtrip(euctw);
        compare(euctw, new EUC_TW_OLD());
        checkMalformed(euctw);
        checkUnderOverflow(euctw);
    }
}
