/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
   @bug 5101128
   @summary Check behavior of CharsetDecoder.decode when overflow occurs
   @author Martin Buchholz
 */

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class DecoderOverflow {
    static int failures = 0;

    public static void main(String[] args) throws Exception {
        for (String csn : Charset.availableCharsets().keySet()) {
            try {
                test(csn);
            } catch (Throwable t) {
                System.out.println(csn);
                t.printStackTrace();
                failures++;
            }
        }
        if (failures > 0)
            throw new Exception(failures + " charsets failed");
    }

    static void test(String encoding) throws Exception {
        String text = "Vote for Duke!";
        Charset cs = Charset.forName(encoding);
        if (! cs.canEncode() || ! cs.newEncoder().canEncode('.')) return;
        ByteBuffer in = ByteBuffer.wrap(text.getBytes(encoding));
        CharBuffer out = CharBuffer.allocate(text.length()/2);
        CoderResult result = cs.newDecoder().decode(in, out, true);
        if (out.hasRemaining() || ! result.isOverflow())
            throw new Exception
                ("out.hasRemaining()=" + out.hasRemaining() +
                 " result.isOverflow()=" + result.isOverflow() +
                 " in.capacity()=" + in.capacity() +
                 " encoding=" + encoding);
    }
}
