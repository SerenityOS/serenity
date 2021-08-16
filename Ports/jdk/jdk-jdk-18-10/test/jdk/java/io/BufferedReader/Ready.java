/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4329985
 * @summary Ensure that BufferedReader's ready() method handles the new line
 * character following the carriage return correctly and returns the right
 * value so that a read operation after a ready() does not block unnecessarily.
 */
import java.io.*;

public class Ready {

    public static void main(String[] args) throws IOException {
        BufferedReader reader;
        String[] strings = {
            "LF-Only\n",
            "LF-Only\n",
            "CR/LF\r\n",
            "CR/LF\r\n",
            "CR-Only\r",
            "CR-Only\r",
            "CR/LF line\r\nMore data.\r\n",
            "CR/LF line\r\nMore data.\r\n"
        };

        // The buffer sizes are chosen such that the boundary conditions are
        // tested.
        int[] bufsizes = { 7, 8, 6, 5, 7, 8, 11, 10};

        for (int i = 0; i < strings.length; i++) {
            reader = new BufferedReader(new BoundedReader(strings[i]),
                    bufsizes[i]);
            while (reader.ready()) {
                String str = reader.readLine();
                System.out.println("read>>" + str);
            }
        }
    }



    private static class BoundedReader extends Reader{

        private char[] content;
        private int limit;
        private int pos = 0;

        public BoundedReader(String content) {
            this.limit = content.length();
            this.content = new char[limit];
            content.getChars(0, limit, this.content, 0);
        }

        public int read() throws IOException {
            if (pos >= limit)
                throw new RuntimeException("Hit infinite wait condition");
            return content[pos++];
        }

        public int read(char[] buf, int offset, int length)
            throws IOException
        {
            if (pos >= limit)
                throw new RuntimeException("Hit infinite wait condition");
            int oldPos = pos;
            int readlen = (length > (limit - pos)) ? (limit - pos) : length;
            for (int i = offset; i < readlen; i++) {
                buf[i] = (char)read();
            }

            return (pos - oldPos);
        }

        public void close() {}

        public boolean ready() {
            if (pos < limit)
                return true;
            else
                return false;
        }
    }

}
