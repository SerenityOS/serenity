/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6840246 6559590
 * @summary test String.split()
 * @key randomness
 */
import java.util.Arrays;
import java.util.Random;
import java.util.regex.*;

public class Split {

    public static void main(String[] args) throws Exception {
        String source = "0123456789";

        for (int limit=-2; limit<3; limit++) {
            for (int x=0; x<10; x++) {
                String[] result = source.split(Integer.toString(x), limit);
                int expectedLength = limit < 1 ? 2 : limit;

                if ((limit == 0) && (x == 9)) {
                    // expected dropping of ""
                    if (result.length != 1)
                        throw new RuntimeException("String.split failure 1");
                    if (!result[0].equals("012345678")) {
                        throw new RuntimeException("String.split failure 2");
                    }
                } else {
                    if (result.length != expectedLength) {
                        throw new RuntimeException("String.split failure 3");
                    }
                    if (!result[0].equals(source.substring(0,x))) {
                        if (limit != 1) {
                            throw new RuntimeException(
                                "String.split failure 4");
                        } else {
                            if (!result[0].equals(source.substring(0,10))) {
                            throw new RuntimeException(
                                "String.split failure 10");
                            }
                        }
                    }
                    if (expectedLength > 1) { // Check segment 2
                       if (!result[1].equals(source.substring(x+1,10)))
                          throw new RuntimeException("String.split failure 5");
                    }
                }
            }
        }
        // Check the case for no match found
        for (int limit=-2; limit<3; limit++) {
            String[] result = source.split("e", limit);
            if (result.length != 1)
                throw new RuntimeException("String.split failure 6");
            if (!result[0].equals(source))
                throw new RuntimeException("String.split failure 7");
        }
        // Check the case for limit == 0, source = "";
        // split() now returns 0-length for empty source "" see #6559590
        source = "";
        String[] result = source.split("e", 0);
        if (result.length != 1)
            throw new RuntimeException("String.split failure 8");
        if (!result[0].equals(source))
            throw new RuntimeException("String.split failure 9");

        // check fastpath of String.split()
        source = "0123456789abcdefgABCDEFG";
        Random r = new Random();

        for (boolean doEscape: new boolean[] {false, true}) {
            for (int cp = 0; cp < 0x11000; cp++) {
                Pattern p = null;
                String regex = new String(Character.toChars(cp));
                if (doEscape)
                    regex = "\\" + regex;
                try {
                    p = Pattern.compile(regex);
                } catch (PatternSyntaxException pse) {
                    // illegal syntax
                    try {
                        "abc".split(regex);
                    } catch (PatternSyntaxException pse0) {
                        continue;
                    }
                    throw new RuntimeException("String.split failure 11");
                }
                int off = r.nextInt(source.length());
                String[] srcStrs = new String[] {
                    "",
                    source,
                    regex + source,
                    source + regex,
                    source.substring(0, 3)
                        + regex + source.substring(3, 9)
                        + regex + source.substring(9, 15)
                        + regex + source.substring(15),
                    source.substring(0, off) + regex + source.substring(off)
                };
                for (String src: srcStrs) {
                    for (int limit=-2; limit<3; limit++) {
                        if (!Arrays.equals(src.split(regex, limit),
                                           p.split(src, limit)))
                            throw new RuntimeException("String.split failure 12");
                    }
                }
            }
        }
    }
}
