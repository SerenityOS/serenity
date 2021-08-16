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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
        FILE
    }

    static final String TEST_STRING = "abc \u0100 \u0101 \u0555 \u07FD \u07FF";
    static final int BUFFER_SIZE = 8192;

    @DataProvider(name = "parameters")
    public Object[][] getParameters() throws IOException {
        File file1 = new File(USER_DIR, "FileReaderTest1.txt");
        File file2 = new File(USER_DIR, "FileReaderTest2.txt");

        return new Object[][]{
            {ConstructorType.STRING, file1, file2, StandardCharsets.UTF_8},
            {ConstructorType.FILE, file1, file2, StandardCharsets.UTF_8},
            {ConstructorType.STRING, file1, file2, StandardCharsets.ISO_8859_1},
            {ConstructorType.FILE, file1, file2, StandardCharsets.ISO_8859_1},
        };
    }

    /**
     * Verifies that the new constructors that take a Charset function the same
     * as an InputStreamReader on a FileInputStream as was recommended before
     * this change.
     *
     * @param type the type of the constructor
     * @param file1 file1 to be read with a FileReader
     * @param file2 file2 to be read with an InputStreamReader
     * @param charset the charset
     * @throws IOException
     */
    @Test(dataProvider = "parameters")
    void test(ConstructorType type, File file1, File file2, Charset charset)
            throws Exception {
        prepareFile(file1, TEST_STRING, charset);
        prepareFile(file2, TEST_STRING, charset);

        try (FileReader fr = getFileReader(type, file1, charset);
                FileInputStream is = new FileInputStream(file2);
                InputStreamReader isr = new InputStreamReader(is, charset);) {
            String result1 = readAll(fr, BUFFER_SIZE);
            String result2 = readAll(isr, BUFFER_SIZE);
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
                return new FileReader(file.getPath(), charset);
            case FILE:
                return new FileReader(file, charset);
        }

        return null;
    }

    void prepareFile(File file, String content, Charset charset) throws IOException {
        try (FileWriter writer = new FileWriter(file, charset);) {
            writer.write(content);
        }
    }
}
