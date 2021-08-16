/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 6896617
 * @summary Optimize sun.nio.cs.ISO_8859_1$Encode.encodeArrayLoop() with SSE instructions on x86
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.base/sun.nio.cs
 *          java.management
 *
 * @ignore 8193479
 * @run main/othervm/timeout=1200 -Xbatch -Xmx256m compiler.codegen.Test6896617
 */

package compiler.codegen;

import jdk.test.lib.Utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Random;

public class Test6896617 {
    final static int SIZE = 256;

    public static void main(String[] args) {
        String csn = "ISO-8859-1";
        Charset cs = Charset.forName(csn);
        CharsetEncoder enc = cs.newEncoder();
        enc.onMalformedInput(CodingErrorAction.REPLACE)
           .onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharsetDecoder dec = cs.newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPLACE)
           .onUnmappableCharacter(CodingErrorAction.REPLACE);

        byte repl = (byte)'?';
        enc.replaceWith(new byte[] { repl });

        // Use internal API for tests.
        sun.nio.cs.ArrayEncoder arrenc = (sun.nio.cs.ArrayEncoder)enc;
        sun.nio.cs.ArrayDecoder arrdec = (sun.nio.cs.ArrayDecoder)dec;

        // Populate char[] with chars which can be encoded by ISO_8859_1 (<= 0xFF)
        Random rnd = Utils.getRandomInstance();
        int maxchar = 0xFF;
        char[] a = new char[SIZE];
        byte[] b = new byte[SIZE];
        char[] at = new char[SIZE];
        byte[] bt = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            char c = (char) rnd.nextInt(maxchar);
            if (!enc.canEncode(c)) {
                System.out.printf("Something wrong: can't encode c=%03x\n", (int)c);
                System.exit(97);
            }
            a[i] = c;
            b[i] = (byte)c;
            at[i] = (char)-1;
            bt[i] = (byte)-1;
        }
        if (arrenc.encode(a, 0, SIZE, bt) != SIZE || !Arrays.equals(b, bt)) {
            System.out.println("Something wrong: ArrayEncoder.encode failed");
            System.exit(97);
        }
        if (arrdec.decode(b, 0, SIZE, at) != SIZE || !Arrays.equals(a, at)) {
            System.out.println("Something wrong: ArrayDecoder.decode failed");
            System.exit(97);
        }
        for (int i = 0; i < SIZE; i++) {
            at[i] = (char)-1;
            bt[i] = (byte)-1;
        }

        ByteBuffer bb  = ByteBuffer.wrap(b);
        CharBuffer ba  = CharBuffer.wrap(a);
        ByteBuffer bbt = ByteBuffer.wrap(bt);
        CharBuffer bat = CharBuffer.wrap(at);
        if (!enc.encode(ba, bbt, true).isUnderflow() || !Arrays.equals(b, bt)) {
            System.out.println("Something wrong: Encoder.encode failed");
            System.exit(97);
        }
        if (!dec.decode(bb, bat, true).isUnderflow() || !Arrays.equals(a, at)) {
            System.out.println("Something wrong: Decoder.decode failed");
            System.exit(97);
        }
        for (int i = 0; i < SIZE; i++) {
            at[i] = (char)-1;
            bt[i] = (byte)-1;
        }

        // Warm up
        boolean failed = false;
        int result = 0;
        for (int i = 0; i < 10000; i++) {
            result += arrenc.encode(a, 0, SIZE, bt);
            result -= arrdec.decode(b, 0, SIZE, at);
        }
        for (int i = 0; i < 10000; i++) {
            result += arrenc.encode(a, 0, SIZE, bt);
            result -= arrdec.decode(b, 0, SIZE, at);
        }
        for (int i = 0; i < 10000; i++) {
            result += arrenc.encode(a, 0, SIZE, bt);
            result -= arrdec.decode(b, 0, SIZE, at);
        }
        if (result != 0 || !Arrays.equals(b, bt) || !Arrays.equals(a, at)) {
            failed = true;
            System.out.println("Failed: ArrayEncoder.encode char[" + SIZE + "] and ArrayDecoder.decode byte[" + SIZE + "]");
        }
        for (int i = 0; i < SIZE; i++) {
            at[i] = (char)-1;
            bt[i] = (byte)-1;
        }

        boolean is_underflow = true;
        for (int i = 0; i < 10000; i++) {
            ba.clear(); bb.clear(); bat.clear(); bbt.clear();
            boolean enc_res = enc.encode(ba, bbt, true).isUnderflow();
            boolean dec_res = dec.decode(bb, bat, true).isUnderflow();
            is_underflow = is_underflow && enc_res && dec_res;
        }
        for (int i = 0; i < SIZE; i++) {
            at[i] = (char)-1;
            bt[i] = (byte)-1;
        }
        for (int i = 0; i < 10000; i++) {
            ba.clear(); bb.clear(); bat.clear(); bbt.clear();
            boolean enc_res = enc.encode(ba, bbt, true).isUnderflow();
            boolean dec_res = dec.decode(bb, bat, true).isUnderflow();
            is_underflow = is_underflow && enc_res && dec_res;
        }
        for (int i = 0; i < SIZE; i++) {
            at[i] = (char)-1;
            bt[i] = (byte)-1;
        }
        for (int i = 0; i < 10000; i++) {
            ba.clear(); bb.clear(); bat.clear(); bbt.clear();
            boolean enc_res = enc.encode(ba, bbt, true).isUnderflow();
            boolean dec_res = dec.decode(bb, bat, true).isUnderflow();
            is_underflow = is_underflow && enc_res && dec_res;
        }
        if (!is_underflow || !Arrays.equals(b, bt) || !Arrays.equals(a, at)) {
            failed = true;
            System.out.println("Failed: Encoder.encode char[" + SIZE + "] and Decoder.decode byte[" + SIZE + "]");
        }

        // Test encoder with different source and destination sizes
        System.out.println("Testing different source and destination sizes");
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 1; j <= SIZE; j++) {
                bt = new byte[j];
                // very source's SIZE
                result = arrenc.encode(a, 0, i, bt);
                int l = Math.min(i, j);
                if (result != l) {
                    failed = true;
                    System.out.println("Failed: encode char[" + i + "] to byte[" + j + "]: result = " + result + ", expected " + l);
                }
                for (int k = 0; k < l; k++) {
                    if (bt[k] != b[k]) {
                        failed = true;
                        System.out.println("Failed: encoded byte[" + k + "] (" + bt[k] + ") != " + b[k]);
                    }
                }
                // very source's offset
                int sz = SIZE - i + 1;
                result = arrenc.encode(a, i-1, sz, bt);
                l = Math.min(sz, j);
                if (result != l) {
                    failed = true;
                    System.out.println("Failed: encode char[" + sz + "] to byte[" + j + "]: result = " + result + ", expected " + l);
                }
                for (int k = 0; k < l; k++) {
                    if (bt[k] != b[i+k-1]) {
                        failed = true;
                        System.out.println("Failed: encoded byte[" + k + "] (" + bt[k] + ") != " + b[i+k-1]);
                    }
                }
            }
        }

        // Test encoder with char > 0xFF
        System.out.println("Testing big char");

        byte orig = (byte)'A';
        bt = new byte[SIZE];
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 0; j < i; j++) {
                a[j] += 0x100;
                // make sure to replace a different byte
                bt[j] = orig;
                result = arrenc.encode(a, 0, i, bt);
                if (result != i) {
                    failed = true;
                    System.out.println("Failed: encode char[" + i + "] to byte[" + i + "]: result = " + result + ", expected " + i);
                }
                if (bt[j] != repl) {
                    failed = true;
                    System.out.println("Failed: encoded replace byte[" + j + "] (" + bt[j] + ") != " + repl);
                }
                bt[j] = b[j]; // Restore to compare whole array
                for (int k = 0; k < i; k++) {
                    if (bt[k] != b[k]) {
                        failed = true;
                        System.out.println("Failed: encoded byte[" + k + "] (" + bt[k] + ") != " + b[k]);
                    }
                }
                a[j] -= 0x100; // Restore
            }
        }

        // Test sun.nio.cs.ISO_8859_1$Encode.encodeArrayLoop() performance.

        int itrs = Integer.getInteger("iterations", 1000000);
        int size = Integer.getInteger("size", 256);
        a  = new char[size];
        b  = new byte[size];
        bt = new byte[size];
        for (int i = 0; i < size; i++) {
            char c = (char) rnd.nextInt(maxchar);
            if (!enc.canEncode(c)) {
                System.out.printf("Something wrong: can't encode c=%03x\n", (int)c);
                System.exit(97);
            }
            a[i] = c;
            b[i]  = (byte)-1;
            bt[i] = (byte)c;
        }
        ba = CharBuffer.wrap(a);
        bb = ByteBuffer.wrap(b);
        boolean enc_res = enc.encode(ba, bb, true).isUnderflow();
        if (!enc_res || !Arrays.equals(b, bt)) {
            failed = true;
            System.out.println("Failed 1: Encoder.encode char[" + size + "]");
        }
        for (int i = 0; i < size; i++) {
            b[i] = (byte)-1;
        }

        // Make sure to recompile method if needed before performance run.
        for (int i = 0; i < 10000; i++) {
            ba.clear(); bb.clear();
            enc_res = enc_res && enc.encode(ba, bb, true).isUnderflow();
        }
        for (int i = 0; i < size; i++) {
            b[i] = (byte)-1;
        }
        for (int i = 0; i < 10000; i++) {
            ba.clear(); bb.clear();
            enc_res = enc_res && enc.encode(ba, bb, true).isUnderflow();
        }
        if (!enc_res || !Arrays.equals(b, bt)) {
            failed = true;
            System.out.println("Failed 2: Encoder.encode char[" + size + "]");
        }
        for (int i = 0; i < size; i++) {
            b[i] = (byte)-1;
        }

        System.out.println("Testing ISO_8859_1$Encode.encodeArrayLoop() performance");
        long start = System.currentTimeMillis();
        for (int i = 0; i < itrs; i++) {
            ba.clear(); bb.clear();
            enc_res = enc_res && enc.encode(ba, bb, true).isUnderflow();
        }
        long end = System.currentTimeMillis();
        if (!enc_res || !Arrays.equals(b, bt)) {
            failed = true;
            System.out.println("Failed 3: Encoder.encode char[" + size + "]");
        } else {
            System.out.println("size: " + size + " time: " + (end - start));
        }

        // Test sun.nio.cs.ISO_8859_1$Encode.encode() performance.

        // Make sure to recompile method if needed before performance run.
        result = 0;
        for (int i = 0; i < size; i++) {
            b[i] = (byte)-1;
        }
        for (int i = 0; i < 10000; i++) {
            result += arrenc.encode(a, 0, size, b);
        }
        for (int i = 0; i < size; i++) {
            b[i] = (byte)-1;
        }
        for (int i = 0; i < 10000; i++) {
            result += arrenc.encode(a, 0, size, b);
        }
        if (result != size*20000 || !Arrays.equals(b, bt)) {
            failed = true;
            System.out.println("Failed 1: ArrayEncoder.encode char[" + SIZE + "]");
        }
        for (int i = 0; i < size; i++) {
            b[i] = (byte)-1;
        }

        System.out.println("Testing ISO_8859_1$Encode.encode() performance");
        result = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < itrs; i++) {
            result += arrenc.encode(a, 0, size, b);
        }
        end = System.currentTimeMillis();
        if (!Arrays.equals(b, bt)) {
            failed = true;
            System.out.println("Failed 2: ArrayEncoder.encode char[" + size + "]");
        } else {
            System.out.println("size: " + size + " time: " + (end - start));
        }

        if (failed) {
          System.out.println("FAILED");
          System.exit(97);
        }
        System.out.println("PASSED");
    }
}
