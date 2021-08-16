/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4071765
   @summary Bug in the parameter of str.getChars called in write
*/



import java.io.*;

public class WriteFromString {


    public static void main(String argv[]) throws Exception {
        LocalStringWriter lsw = new LocalStringWriter();
        boolean result = true;

        String testString = "Testing of what gets written";
        // Should write out at offset 2 for length 4 i.e."stin"
        lsw.write(testString, 1, 4);
        String res = lsw.toString();
        if (!res.equals("esti")) {
            result = false;
            System.err.println("Writer.write is incorrect:" + res);
        }

        // Same bug in stringwriter as well
        StringWriter sw = new StringWriter();
        sw.write(testString, 1, 4);
        res = sw.toString();
        String ss = testString.substring(1,4);
        System.out.println("Substring = "+ss);
        if (!res.equals("esti")) {
            System.err.println("StringWriter.write is incorrect:" + res);
            result = false;
        }
        if (!result) {
            throw new Exception("Writer.write method is incorrect.");
        }
    }

}

/**
 * A copy of StringWriter to test the write method in Writer
 */

class LocalStringWriter extends Writer {

    private StringBuffer buf;

    /**
     * Create a new string writer, using the default initial string-buffer
     * size.
     */
    public LocalStringWriter() {
        buf = new StringBuffer();
        lock = buf;
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param  cbuf  Array of characters
     * @param  off   Offset from which to start writing characters
     * @param  len   Number of characters to write
     */
    public void write(char cbuf[], int off, int len) {
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
            ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        buf.append(cbuf, off, len);
    }

    /**
     * Write a string.
     */
    public void write(String str) {
        buf.append(str);
    }

    /**
     * Return the buffer's current value as a string.
     */
    public String toString() {
        return buf.toString();
    }


    public void flush(){ }

    public void close(){ }

}
