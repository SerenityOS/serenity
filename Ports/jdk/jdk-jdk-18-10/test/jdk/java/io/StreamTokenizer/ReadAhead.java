/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4150737
   @summary Ensure that StreamTokenizer does not read any further ahead
            than is absolutely necessary
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.IOException;


public class ReadAhead {


    /* An InputStream subclass that cannot read past a given limit */
    private static class LimitedInputStream extends InputStream {

        private String input;
        private int limit;      /* Do not allow input[limit] to be read */
        private int next = 0;

        public LimitedInputStream(String input, int limit) {
            this.input = input;
            this.limit = limit;
        }

        public int read() throws IOException {
            if (next >= limit)
                throw new IOException("Attempted to read too far in stream");
            return input.charAt(next++);
        }

    }


    /* A Reader subclass that cannot read past a given limit */
    private static class LimitedReader extends Reader {

        private String input;
        private int limit;      /* Do not allow input[limit] to be read */
        private int next = 0;

        public LimitedReader(String input, int limit) {
            this.input = input;
            this.limit = limit;
        }

        public int read() throws IOException {
            if (next >= limit)
                throw new IOException("Attempted to read too far in stream");
            return input.charAt(next++);
        }

        public int read(char[] b, int off, int len) throws IOException {
            int top = off + len;
            int i;
            for (i = off; i < top; i++) {
                int c = read();
                if (c < 0) break;
                b[i] = (char)c;
            }
            return i - off;
        }

        public void close() { }

    }


    /* Interface for objects that can create new StreamTokenizers
       with a given limited input */
    private static interface StreamTokenizerMaker {
        public StreamTokenizer create(String input, int limit);
    }

    private static void fail(String why) throws Exception {
        throw new Exception(why);
    }

    private static void test(StreamTokenizer st) throws Exception {
        st.eolIsSignificant(true);
        int tt = st.nextToken();
        if (tt != StreamTokenizer.TT_WORD) fail("expected TT_WORD");
        if (!st.sval.equals("foo")) fail("expected word token \"foo\"");
        tt = st.nextToken();
        if (tt != StreamTokenizer.TT_EOL) fail("expected TT_EOL");
    }

    private static void test(StreamTokenizerMaker stm) throws Exception {
        test(stm.create("foo\nx", 4));
        test(stm.create("foo\r\nx", 4));
    }


    public static void main(String[] args) throws Exception {

        /* InputStream case */
        test(new StreamTokenizerMaker() {
            public StreamTokenizer create(String input, int limit) {
                return new StreamTokenizer(new LimitedInputStream(input, limit));
            }});

        /* Reader case */
        test(new StreamTokenizerMaker() {
            public StreamTokenizer create(String input, int limit) {
                return new StreamTokenizer(new LimitedReader(input, limit));
            }});

    }

}
