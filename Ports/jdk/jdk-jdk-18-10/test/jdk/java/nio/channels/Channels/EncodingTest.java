/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8183743
 * @summary Test to verify the new overload method with Charset functions the same
 * as the existing method that takes a charset name.
 * @run testng EncodingTest
 */
public class EncodingTest {
    static final int ITERATIONS = 2;
    public static final String CS_UTF8 = StandardCharsets.UTF_8.name();
    public static final String CS_ISO8859 = StandardCharsets.ISO_8859_1.name();
    static String USER_DIR = System.getProperty("user.dir", ".");

    // malformed input: a high surrogate without the low surrogate
    static char[] illChars = {
        '\u00fa', '\ud800'
    };

    static byte[] data = getData();

    static byte[] getData() {
        try {
            String str1 = "A string that contains ";
            String str2 = " , an invalid character for UTF-8.";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(str1.getBytes());
            baos.write(0xFA);
            baos.write(str2.getBytes());
            return baos.toByteArray();
        } catch (IOException ex) {
            return null; //shouldn't happen
        }
    }

    String testFile = Paths.get(USER_DIR, "channelsEncodingTest.txt").toString();
    String testIllegalInput = Paths.get(USER_DIR, "channelsIllegalInputTest.txt").toString();
    String testIllegalOutput = Paths.get(USER_DIR, "channelsIllegalOutputTest.txt").toString();


    /*
     * DataProvider for read and write test.
     * Writes and reads with the same encoding
     */
    @DataProvider(name = "writeAndRead")
    public Object[][] getWRParameters() {
        return new Object[][]{
            {testFile, StandardCharsets.ISO_8859_1.name(), null,
                StandardCharsets.ISO_8859_1.name(), StandardCharsets.ISO_8859_1},
            {testFile, null, StandardCharsets.ISO_8859_1,
                StandardCharsets.ISO_8859_1.name(), StandardCharsets.ISO_8859_1},
            {testFile, StandardCharsets.UTF_8.name(), null,
                StandardCharsets.UTF_8.name(), StandardCharsets.UTF_8},
            {testFile, null, StandardCharsets.UTF_8,
                StandardCharsets.UTF_8.name(), StandardCharsets.UTF_8}
        };
    }

    /*
     * DataProvider for illegal input test
     * Writes the data in ISO8859 and reads with UTF8, expects MalformedInputException
     */
    @DataProvider(name = "illegalInput")
    public Object[][] getParameters() {
        return new Object[][]{
            {testIllegalInput, StandardCharsets.ISO_8859_1.name(), null, StandardCharsets.UTF_8.name(), null},
            {testIllegalInput, StandardCharsets.ISO_8859_1.name(), null, null, StandardCharsets.UTF_8},
            {testIllegalInput, null, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8.name(), null},
            {testIllegalInput, null, StandardCharsets.ISO_8859_1, null, StandardCharsets.UTF_8},
        };
    }

    /*
     * DataProvider for illegal output test
     * Attemps to write some malformed chars, expects MalformedInputException
     */
    @DataProvider(name = "illegalOutput")
    public Object[][] getWriteParameters() {
        return new Object[][]{
            {testIllegalOutput, StandardCharsets.UTF_8.name(), null},
            {testIllegalOutput, null, StandardCharsets.UTF_8}
        };
    }

    /**
     * Verifies that the Readers created with the following methods are
     * equivalent:
     * newReader(ReadableByteChannel ch, String csName)
     * newReader(ReadableByteChannel ch, Charset charset)
     *
     * The verification follows the following steps:
     * Writes a file with a writer created with the specified charset
     * Reads it with a reader created with newReader using the same charset;
     * Compares that the results are the same.
     *
     * @param file the file name
     * @param csnWriter the charset name for creating the writer
     * @param charsetWriter the charset for creating the writer
     * @param csnReader the charset name for creating the reader
     * @param charsetReader the charset for creating the reader
     * @throws Exception
     */
    @Test(dataProvider = "writeAndRead")
    public void testWriteAndRead(String file, String csnWriter, Charset charsetWriter,
            String csnReader, Charset charsetReader) throws Exception {
        writeToFile(data, file, csnWriter, charsetWriter);
        // read using charset name
        String result1 = readFileToString(file, csnReader, null);
        String result2 = readFileToString(file, null, charsetReader);

        Assert.assertEquals(result1, result2);
    }

