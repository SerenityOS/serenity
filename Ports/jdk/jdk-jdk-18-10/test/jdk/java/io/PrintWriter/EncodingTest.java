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
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    static String USER_DIR = System.getProperty("user.dir", ".");
    static boolean AUTOFLUSH = true;
    public static enum ConstructorType {
        STRING,
        FILE,
        OUTPUTSTREAM
    }

    /*
     * DataProvider fields:
     * Type of the constructor, a file to be written with a charset name,
     * a file to be written with a charset, charset name, charset.
     */
    @DataProvider(name = "parameters")
    public Object[][] getParameters() throws IOException {
        String csn = StandardCharsets.UTF_8.name();
        Charset charset = StandardCharsets.UTF_8;
        File file1 = new File(USER_DIR, "PWCharsetTest1.txt");
        File file2 = new File(USER_DIR, "PWCharsetTest2.txt");

        return new Object[][]{
            {ConstructorType.STRING, file1, file2, csn, charset},
            {ConstructorType.FILE, file1, file2, csn, charset},
            {ConstructorType.OUTPUTSTREAM, file1, file2, csn, charset}
        };
    }

    /**
     * Verifies that the overloading constructor behaves the same as the existing
     * one.
     *
     * @param type the type of the constructor
     * @param file1 file1 written with the name of a charset
     * @param file2 file2 written with a charset
     * @param csn the charset name
     * @param charset the charset
     * @throws IOException
     */
    @Test(dataProvider = "parameters")
    public void test(ConstructorType type, File file1, File file2, String csn, Charset charset)
            throws Exception {
        createFile(getWriter(type, file1.getPath(), csn, null));
        createFile(getWriter(type, file2.getPath(), null, charset));

        Assert.assertEquals(Files.readAllLines(Paths.get(file1.getPath()), charset),
                Files.readAllLines(Paths.get(file2.getPath()), charset));
    }

    void createFile(PrintWriter out) throws IOException {
        out.println("high surrogate");
        out.println(Character.MIN_HIGH_SURROGATE);
        out.println("low surrogate");
        out.println(Character.MIN_LOW_SURROGATE);
        out.flush();
        out.close();
    }

    PrintWriter getWriter(ConstructorType type, String path, String csn, Charset charset)
            throws IOException {
        PrintWriter out = null;
        if (csn != null) {
            switch (type) {
                case STRING:
                    out = new PrintWriter(path, csn);
                    break;
                case FILE:
                    out = new PrintWriter(new File(path), csn);
                    break;
                case OUTPUTSTREAM:
                    // No corresponding method with charset name
                    // compare with PrintWriter(path, csn) instead
                    out = new PrintWriter(path, csn);
                    break;
            }
        } else {
            switch (type) {
                case STRING:
                    out = new PrintWriter(path, charset);
                    break;
                case FILE:
                    out = new PrintWriter(new File(path), charset);
                    break;
                case OUTPUTSTREAM:
                    FileOutputStream fout = new FileOutputStream(path);
                    out = new PrintWriter(fout, AUTOFLUSH, charset);
                    break;
            }
        }

        return out;
    }
}
