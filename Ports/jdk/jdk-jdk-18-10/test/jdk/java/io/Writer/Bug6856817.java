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

/*
 * @test
 * @bug 6856817
 * @summary optimize the Writer.append(CharSequence) method
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 *
 * @author vyom.tewari@oacle.com
 */
public class Bug6856817 {

    private static final String str = "This is just a test string that i am using it to test the CharBuffer.append(CharSequence csq)  for little bit performance improvement.";
    private static final int BUF_SIZE = 1024;

    /**
     *
     * @param args
     */
    public static void main(String args[]) {
        CharBuffer charBuffer = CharBuffer.allocate(BUF_SIZE);
        File file = new File("temp.txt");
        file.deleteOnExit();

        charBuffer.put(str);
        charBuffer.flip();
        checkFileContent(charBuffer, file, str);
        charBuffer.position(10);
        checkFileContent(charBuffer, file, str.substring(10));
        charBuffer.position(charBuffer.limit());
        checkFileContent(charBuffer, file, str.substring(charBuffer.limit()));

        char arr[] = new char[BUF_SIZE];
        charBuffer = CharBuffer.wrap(arr);
        charBuffer.put(str);
        charBuffer.flip();
        checkFileContent(charBuffer, file, str);
        charBuffer.position(10);
        checkFileContent(charBuffer, file, str.substring(10));
        charBuffer.position(charBuffer.limit());
        checkFileContent(charBuffer, file, str.substring(charBuffer.limit()));

        char secArr[] = new char[BUF_SIZE];
        charBuffer = CharBuffer.wrap(secArr);
        charBuffer.put(str);
        charBuffer.position(5);
        charBuffer.limit(str.length() - 7);
        charBuffer = charBuffer.slice();
        checkFileContent(charBuffer, file, str.substring(5, (str.length() - 7)));
        charBuffer.position(10);
        checkFileContent(charBuffer, file, str.substring(15, (str.length() - 7)));
        charBuffer.position(charBuffer.limit());
        checkFileContent(charBuffer, file, str.substring(charBuffer.limit()));

        charBuffer = ByteBuffer.allocate(BUF_SIZE).asCharBuffer();
        charBuffer.put(str);
        charBuffer.flip();
        checkFileContent(charBuffer, file, str);
        charBuffer.position(10);
        checkFileContent(charBuffer, file, str.substring(10));
        charBuffer.position(charBuffer.limit());
        checkFileContent(charBuffer, file, str.substring(charBuffer.limit()));

        charBuffer = ByteBuffer.allocateDirect(1024).asCharBuffer();
        charBuffer.put(str);
        charBuffer.flip();
        checkFileContent(charBuffer, file, str);
        charBuffer.position(10);
        checkFileContent(charBuffer, file, str.substring(10));
        charBuffer.position(charBuffer.limit());
        checkFileContent(charBuffer, file, str.substring(charBuffer.limit()));
    }

    private static void checkFileContent(CharBuffer charBuffer, File file, String expectedValue) {
        OutputStreamWriter writer = null;
        FileReader reader = null;
        int position, limit;
        position = charBuffer.position();
        limit = charBuffer.limit();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            writer = new OutputStreamWriter(outputStream);
            writer.append(charBuffer);
            writer.close();
            if (!isEqual(position, charBuffer.position())) {
                System.out.println(": failed");
                throw new RuntimeException("buffer position before write: " + position + " and position after write: " + charBuffer.position());
            }
            if (!isEqual(limit, charBuffer.limit())) {
                System.out.println(": failed");
                throw new RuntimeException("buffer limit before write: " + limit + " and limit after write: " + charBuffer.limit());
            }
            reader = new FileReader(file);
            char arr[] = new char[BUF_SIZE];
            int byteRead = reader.read(arr);
            if (byteRead != -1) {
                String stringRead = new String(arr, 0, byteRead);
                if (expectedValue.equals(stringRead)) {
                    System.out.println(": passed");
                } else {
                    System.out.println(": failed");
                    throw new RuntimeException("expected :" + expectedValue + " and got:" + stringRead);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static boolean isEqual(final int first, final int second) {
        return (first == second);
    }
}
