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
 * @bug 4151072
 * @summary Ensure that BufferedReader's methods handle the new line character
 *          following the carriage return correctly after a readLine
 *          operation that resulted in reading a line terminated by a
 *          carriage return (\r).
 */
import java.io.*;

public class ReadLine {

    public static void main(String[] args) throws IOException {
        // Make sure that the reader does not wait for additional characters to
        // be read after reading a new line.
        BufferedReader reader;
        String[][] strings = {
            {"CR/LF\r\n", "CR/LF"},
            {"LF-Only\n", "LF-Only"},
            {"CR-Only\r", "CR-Only"},
            {"CR/LF line\r\nMore data", "More data"}
        };

        // test 0 "CR/LF\r\n"
        // test 1 "LF-Only\n"
        // test 2 "CR-Only\r"
        for (int i = 0; i < 3; i++) {
            reader = new BufferedReader(new
                    BoundedReader(strings[i][0]), strings[i][0].length());
            if (!reader.readLine().equals(strings[i][1]))
                throw new RuntimeException("Read incorrect text");
        }


        // Now test the mark and reset operations. Consider two cases.
        // 1. For lines ending with CR only.
        markResetTest("Lot of textual data\rMore textual data\n",
                "More textual data");

        // 2. Now for lines ending with CR/LF
        markResetTest("Lot of textual data\r\nMore textual data\n",
                "More textual data");

        // 3. Now for lines ending with LF only
        markResetTest("Lot of textual data\nMore textual data\n",
                "More textual data");

        // Need to ensure behavior of read() after a readLine() read of a CR/LF
        // terminated line.
        // 1.  For lines ending with CR/LF only.

        // uses "CR/LF line\r\nMore data"
        reader = new BufferedReader(new
                BoundedReader(strings[3][0]), strings[3][0].length());
        reader.readLine();
        if (reader.read() != 'M')
            throw new RuntimeException("Read() failed");


        // Need to ensure that a read(char[], int, int) following a readLine()
        // read of a CR/LF terminated line behaves correctly.

        // uses "CR/LF line\r\nMore data"
        reader = new BufferedReader(new
                BoundedReader(strings[3][0]), strings[3][0].length());
        reader.readLine();

        char[] buf = new char[9];
        reader.read(buf, 0, 9);
        String newStr = new String(buf);
        if (!newStr.equals(strings[3][1]))
            throw new RuntimeException("Read(char[],int,int) failed");
    }

    static void markResetTest(String inputStr, String resetStr)
        throws IOException {
        BufferedReader reader = new BufferedReader(new
                BoundedReader(inputStr), inputStr.length());
        System.out.println("> " + reader.readLine());
        reader.mark(30);
        System.out.println("......Marking stream .....");
        String str = reader.readLine();
        System.out.println("> " + str);
        reader.reset();
        String newStr = reader.readLine();
        System.out.println("reset> " + newStr);

        // Make sure that the reset point was set correctly.
        if (!newStr.equals(resetStr))
            throw new RuntimeException("Mark/Reset failed");
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
                throw new RuntimeException("Read past limit");
            return content[pos++];
        }

        public int read(char[] buf, int offset, int length)
            throws IOException
        {
            int oldPos = pos;
            for (int i = offset; i < length; i++) {
                buf[i] = (char)read();
            }
            return (pos - oldPos);
        }

        public void close() {}
    }

}
