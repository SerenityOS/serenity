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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Scanner;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @bug 8183743
 * @summary Test to verify the new overload method with Charset functions the
 * same as the existing method that takes a charset name.
 * @run testng EncodingTest
 */
public class EncodingTest {
    static String USER_DIR = System.getProperty("user.dir", ".");

    public static enum ConstructorType {
        FILE,
        PATH,
        INPUTSTREAM,
        READABLEBYTECHANNEL
    }

    static final String TEST_STRING = "abc \u0100 \u0101 \u0555 \u07FD \u07FF";

    @DataProvider(name = "parameters")
    public Object[][] getParameters() throws IOException {
        String csn = StandardCharsets.UTF_8.name();
        Charset charset = StandardCharsets.UTF_8;
        File file1 = new File(USER_DIR, "ScannerCharsetTest1.txt");
        File file2 = new File(USER_DIR, "ScannerCharsetTest2.txt");

        return new Object[][]{
            {ConstructorType.FILE, file1, file2, csn, charset},
            {ConstructorType.PATH, file1, file2, csn, charset},
            {ConstructorType.INPUTSTREAM, file1, file2, csn, charset},
            {ConstructorType.READABLEBYTECHANNEL, file1, file2, csn, charset},};
    }

    /**
     * Verifies that the overloading constructor behaves the same as the
     * existing one.
     *
     * @param type the type of the constructor
     * @param file1 file1 written with the name of a charset
     * @param file2 file2 written with a charset
     * @param csn the charset name
     * @param charset the charset
     * @throws IOException
     */
    @Test(dataProvider = "parameters")
    void test(ConstructorType type, File file1, File file2, String csn, Charset charset)
            throws Exception {
        prepareFile(file1, TEST_STRING);
        prepareFile(file2, TEST_STRING);

        try (Scanner s1 = getScanner(type, file1.getPath(), csn, null);
                Scanner s2 = getScanner(type, file2.getPath(), null, charset);) {
            String result1 = s1.findInLine(TEST_STRING);
            String result2 = s2.findInLine(TEST_STRING);
            Assert.assertEquals(result1, result2);
        }
    }

    /*
     * Creates a Scanner over the given input file.
     */
    Scanner getScanner(ConstructorType type, String file, String csn, Charset charset)
            throws Exception {
        if (csn != null) {
            switch (type) {
                case FILE:
                    return new Scanner(new File(file), csn);
                case PATH:
                    return new Scanner(Paths.get(file), csn);
                case INPUTSTREAM:
                    FileInputStream fis = new FileInputStream(file);
                    return new Scanner(fis, csn);
                case READABLEBYTECHANNEL:
                    FileInputStream fis1 = new FileInputStream(file);
                    return new Scanner(fis1.getChannel(), csn);
            }
        } else {
            switch (type) {
                case FILE:
                    return new Scanner(new File(file), charset);
                case PATH:
                    return new Scanner(Paths.get(file), charset);
                case INPUTSTREAM:
                    FileInputStream fis = new FileInputStream(file);
                    return new Scanner(fis, charset);
                case READABLEBYTECHANNEL:
                    FileInputStream fis1 = new FileInputStream(file);
                    return new Scanner(fis1.getChannel(), charset);
            }
        }

        return null;
    }

    void prepareFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file);) {
            writer.write(content);
        }
    }
}
