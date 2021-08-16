/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.charset.StandardCharsets.*;

/**
 * Simple utility to convert from native encoding file to ascii or reverse
 * including \udddd Unicode notation.
 */
public class Native2Ascii {
    final Charset cs;
    final CharsetEncoder encoder;
    public Native2Ascii(Charset cs) {
        this.cs = cs;
        this.encoder = cs.newEncoder();
    }

    /**
     * ASCII to Native conversion
     */
    public void asciiToNative(Path infile, Path outfile) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(infile, US_ASCII);
             BufferedReader reader = new BufferedReader(new A2NFilter(in));
             BufferedWriter writer = Files.newBufferedWriter(outfile, cs)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line.toCharArray());
                writer.newLine();
            }
        }
    }

    /**
     * Native to ASCII conversion
     */
    public void nativeToAscii(Path infile, Path outfile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(infile, cs);
             BufferedWriter out = Files.newBufferedWriter(outfile, US_ASCII);
             BufferedWriter writer = new BufferedWriter(new N2AFilter(out))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line.toCharArray());
                writer.newLine();
            }
        }
    }

    // A copy of native2ascii N2AFilter
    class N2AFilter extends FilterWriter {
        public N2AFilter(Writer out) { super(out); }
        public void write(char b) throws IOException {
            char[] buf = new char[1];
            buf[0] = b;
            write(buf, 0, 1);
        }

        public void write(char[] buf, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                if ((buf[i] > '\u007f')) {
                    // write \udddd
                    out.write('\\');
                    out.write('u');
                    String hex = Integer.toHexString(buf[i]);
                    StringBuilder hex4 = new StringBuilder(hex);
                    hex4.reverse();
                    int length = 4 - hex4.length();
                    for (int j = 0; j < length; j++) {
                        hex4.append('0');
                    }
                    for (int j = 0; j < 4; j++) {
                        out.write(hex4.charAt(3 - j));
                    }
                } else
                    out.write(buf[i]);
            }
        }
    }

    // A copy of native2ascii A2NFilter
    class A2NFilter extends FilterReader {
        // maintain a trailing buffer to hold any incompleted
        // unicode escaped sequences
        private char[] trailChars = null;

        public A2NFilter(Reader in) {
            super(in);
        }

        public int read(char[] buf, int off, int len) throws IOException {
            int numChars = 0;        // how many characters have been read
            int retChars = 0;        // how many characters we'll return

            char[] cBuf = new char[len];
            int cOffset = 0;         // offset at which we'll start reading
            boolean eof = false;

            // copy trailing chars from previous invocation to input buffer
            if (trailChars != null) {
                for (int i = 0; i < trailChars.length; i++)
                    cBuf[i] = trailChars[i];
                numChars = trailChars.length;
                trailChars = null;
            }

            int n = in.read(cBuf, numChars, len - numChars);
            if (n < 0) {
                eof = true;
                if (numChars == 0)
                    return -1;              // EOF;
            } else {
                numChars += n;
            }

            for (int i = 0; i < numChars; ) {
                char c = cBuf[i++];

                if (c != '\\' || (eof && numChars <= 5)) {
                    // Not a backslash, so copy and continue
                    // Always pass non backslash chars straight thru
                    // for regular encoding. If backslash occurs in
                    // input stream at the final 5 chars then don't
                    // attempt to read-ahead and de-escape since these
                    // are literal occurrences of U+005C which need to
                    // be encoded verbatim in the target encoding.
                    buf[retChars++] = c;
                    continue;
                }

                int remaining = numChars - i;
                if (remaining < 5) {
                    // Might be the first character of a unicode escape, but we
                    // don't have enough characters to tell, so save it and finish
                    trailChars = new char[1 + remaining];
                    trailChars[0] = c;
                    for (int j = 0; j < remaining; j++)
                        trailChars[1 + j] = cBuf[i + j];
                    break;
                }
                // At this point we have at least five characters remaining

                c = cBuf[i++];
                if (c != 'u') {
                    // Not a unicode escape, so copy and continue
                    buf[retChars++] = '\\';
                    buf[retChars++] = c;
                    continue;
                }

                // The next four characters are the hex part of a unicode escape
                char rc = 0;
                boolean isUE = true;
                try {
                    rc = (char) Integer.parseInt(new String(cBuf, i, 4), 16);
                } catch (NumberFormatException x) {
                    isUE = false;
                }
                if (isUE && encoder.canEncode(rc)) {
                    // We'll be able to convert this
                    buf[retChars++] = rc;
                    i += 4; // Align beyond the current uXXXX sequence
                } else {
                    // We won't, so just retain the original sequence
                    buf[retChars++] = '\\';
                    buf[retChars++] = 'u';
                    continue;
                }

            }

            return retChars;
        }

        public int read() throws IOException {
            char[] buf = new char[1];

            if (read(buf, 0, 1) == -1)
                return -1;
            else
                return (int) buf[0];
        }
    }
}
