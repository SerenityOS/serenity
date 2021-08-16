/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.Path;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Unix specific Path <--> URI conversion
 */

class UnixUriUtils {
    private UnixUriUtils() { }

    /**
     * Converts URI to Path
     */
    static Path fromUri(UnixFileSystem fs, URI uri) {
        if (!uri.isAbsolute())
            throw new IllegalArgumentException("URI is not absolute");
        if (uri.isOpaque())
            throw new IllegalArgumentException("URI is not hierarchical");
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase("file"))
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        if (uri.getRawAuthority() != null)
            throw new IllegalArgumentException("URI has an authority component");
        if (uri.getRawFragment() != null)
            throw new IllegalArgumentException("URI has a fragment component");
        if (uri.getRawQuery() != null)
            throw new IllegalArgumentException("URI has a query component");

        // compatibility with java.io.File
        if (!uri.toString().startsWith("file:///"))
            return new File(uri).toPath();

        // transformation use raw path
        String p = uri.getRawPath();
        int len = p.length();
        if (len == 0)
            throw new IllegalArgumentException("URI path component is empty");

        // transform escaped octets and unescaped characters to bytes
        if (p.endsWith("/") && len > 1)
            len--;
        byte[] result = new byte[len];
        int rlen = 0;
        int pos = 0;
        while (pos < len) {
            char c = p.charAt(pos++);
            byte b;
            if (c == '%') {
                assert (pos+2) <= len;
                char c1 = p.charAt(pos++);
                char c2 = p.charAt(pos++);
                b = (byte)((decode(c1) << 4) | decode(c2));
                if (b == 0)
                    throw new IllegalArgumentException("Nul character not allowed");
            } else {
                if (c == 0 || c >= 0x80)
                    throw new IllegalArgumentException("Bad escape");
                b = (byte)c;
            }
            result[rlen++] = b;
        }
        if (rlen != result.length)
            result = Arrays.copyOf(result, rlen);

        return new UnixPath(fs, result);
    }

    /**
     * Converts Path to URI
     */
    static URI toUri(UnixPath up) {
        byte[] path = up.toAbsolutePath().asByteArray();
        StringBuilder sb = new StringBuilder("file:///");
        assert path[0] == '/';
        HexFormat hex = HexFormat.of().withUpperCase();
        for (int i=1; i<path.length; i++) {
            char c = (char)(path[i] & 0xff);
            if (match(c, L_PATH, H_PATH)) {
                sb.append(c);
            } else {
               sb.append('%');
               hex.toHexDigits(sb, (byte)c);
            }
        }

        // trailing slash if directory
        if (sb.charAt(sb.length()-1) != '/') {
            try {
                up.checkRead();
                int mode = UnixNativeDispatcher.stat(up);
                if ((mode & UnixConstants.S_IFMT) == UnixConstants.S_IFDIR)
                    sb.append('/');
            } catch (SecurityException ignore) { }
        }

        try {
            return new URI(sb.toString());
        } catch (URISyntaxException x) {
            throw new AssertionError(x);  // should not happen
        }
    }

    // The following is copied from java.net.URI

    // Compute the low-order mask for the characters in the given string
    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Compute the high-order mask for the characters in the given string
    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    private static long lowMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    private static long highMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Tell whether the given character is permitted by the given mask pair
    private static boolean match(char c, long lowMask, long highMask) {
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    // decode
    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        throw new AssertionError();
    }

    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long H_DIGIT = 0L;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    private static final long L_UPALPHA = 0L;
    private static final long H_UPALPHA = highMask('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    private static final long L_LOWALPHA = 0L;
    private static final long H_LOWALPHA = highMask('a', 'z');

    // alpha         = lowalpha | upalpha
    private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long H_MARK = highMask("-_.!~*'()");

    // unreserved    = alphanum | mark
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;

    // pchar         = unreserved | escaped |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    private static final long L_PCHAR
        = L_UNRESERVED | lowMask(":@&=+$,");
    private static final long H_PCHAR
        = H_UNRESERVED | highMask(":@&=+$,");

   // All valid path characters
   private static final long L_PATH = L_PCHAR | lowMask(";/");
   private static final long H_PATH = H_PCHAR | highMask(";/");
}
