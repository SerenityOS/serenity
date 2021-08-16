/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/*
 * @test
 * @bug 8048604
 * @summary This test verifies the assertion "The chaining feature of
 *  Filter streams should be supported." for feature "CipherInputStream &
 *  CipherOutputStream"
 * @run main CICOChainingTest
 */
public class CICOChainingTest {
    /**
     * Plain text length.
     */
    private static final int PLAIN_TEXT_LENGTH = 200;

    public static void main(String argv[]) throws Exception {
        CICOChainingTest test = new CICOChainingTest();
        test.chainTest(true);
        test.chainTest(false);
    }

    /**
     * Chain CipherInputStream/CipherOutputStream with other stream, encrypt
     * the text and decrypt it, recovered text is supposed to be same as
     * original text.
     * @param useInt true if read byte by byte false if read with buffer.
     * @throws IOException any I/O operation failed.
     */
    public void chainTest(boolean useInt) throws IOException {
        byte[] plainText = TestUtilities.generateBytes(PLAIN_TEXT_LENGTH);
        byte[] recoveredText = new byte[plainText.length];
        // Do initialization
        try (MyNullCipherInputStream ciInput1 = new MyNullCipherInputStream(
                new ByteArrayInputStream(plainText));
                PipedOutputStream piOut = new PipedOutputStream();
                MyNullCipherInputStream ciInput2 = new MyNullCipherInputStream(
                        new PipedInputStream(piOut));
                MyNullCipherOutputStream ciOut = new MyNullCipherOutputStream(
                        piOut);) {
            if (useInt) {
                int buffer = ciInput1.read();
                while (buffer != -1) {
                    piOut.write(buffer);
                    buffer = ciInput1.read();
                }
            } else {
                byte[] buffer = new byte[20];
                int len = ciInput1.read(buffer);
                while (len != -1) {
                    ciOut.write(buffer, 0, len);
                    len = ciInput1.read(buffer);
                }
            }
            ciOut.flush();
            piOut.flush();
            // Get the output
            ciInput2.read(recoveredText);
            if (ciInput2.available() > 0) {
                throw new RuntimeException("Expected no data from ciInput2, but"
                        + " ciInput2.available() = " + ciInput2.available());
            }
        }
        // Verify output is same to input.
        if (!Arrays.equals(plainText, recoveredText)) {
            throw new RuntimeException("plainText:" + new String(plainText)
                    + " recoveredText:" + new String(recoveredText)
                    + " Test failed due to result compare fail");
        }
    }
}

class MyNullCipherInputStream extends CipherInputStream {

    public MyNullCipherInputStream(InputStream is) {
        super(is);
    }
}

class MyNullCipherOutputStream extends CipherOutputStream {

    public MyNullCipherOutputStream(OutputStream os) {
        super(os);
    }
}
