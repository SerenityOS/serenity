/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.nio.zipfs;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for zipfile name and comment decoding and encoding
 *
 * @author Xueming Shen
 */
class ZipCoder {

    static class UTF8 extends ZipCoder {
        UTF8() {
            super(UTF_8);
        }

        @Override
        byte[] getBytes(String s) {        // fast pass for ascii
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) > 0x7f) return super.getBytes(s);
            }
            return s.getBytes(ISO_8859_1);
        }

        @Override
        String toString(byte[] ba) {
            for (byte b : ba) {
                if (b < 0) return super.toString(ba);
            }
            return new String(ba, ISO_8859_1);
        }
    }

    private static final ZipCoder utf8 = new UTF8();

    public static ZipCoder get(String csn) {
        Charset cs = Charset.forName(csn);
        if (cs.name().equals("UTF-8")) {
            return utf8;
        }
        return new ZipCoder(cs);
    }

    String toString(byte[] ba) {
        CharsetDecoder cd = decoder().reset();
        int clen = (int)(ba.length * cd.maxCharsPerByte());
        char[] ca = new char[clen];
        if (clen == 0)
            return new String(ca);
        ByteBuffer bb = ByteBuffer.wrap(ba, 0, ba.length);
        CharBuffer cb = CharBuffer.wrap(ca);
        CoderResult cr = cd.decode(bb, cb, true);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        cr = cd.flush(cb);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        return new String(ca, 0, cb.position());
    }

    byte[] getBytes(String s) {
        CharsetEncoder ce = encoder().reset();
        char[] ca = s.toCharArray();
        int len = (int)(ca.length * ce.maxBytesPerChar());
        byte[] ba = new byte[len];
        if (len == 0)
            return ba;
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca);
        CoderResult cr = ce.encode(cb, bb, true);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        cr = ce.flush(bb);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        if (bb.position() == ba.length)  // defensive copy?
            return ba;
        else
            return Arrays.copyOf(ba, bb.position());
    }

    boolean isUTF8() {
        return cs == UTF_8;
    }

    private Charset cs;

    private ZipCoder(Charset cs) {
        this.cs = cs;
    }

    private final ThreadLocal<CharsetDecoder> decTL = new ThreadLocal<>();
    private final ThreadLocal<CharsetEncoder> encTL = new ThreadLocal<>();

    private CharsetDecoder decoder() {
        CharsetDecoder dec = decTL.get();
        if (dec == null) {
        dec = cs.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        decTL.set(dec);
        }
        return dec;
    }

    private CharsetEncoder encoder() {
        CharsetEncoder enc = encTL.get();
        if (enc == null) {
        enc = cs.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        encTL.set(enc);
        }
        return enc;
    }
}
