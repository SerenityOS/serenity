/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4257115
 * @summary Test URL encoder and decoder on a string that contains
 * characters within and beyond the 8859-1 range.
 *
 */

import java.io.*;
import java.net.*;

public class URLEncodeDecode {

    static char chars[] = {'H', 'e', 'l', 'l', 'o',
                           ' ', '+', '%',
                           '-', '_', '.',       '!', '~', '*', '\'', '(',
                           ')',
                           '@',
                           '\u00ae', '\u0101', '\u10a0'};

    static String str = new String(chars);

    static String correctEncodedUTF8 =
        "Hello+%2B%25-_.%21%7E*%27%28%29%40%C2%AE%C4%81%E1%82%A0";

    public static void main(String[] args) throws Exception {

        System.out.println("Constructed the string: " + str);
        System.out.println("The Unicode bytes are: " +
                           getHexBytes(str));
        System.out.println("");
        test("UTF-8", correctEncodedUTF8);
    }

    private static void test(String enc, String correctEncoded)
        throws Exception{

        String encoded = null;
        String outStr = null;

        if (enc == null) {
            encoded = URLEncoder.encode(str);
            outStr = "default";
        }
        else {
            encoded = URLEncoder.encode(str, enc);
            outStr = enc;
        }

        System.out.println("URLEncode it ("
                           + outStr + ") : " + encoded);
        System.out.println("The Unicode bytes are: " +
                           getHexBytes(encoded));

        if (encoded.equals(correctEncoded))
            System.out.println("The encoding is correct!");
        else {
            throw new Exception("The encoding is incorrect!" +
                                " It should be " + correctEncoded);
        }
        System.out.println("");

        String decoded = null;

        if (enc == null)
            decoded = URLDecoder.decode(encoded);
        else
            decoded = URLDecoder.decode(encoded, enc);

        System.out.println("URLDecode it ("
                           + outStr + ") : " + decoded);
        System.out.println("The Unicode bytes are: " +
                           getHexBytes(decoded));

        if (str.equals(decoded))
            System.out.println("The decoding is correct");
        else {
            throw new Exception("The decoded is not equal to the original");
        }

    }

    private static String getHexBytes(String s) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {

            int a = s.charAt(i);
            int b1 = (a >>8) & 0xff;
            int b2 = (byte)a;
            int b11 = (b1>>4) & 0x0f;
            int b12 = b1 & 0x0f;
            int b21 = (b2 >>4) & 0x0f;
            int b22 = b2 & 0x0f;

            sb.append(Integer.toHexString(b11));
            sb.append(Integer.toHexString(b12));
            sb.append(Integer.toHexString(b21));
            sb.append(Integer.toHexString(b22));
            sb.append(' ');
        }
        return sb.toString();
    }

}
