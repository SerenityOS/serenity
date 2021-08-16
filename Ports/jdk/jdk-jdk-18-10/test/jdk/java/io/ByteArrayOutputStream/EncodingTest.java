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
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    /*
     * DataProvider for the toString method test. Provides the following fields:
     * byte array, charset name string, charset object
     */
    @DataProvider(name = "parameters")
    public Object[][] getParameters() throws IOException {
        byte[] data = getData();
        return new Object[][]{
            {data, StandardCharsets.UTF_8.name(), StandardCharsets.UTF_8},
            {data, StandardCharsets.ISO_8859_1.name(), StandardCharsets.ISO_8859_1},
        };
    }

    /**
     * Verifies that the new overload method that takes a Charset is equivalent to
     * the existing one that takes a charset name.
     * @param data a byte array
     * @param csn the charset name
     * @param charset the charset
     * @throws Exception if the test fails
     */
    @Test(dataProvider = "parameters")
    public void test(byte[] data, String csn, Charset charset) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data);
        String str1 = baos.toString(csn);
        String str2 = baos.toString(charset);
        Assert.assertEquals(str1, str2);
    }

    /*
     * Returns an array containing a character that's invalid for UTF-8
     * but valid for ISO-8859-1
     */
    byte[] getData() throws IOException {
        String str1 = "A string that contains ";
        String str2 = " , an invalid character for UTF-8.";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(str1.getBytes());
        baos.write(0xFA);
        baos.write(str2.getBytes());
        return baos.toByteArray();
    }
}
