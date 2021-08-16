/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8183554
 * @summary Test to verify the new Constructors that take a Charset.
 * @run testng ConstructorTest
 */
public class ConstructorTest {
    static String USER_DIR = System.getProperty("user.dir", ".");

    public static enum ConstructorType {
        STRING,
        FILE,
        STRING_APPEND,
        FILE_APPEND
    }

    static final String TEST_STRING = "abc \u0100 \u0101 \u0555 \u07FD \u07FF";
    static final int BUFFER_SIZE = 8192;

    @DataProvider(name = "parameters")
    public Object[][] getParameters() throws IOException {
        File file1 = new File(USER_DIR, "FileWriterTest1.txt");
        File file2 = new File(USER_DIR, "FileWriterTest2.txt");

        return new Object[][]{
            {ConstructorType.STRING, file1, file2, StandardCharsets.UTF_8},
            {ConstructorType.FILE, file1, file2, StandardCharsets.UTF_8},
            {ConstructorType.STRING_APPEND, file1, file2, StandardCharsets.UTF_8},
            {ConstructorType.FILE_APPEND, file1, file2, StandardCharsets.UTF_8},
            {ConstructorType.STRING, file1, file2, StandardCharsets.ISO_8859_1},
            {ConstructorType.FILE, file1, file2, StandardCharsets.ISO_8859_1},
            {ConstructorType.STRING_APPEND, file1, file2, StandardCharsets.ISO_8859_1},
            {ConstructorType.FILE_APPEND, file1, file2, StandardCharsets.ISO_8859_1},
        };
    }

    /**
     * Verifies that the new constructors that take a Charset function the same
     * as an OutputStreamWriter on a FileOutputStream as was recommended before
     * this change.
     *
     * @param type the type of the constructor
     * @param file1 file1 to be written with a FileWriter
     * @param file2 file2 to be written  with an OutputStreamWriter
     * @param charset the charset
     * @throws IOException
     */
    @Test(dataProvider = "parameters")
    void test(ConstructorType type, File file1, File file2, Charset charset)
            throws Exception {
        writeWithFileWriter(type, file1, TEST_STRING, charset);
        writeWithOutputStreamWriter(type, file2, TEST_STRING, charset);

        try (
                FileReader r1 = getFileReader(type, file1, charset);
                FileReader r2 = getFileReader(type, file2, charset);
            ) {
            String result1 = readAll(r1, BUFFER_SIZE);
            String result2 = readAll(r2, BUFFER_SIZE);
            Assert.assertEquals(result1, result2);
        }
    }

    public String readAll(Reader reader, int bufferSize) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[bufferSize];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            if (numRead == buf.length) {
                sb.append(buf);
            } else {
                sb.append(String.valueOf(buf, 0, numRead));
            }
        }
        return sb.toString();
    }

    /*
     * Creates a FileReader over the given input file.
     */
    FileReader getFileReader(ConstructorType type, File file, Charset charset)
            throws IOException {
        switch (type) {
            case STRING:
            case STRING_APPEND:
                return new FileReader(file.getPath(), charset);
            case FILE:
            case FILE_APPEND:
                return new FileReader(file, charset);
        }

        return null;
    }

    /*
     * Creates a FileWriter using the constructor as specified.
     */
    FileWriter getFileWriter(ConstructorType type, File file, Charset charset)
            throws IOException {
        switch (type) {
            case STRING:
                return new FileWriter(file.getPath(), charset);
            case FILE:
                return new FileWriter(file, charset);
            case STRING_APPEND:
                return new FileWriter(file.getPath(), charset, true);
            case FILE_APPEND:
                return new FileWriter(file, charset, true);
        }

        return null;
    }

    void writeWithFileWriter(ConstructorType type, File file, String content, Charset charset)
            throws IOException {
        if (type == ConstructorType.STRING_APPEND || type == ConstructorType.FILE_APPEND) {
            try (FileWriter writer = getFileWriter(ConstructorType.FILE, file, charset);) {
                writer.write(content);
            }
        }
        try (FileWriter writer = getFileWriter(type, file, charset);) {
            writer.write(content);
        }
    }

    void writeWithOutputStreamWriter(ConstructorType type, File file, String content, Charset charset)
            throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), charset)) {
            writer.write(content);
            if (type == ConstructorType.STRING_APPEND || type == ConstructorType.FILE_APPEND) {
                writer.write(content);
            }
        }
    }
}
