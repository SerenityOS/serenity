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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.Locale;
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
    static String USER_DIR = System.getProperty("user.dir", ".");

    // Charset added only for the 3-parameter constructors
    public static enum ConstructorType {
        STRING3,
        FILE3,
        OUTPUTSTREAM3
    }

    @DataProvider(name = "parameters")
    public Object[][] getParameters() throws IOException {
        Locale l = Locale.getDefault();
        String csn1 = StandardCharsets.ISO_8859_1.name();
        Charset charset1 = StandardCharsets.ISO_8859_1;
        String csn2 = StandardCharsets.UTF_8.name();
        Charset charset2 = StandardCharsets.UTF_8;

        File file1 = new File(USER_DIR, "FormatterCharsetTest1.txt");
        File file2 = new File(USER_DIR, "FormatterCharsetTest2.txt");

        return new Object[][]{
            {ConstructorType.STRING3, file1, file2, csn1, charset1},
            {ConstructorType.FILE3, file1, file2, csn1, charset1},
            {ConstructorType.OUTPUTSTREAM3, file1, file2, csn1, charset1},
            {ConstructorType.STRING3, file1, file2, csn2, charset2},
            {ConstructorType.FILE3, file1, file2, csn2, charset2},
            {ConstructorType.OUTPUTSTREAM3, file1, file2, csn2, charset2},
        };
    }

    /**
     * Verifies that the overloading constructor behaves the same as the existing
     * one.
     * @param type the type of the constructor
     * @param file1 file1 written with the name of a charset
     * @param file2 file2 written with a charset
     * @param csn the charset name
     * @param charset the charset
     * @throws IOException
     */
    @Test(dataProvider = "parameters")
    public void testConstructor(ConstructorType type, File file1, File file2,
            String csn, Charset charset) throws Exception {
        format(getFormatter(type, file1.getPath(), csn, null));
        format(getFormatter(type, file2.getPath(), null, charset));
        Assert.assertEquals(Files.readAllLines(Paths.get(file1.getPath()), charset),
                Files.readAllLines(Paths.get(file2.getPath()), charset));
    }

    void format(Formatter formatter)
            throws IOException {
        formatter.format("abcde \u00FA\u00FB\u00FC\u00FD");
        formatter.format("Java \uff08\u8ba1\u7b97\u673a\u7f16\u7a0b\u8bed\u8a00\uff09");
        formatter.flush();
        formatter.close();
    }


    Formatter getFormatter(ConstructorType type, String path, String csn, Charset charset)
            throws IOException {
        Formatter formatter = null;
        if (csn != null) {
            switch (type) {
                case STRING3:
                    formatter = new Formatter(path, csn, Locale.getDefault());
                    break;
                case FILE3:
                    formatter = new Formatter(new File(path), csn, Locale.getDefault());
                    break;
                case OUTPUTSTREAM3:
                    formatter = new Formatter(new FileOutputStream(path), csn, Locale.getDefault());
                    break;
            }
        } else {
            switch (type) {
                case STRING3:
                    formatter = new Formatter(path, charset, Locale.getDefault());
                    break;
                case FILE3:
                    formatter = new Formatter(new File(path), charset, Locale.getDefault());
                    break;
                case OUTPUTSTREAM3:
                    formatter = new Formatter(new FileOutputStream(path), charset, Locale.getDefault());
                    break;
            }
        }
        return formatter;
    }
}
