/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7040220 8054307
   @summary Test if StringCoding and NIO result have the same de/encoding result for UTF-8
 * @run main/othervm/timeout=2000 -Djava.security.manager=allow TestStringCodingUTF8
 * @key randomness
 */

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class TestStringCodingUTF8 {
    public static void main(String[] args) throws Throwable {
        test("UTF-8");
        test("CESU-8");
        // security manager on
        System.setSecurityManager(new PermissiveSecurityManger());
        test("UTF-8");
        test("CESU-8");
    }

    static void test(String csn) throws Throwable {
        Charset cs = Charset.forName(csn);
        char[] bmp = new char[0x10000];
        for (int i = 0; i < 0x10000; i++) {
            bmp[i] = (char)i;
        }
        test(cs, bmp, 0, bmp.length);

        char[] ascii = new char[0x80];
        for (int i = 0; i < 0x80; i++) {
            ascii[i] = (char)i;
        }
        test(cs, ascii, 0, ascii.length);

        char[] latin1 = new char[0x100];
        for (int i = 0; i < 0x100; i++) {
            latin1[i] = (char)i;
        }
        test(cs, latin1, 0, latin1.length);

        ArrayList<Integer> list = new ArrayList<>(0x20000);
        for (int i = 0; i < 0x20000; i++) {
            list.add(i, i);
        }
        Collections.shuffle(list);
        int j = 0;
        char[] bmpsupp = new char[0x30000];
        for (int i = 0; i < 0x20000; i++) {
            j += Character.toChars(list.get(i), bmpsupp, j);
        }
        assert (j == bmpsupp.length);
        test(cs, bmpsupp, 0, bmpsupp.length);

        // randomed "off" and "len" on shuffled data
        Random rnd = new Random();
        int maxlen = 1000;
        int itr = 5000;
        for (int i = 0; i < itr; i++) {
            int off = rnd.nextInt(bmpsupp.length - maxlen);
            int len = rnd.nextInt(maxlen);
            test(cs, bmpsupp, off, len);
        }

        // random length of bytes, test the edge corner case
        for (int i = 0; i < itr; i++) {
            byte[] ba = new byte[rnd.nextInt(maxlen)];
            rnd.nextBytes(ba);
            //new String(csn);
            if (!new String(ba, cs.name()).equals(
                 new String(decode(cs, ba, 0, ba.length))))
                throw new RuntimeException("new String(csn) failed");
            //new String(cs);
            if (!new String(ba, cs).equals(
                 new String(decode(cs, ba, 0, ba.length))))
                throw new RuntimeException("new String(cs) failed");
        }
        System.out.println("done!");
    }

    static void test(Charset cs, char[] ca, int off, int len) throws Throwable {
        String str = new String(ca, off, len);
        byte[] ba = encode(cs, ca, off, len);

        //getBytes(csn);
        byte[] baStr = str.getBytes(cs.name());
        if (!Arrays.equals(ba, baStr))
            throw new RuntimeException("getBytes(csn) failed");

        //getBytes(cs);
        baStr = str.getBytes(cs);
        if (!Arrays.equals(ba, baStr))
            throw new RuntimeException("getBytes(cs) failed");

        //new String(csn);
        if (!new String(ba, cs.name()).equals(new String(decode(cs, ba, 0, ba.length))))
            throw new RuntimeException("new String(csn) failed");

        //new String(cs);
        if (!new String(ba, cs).equals(new String(decode(cs, ba, 0, ba.length))))
            throw new RuntimeException("new String(cs) failed");
    }

    // copy/paste of the StringCoding.decode()
    static char[] decode(Charset cs, byte[] ba, int off, int len) {
        CharsetDecoder cd = cs.newDecoder();
        int en = (int)(len * cd.maxCharsPerByte());
        char[] ca = new char[en];
        if (len == 0)
            return ca;
        cd.onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE)
          .reset();

        ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
        CharBuffer cb = CharBuffer.wrap(ca);
        try {
            CoderResult cr = cd.decode(bb, cb, true);
            if (!cr.isUnderflow())
                cr.throwException();
            cr = cd.flush(cb);
            if (!cr.isUnderflow())
                cr.throwException();
        } catch (CharacterCodingException x) {
            throw new Error(x);
        }
        return Arrays.copyOf(ca, cb.position());
    }

    // copy/paste of the StringCoding.encode()
    static byte[] encode(Charset cs, char[] ca, int off, int len) {
        CharsetEncoder ce = cs.newEncoder();
        int en = (int)(len * ce.maxBytesPerChar());
        byte[] ba = new byte[en];
        if (len == 0)
            return ba;
        ce.onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE)
          .reset();
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca, off, len);
        try {
            CoderResult cr = ce.encode(cb, bb, true);
            if (!cr.isUnderflow())
                cr.throwException();
            cr = ce.flush(bb);
            if (!cr.isUnderflow())
                cr.throwException();
        } catch (CharacterCodingException x) {
            throw new Error(x);
        }
        return Arrays.copyOf(ba, bb.position());
    }

    static class PermissiveSecurityManger extends SecurityManager {
        @Override public void checkPermission(java.security.Permission p) {}
    }
}