    /**
     * Verifies that MalformedInputException is thrown when an input byte sequence
     * is illegal for given charset that is configured for the reader.
     *
     * @param file the file to be read
     * @param csnWriter the charset name for creating the writer
     * @param charsetWriter the charset for creating the writer
     * @param csnReader the charset name for creating the reader
     * @param charsetReader the charset for creating the reader
     * @throws Exception
     */
    @Test(dataProvider = "illegalInput", expectedExceptions = MalformedInputException.class)
    void testMalformedInput(String file, String csnWriter, Charset charsetWriter,
            String csnReader, Charset charsetReader) throws Exception {
        writeToFile(data, file, csnWriter, charsetWriter);
        readFileToString(file, csnReader, charsetReader);
    }

    /**
     * Attempts to write illegal characters using a writer created by calling
     * the newWriter method and expects a MalformedInputException.
     *
     * @param fileName the file name
     * @param csn the charset name
     * @param charset the charset
     * @throws Exception
     */
    @Test(dataProvider = "illegalOutput", expectedExceptions = MalformedInputException.class)
    public void testMalformedOutput(String fileName, String csn, Charset charset)
            throws Exception {
        try (FileOutputStream fos = new FileOutputStream(fileName);
                WritableByteChannel wbc = (WritableByteChannel) fos.getChannel();) {
            Writer writer;
            if (csn != null) {
                writer = Channels.newWriter(wbc, csn);
            } else {
                writer = Channels.newWriter(wbc, charset);
            }

            for (int i = 0; i < ITERATIONS; i++) {
                writer.write(illChars);
            }
            writer.flush();
            writer.close();
        }
    }

    /**
     * Writes the data to a file using a writer created by calling the newWriter
     * method.
     *
     * @param data the data to be written
     * @param fileName the file name
     * @param csn the charset name
     * @param charset the charset
     * @throws Exception
     */
    private void writeToFile(byte[] data, String fileName, String csn, Charset charset) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(fileName);
                WritableByteChannel wbc = (WritableByteChannel) fos.getChannel()) {
            Writer writer;
            String temp;
            if (csn != null) {
                writer = Channels.newWriter(wbc, csn);
                temp = new String(data, csn);
            } else {
                writer = Channels.newWriter(wbc, charset);
                temp = new String(data, charset);
            }

            for (int i = 0; i < ITERATIONS; i++) {
                writer.write(temp);
            }
            writer.flush();
            writer.close();
        }
    }

    /**
     * Reads a file into a String.
     *
     * @param file the file to be read
     * @param csn the charset name
     * @param charset the charset
     * @throws Exception
     */
    String readFileToString(String file, String csn, Charset charset) throws Exception {
        String result;
        try (FileInputStream fis = new FileInputStream(file);
                ReadableByteChannel rbc = (ReadableByteChannel) fis.getChannel()) {
            Reader reader;
            if (csn != null) {
                reader = Channels.newReader(rbc, csn);
            } else {
                reader = Channels.newReader(rbc, charset);
            }

            int messageSize = data.length * ITERATIONS;
            char data1[] = new char[messageSize];
            int totalRead = 0;
            int charsRead = 0;
            while (totalRead < messageSize) {
                totalRead += charsRead;
                charsRead = reader.read(data1, totalRead, messageSize - totalRead);
            }

            result = new String(data1, 0, totalRead);
            reader.close();
        }

        return result;
    }
}
