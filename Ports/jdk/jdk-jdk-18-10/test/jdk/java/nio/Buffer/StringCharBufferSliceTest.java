/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4997655 5071718 7000913
 * @summary (bf) CharBuffer.slice() on wrapped CharSequence results in wrong position
 */

import java.nio.CharBuffer;
import java.nio.InvalidMarkException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StringCharBufferSliceTest {
    public static void main( String[] args) throws Exception {
        System.out.println(
            ">>> StringCharBufferSliceTest-main: testing the slice method...");

        final String in = "for testing";

        System.out.println(
            ">>> StringCharBufferSliceTest-main: testing with the position 0.");

        CharBuffer buff = CharBuffer.wrap(in);
        test(buff, buff.slice());
        test(buff, buff.slice(0, buff.remaining()));

        System.out.println(
            ">>> StringCharBufferSliceTest-main: testing with new position.");

        buff.position(2);
        test(buff, buff.slice());
        test(buff, buff.slice(2, buff.remaining()));

        System.out.println(
          ">>> StringCharBufferSliceTest-main: testing with non zero initial position.");

        buff = CharBuffer.wrap(in, 3, in.length());
        test(buff, buff.slice());
        test(buff, buff.slice(0, buff.remaining()));

        System.out.println(
            ">>> StringCharBufferSliceTest-main: testing slice result with get()");
        buff.position(4);
        buff.limit(7);
        BiConsumer<CharBuffer,CharBuffer> bitest = (b, s) -> {
            for (int i = 0; i < 3; i++) {
                if (s.get() != b.get()) {
                    throw new RuntimeException
                        ("Wrong characters in slice result.");
                }
            }
        };
        bitest.accept(buff, buff.slice());
        buff.position(4);
        bitest.accept(buff, buff.slice(4, 3));

        System.out.println(
            ">>> StringCharBufferSliceTest-main: testing slice result with get(int)");
        buff.position(4);
        buff.limit(7);
        bitest = (b, s) -> {
            for (int i = 0; i < 3; i++) {
                if (s.get(i) != b.get(4 + i)) {
                    throw new RuntimeException
                        ("Wrong characters in slice result.");
                }
            }
        };
        bitest.accept(buff, buff.slice());
        buff.position(4);
        bitest.accept(buff, buff.slice(4, 3));

        System.out.println(
          ">>> StringCharBufferSliceTest-main: testing slice with result of slice");
        buff.position(0);
        buff.limit(buff.capacity());
        Consumer<CharBuffer> test = (s) -> {
            for (int i=0; i<4; i++) {
                s.position(i);
                CharBuffer nextSlice = s.slice();
                if (nextSlice.position() != 0)
                    throw new RuntimeException
                        ("New buffer's position should be zero");
                if (!nextSlice.equals(s))
                    throw new RuntimeException("New buffer should be equal");
                s = nextSlice;
            }
        };
        test.accept(buff.slice());
        test.accept(buff.slice(0, buff.capacity()));

        System.out.println(
          ">>> StringCharBufferSliceTest-main: testing toString.");
        buff.position(4);
        buff.limit(7);
        test = (s) -> {
            if (!s.toString().equals("tes")) {
                throw new RuntimeException
                    ("bad toString() after slice(): " + s.toString());
            }
        };
        test.accept(buff.slice());
        test.accept(buff.slice(4, 3));

        System.out.println(
          ">>> StringCharBufferSliceTest-main: testing subSequence.");
        buff.position(4);
        buff.limit(8);
        test = (s) -> {
            CharSequence subSeq = s.subSequence(1, 3);
            if (subSeq.charAt(0) != 'e' || subSeq.charAt(1) != 's') {
                throw new RuntimeException
                    ("bad subSequence() after slice(): '" + subSeq + "'");
            }
        };
        test.accept(buff.slice());
        test.accept(buff.slice(4, 4));

        System.out.println(
          ">>> StringCharBufferSliceTest-main: testing duplicate.");
        buff.position(4);
        buff.limit(8);
        test = (s) -> {
            CharBuffer dupe = s.duplicate();
            if (dupe.charAt(0) != 't' || dupe.charAt(1) != 'e'
                || dupe.charAt(2) != 's' || dupe.charAt(3) != 't') {
                throw new RuntimeException
                    ("bad duplicate() after slice(): '" + dupe + "'");
            }
        };
        test.accept(buff.slice());
        test.accept(buff.slice(4, 4));

        System.out.println(">>> StringCharBufferSliceTest-main: done!");
    }

    public static void test(CharBuffer buff, CharBuffer slice) throws RuntimeException {
        boolean marked = false;

        try {
            slice.reset();

            marked = true;
        } catch (InvalidMarkException ime) {
            // expected
        }

        if (marked ||
            slice.position() != 0 ||
            buff.remaining() != slice.limit() ||
            buff.remaining() != slice.capacity()) {

            throw new RuntimeException(
                 "Calling the CharBuffer.slice method failed.");
        }
    }
}
