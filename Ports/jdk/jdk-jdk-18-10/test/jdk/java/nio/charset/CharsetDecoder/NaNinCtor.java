/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8210285
 * @summary NaN arguments to the constructor should be rejected
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class NaNinCtor {

    public static void main(String[] args) throws Throwable {
        Charset ascii = Charset.forName("US-ASCII");

        // sanity check
        new MyDecoder(ascii, 0.5f, 1.5f);

        // various combinations of invalid arguments
        test(() -> new MyDecoder(ascii, 0.0f, 1.0f));
        test(() -> new MyDecoder(ascii, 1.0f, 0.0f));
        test(() -> new MyDecoder(ascii, -1.0f, 1.0f));
        test(() -> new MyDecoder(ascii, 1.0f, -1.0f));
        test(() -> new MyDecoder(ascii, Float.NaN, 1.0f));
        test(() -> new MyDecoder(ascii, 1.0f, Float.NaN));
        test(() -> new MyDecoder(ascii, 1.5f, 0.5f));
    }

    static void test(Runnable r) {
        try {
            r.run();
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException expected) {
        }
    }

    static class MyDecoder extends CharsetDecoder {
        public MyDecoder(Charset cs, float avg, float max) {
            super(cs, avg, max);
        }
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            return null;
        }
    }
}
