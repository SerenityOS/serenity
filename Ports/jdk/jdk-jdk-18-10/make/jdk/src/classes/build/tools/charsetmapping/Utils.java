/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.charsetmapping;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Formatter;

public class Utils {

    public final static char UNMAPPABLE_DECODING = '\uFFFD';
    public final static int  UNMAPPABLE_ENCODING = 0xFFFD;

    public static class Entry {
        public int bs;   //byte sequence reps
        public int cp;   //Unicode codepoint
        public int cp2;  //CC of composite

        public Entry () {}
        public Entry (int bytes, int cp, int cp2) {
            this.bs = bytes;
            this.cp = cp;
            this.cp2 = cp2;
        }
    }

    public static class Parser {
        static final Pattern basic = Pattern.compile("(?:0x)?(\\p{XDigit}++)\\s++(?:0x)?(\\p{XDigit}++)?\\s*+.*");
        static final int gBS = 1;
        static final int gCP = 2;
        static final int gCP2 = 3;

        BufferedReader reader;
        boolean closed;
        Matcher matcher;
        int gbs, gcp, gcp2;

        public Parser (InputStream in, Pattern p, int gbs, int gcp, int gcp2)
            throws IOException
        {
            this.reader = new BufferedReader(new InputStreamReader(in));
            this.closed = false;
            this.matcher = p.matcher("");
            this.gbs = gbs;
            this.gcp = gcp;
            this.gcp2 = gcp2;
        }

        public Parser (InputStream in, Pattern p) throws IOException {
            this(in, p, gBS, gCP, gCP2);
        }

        public Parser (InputStream in) throws IOException {
            this(in, basic, gBS, gCP, gCP2);
        }

        protected boolean isDirective(String line) {
            return line.startsWith("#");
        }

        protected Entry parse(Matcher matcher, Entry mapping) {
            mapping.bs = Integer.parseInt(matcher.group(gbs), 16);
            mapping.cp = Integer.parseInt(matcher.group(gcp), 16);
            if (gcp2 <= matcher.groupCount() &&
                matcher.group(gcp2) != null)
                mapping.cp2 = Integer.parseInt(matcher.group(gcp2), 16);
            else
                mapping.cp2 = 0;
            return mapping;
        }

        public Entry next() throws Exception {
            return next(new Entry());
        }

        // returns null and closes the input stream if the eof has beenreached.
        public Entry next(Entry mapping) throws Exception {
            if (closed)
                return null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isDirective(line))
                    continue;
                matcher.reset(line);
                if (!matcher.lookingAt()) {
                    //System.out.println("Missed: " + line);
                    continue;
                }
                return parse(matcher, mapping);
            }
            reader.close();
            closed = true;
            return null;
        }
    }

    public static class Output {
        private Formatter out;

        public Output(Formatter out) {
            this.out = out;
        }

        public void close() {
            out.close();
        }

        private void toChar(String fmt, char c) {
            switch (c) {
            case '\b':
                out.format("\\b"); break;
            case '\t':
                out.format("\\t"); break;
            case '\n':
                out.format("\\n"); break;
            case '\f':
                out.format("\\f"); break;
            case '\r':
                out.format("\\r"); break;
            case '\"':
                out.format("\\\""); break;
            case '\'':
                out.format("\\'"); break;
            case '\\':
                out.format("\\\\"); break;
            default:
                out.format(fmt, c & 0xffff);
            }
        }

        public void format(String fmt, Object ... args) {
            out.format(fmt, args);
        }

        public void format(char[] cc, int off, int end, String closure) {
            while (off < end) {
                out.format("        \"");
                for (int j = 0; j < 8; j++) {
                    if (off == end)
                        break;
                    toChar("\\u%04X", cc[off++]);
                }
                if (off == end)
                    out.format("\" %s%n", closure);
                else
                    out.format("\" + %n");
            }
        }

        public void format(char[] cc, String closure) {
            format(cc, 0, cc.length, closure);
        }

        public void format(char[] db, int b1, int b2Min, int b2Max,
                           String closure)
        {
            char[] cc = new char[b2Max - b2Min + 1];
            int off = 0;
            for (int b2 = b2Min; b2 <= b2Max; b2++) {
                cc[off++] = db[(b1 << 8) | b2];
            }
            format(cc, 0, cc.length, closure);
        }

        public void format(char[] date) {
            int off = 0;
            int end = date.length;
            while (off < end) {
                out.format("        ");
                for (int j = 0; j < 8 && off < end; j++) {
                    toChar("'\\u%04X',", date[off++]);
                }
                out.format("%n");
            }
        }
    }

    public static String getCopyright(File f) throws IOException {
        Scanner s = new Scanner(f, "ISO-8859-1");
        StringBuilder sb = new StringBuilder();
        while (s.hasNextLine()) {
            String ln = s.nextLine();
            sb.append(ln + "\n");
            // assume we have the copyright as the first comment
            if (ln.matches("^\\s\\*\\/$"))
                break;
        }
        s.close();
        return sb.toString();
    }
}
