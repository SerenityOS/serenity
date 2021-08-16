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
   @bug 5066863 5066867 5066874 5066879 5066884 5066887 5065777 6730652
   @summary canEncode() false iff encode() throws CharacterCodingException
   @run main/timeout=1200 FindCanEncodeBugs
   @author Martin Buchholz
 */

import java.util.*;
import java.nio.charset.*;
import java.nio.*;

public class FindCanEncodeBugs {
    static boolean encodable1(CharsetEncoder enc, char c) {
        enc.reset();
        return enc.canEncode(c);
    }

    static boolean encodable2(CharsetEncoder enc, char c) {
        enc.reset();
        try { enc.encode(CharBuffer.wrap(new char[]{c})); return true; }
        catch (CharacterCodingException e) { return false; }
    }

    public static void main(String[] args) throws Exception {
        int failures = 0;

        for (Map.Entry<String,Charset> e
                 : Charset.availableCharsets().entrySet()) {
            String csn = e.getKey();
            Charset cs = e.getValue();

            if (! cs.canEncode() || csn.matches("x-COMPOUND_TEXT"))
                continue;

            //System.out.println(csn);

            CharsetEncoder enc = cs.newEncoder();

            for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
                boolean encodable1 = encodable1(enc, (char)i);
                boolean encodable2 = encodable2(enc, (char)i);
                if (encodable1 != encodable2) {
                    int start = i;
                    int end = i;
                    for (int j = i;
                         j <= '\uffff' &&
                             encodable1(enc, (char)j) == encodable1 &&
                             encodable2(enc, (char)j) == encodable2;
                         j++)
                        end = j;
                    System.out.printf("charset=%-18s canEncode=%-5b ",
                                      csn, encodable1);
                    if (start == end)
                        System.out.printf("\'\\u%04x\'%n", start);
                    else
                        System.out.printf("\'\\u%04x\' - \'\\u%04x\'%n",
                                          start, end);
                    i = end;
                    failures++;
                }
            }
        }

        if (failures > 0)
            throw new Exception(failures + " failures");
    }
}
