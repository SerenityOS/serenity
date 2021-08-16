/*
 * Copyright (c) 1999, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4085160 4139951 5005831
 * @summary Test that required character encodings are supported
 */

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;


public class Encodings {


    static boolean equals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i]) return false;
        return true;
    }


    static void go(String enc, String str, final byte[] bytes, boolean bidir)
        throws Exception
    {
        final Charset charset = Charset.forName(enc);

        /* String(byte[] bs, String enc) */
        if (!(new String(bytes, enc).equals(str)))
            throw new Exception(enc + ": String constructor failed");

        /* String(byte[] bs, Charset charset) */
        if (!(new String(bytes, charset).equals(str)))
            throw new Exception(charset + ": String constructor failed");

        /* String(byte[] bs, int off, int len, Charset charset) */
        String start = str.substring(0, 2);
        String end = str.substring(2);
        if (enc.equals("UTF-16BE") || enc.equals("UTF-16LE")) {
            if (!(new String(bytes, 0, 4, charset).equals(start)))
                throw new Exception(charset + ": String constructor failed");
            if (!(new String(bytes, 4, bytes.length - 4, charset).equals(end)))
                throw new Exception(charset + ": String constructor failed");
        } else if (enc.equals("UTF-16")) {
            if (!(new String(bytes, 0, 6, charset).equals(start)))
                throw new Exception(charset + ": String constructor failed");
        } else {
            if (!(new String(bytes, 0, 2, charset).equals(start)))
                throw new Exception(charset + ": String constructor failed");
            if (!(new String(bytes, 2, bytes.length - 2, charset).equals(end)))
                throw new Exception(charset + ": String constructor failed");
        }

        /* InputStreamReader */
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        InputStreamReader r = new InputStreamReader(bi, enc);
        String inEnc = r.getEncoding();
        int n = str.length();
        char[] cs = new char[n];
        for (int i = 0; i < n;) {
            int m;
            if ((m = r.read(cs, i, n - i)) < 0)
                throw new Exception(enc + ": EOF on InputStreamReader");
            i += m;
        }
        if (!(new String(cs).equals(str)))
            throw new Exception(enc + ": InputStreamReader failed");

        if (!bidir) {
            System.err.println(enc + " --> " + inEnc);
            return;
        }

        /* String.getBytes(String enc) */
        byte[] bs = str.getBytes(enc);
        if (!equals(bs, bytes))
            throw new Exception(enc + ": String.getBytes failed");

        /* String.getBytes(Charset charset) */
        bs = str.getBytes(charset);
        if (!equals(bs, bytes))
            throw new Exception(charset + ": String.getBytes failed");

        // Calls to String.getBytes(Charset) shouldn't automatically
        // use the cached thread-local encoder.
        if (charset.name().equals("UTF-16BE")) {
            String s = new String(bytes, charset);
            // Replace the thread-local encoder with this one.
            byte[] bb = s.getBytes(Charset.forName("UTF-16LE"));
            if (bytes.length != bb.length) {
                // Incidental test.
                throw new RuntimeException("unequal length: "
                                           + bytes.length + " != "
                                           + bb.length);
            } else {
                boolean diff = false;
                // Expect different byte[] between UTF-16LE and UTF-16BE
                // even though encoder was previously cached by last call
                // to getBytes().
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] != bb[i])
                        diff = true;
                }
                if (!diff)
                    throw new RuntimeException("byte arrays equal");
            }
        }

        /* OutputStreamWriter */
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        OutputStreamWriter w = new OutputStreamWriter(bo, enc);
        String outEnc = w.getEncoding();
        w.write(str);
        w.close();
        bs = bo.toByteArray();
        if (!equals(bs, bytes))
            throw new Exception(enc + ": OutputStreamWriter failed");

        System.err.println(enc + " --> " + inEnc + " / " + outEnc);
    }


    static void go(String enc, String str, byte[] bytes) throws Exception {
        go(enc, str, bytes, true);
    }


    public static void main(String[] args) throws Exception {

        go("US-ASCII", "abc", new byte[] { 'a', 'b', 'c' });
        go("us-ascii", "abc", new byte[] { 'a', 'b', 'c' });
        go("ISO646-US", "abc", new byte[] { 'a', 'b', 'c' });
        go("ISO-8859-1", "ab\u00c7", new byte[] { 'a', 'b', (byte)'\u00c7' });
        go("UTF-8", "ab\u1e09",
           new byte[] { 'a', 'b',
                        (byte)(0xe0 | (0x0f & (0x1e09 >> 12))),
                        (byte)(0x80 | (0x3f & (0x1e09 >> 6))),
                        (byte)(0x80 | (0x3f & 0x1e09)) });
        go("UTF-16BE", "ab\u1e09",
           new byte[] { 0, 'a', 0, 'b', 0x1e, 0x09 });
        go("UTF-16LE", "ab\u1e09",
           new byte[] { 'a', 0, 'b', 0, 0x09, 0x1e });

        /* UTF-16 accepts both byte orders on input but always uses big-endian
         * on output, so test all three cases
         */
        go("UTF-16", "ab\u1e09",
           new byte[] { (byte)0xfe, (byte)0xff, 0, 'a', 0, 'b', 0x1e, 0x09 });
        go("UTF-16", "ab\u1e09",
           new byte[] { (byte)0xff, (byte)0xfe, 'a', 0, 'b', 0, 0x09, 0x1e },
           false);
    }

}
