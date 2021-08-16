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

/*
 * @test
 * @bug 4734607 4767225 4863613
 * @summary Decode a file using EUC-TW, test for decode errors
 * @modules jdk.charsets
 * @run main EUCTWBufferBoundaryDecodeTest
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/*
 * Tests for decode errors in NIO EUC-TW decoder. 4734607 details
 * decoding errors which occur when the input file > 8k in size
 * and contains numerous US-ASCII range chars
 */
public class EUCTWBufferBoundaryDecodeTest {

    public static void main(String[] args) throws Exception {
        final String inputFileName
                = System.getProperty("os.name").startsWith("Windows")
                ? "tradChinese.win.po"
                : "tradChinese.po";

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 120);

        File inputFile = new File(System.getProperty("test.src"), inputFileName);
        FileInputStream fis = new FileInputStream(inputFile);

        // Decode the EUC_TW source file, re-encode it -- (roundtrip)
        // input and output files ought to be byte for byte identical
        BufferedReader bin
                = new BufferedReader(new InputStreamReader(fis, "EUC_TW"));
        BufferedWriter bout
                = new BufferedWriter(new OutputStreamWriter(bos, "EUC-TW"));

        String line = bin.readLine();

        while (line != null) {
            bout.write(line);
            bout.newLine();
            line = bin.readLine();
        }

        bout.close();
        bin.close();

        // Compare the output with the expected output byte by byte
        byte[] outputBytes = bos.toByteArray();
        int i = 0;
        FileInputStream fi = new FileInputStream(inputFile);

        while (i < outputBytes.length) {
            byte b = (byte) fi.read();
            if (b != outputBytes[i++]) {
                throw new Exception("bug 4734607: test failed");
            }
        }
        fi.close();
    }
}
