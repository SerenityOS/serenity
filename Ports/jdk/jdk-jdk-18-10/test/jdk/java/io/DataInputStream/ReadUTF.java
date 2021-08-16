/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4806007
 * @summary Checks for vague exceptions from writeUTF/readUTF
 * @key randomness
 */

import java.io.*;
import java.util.*;

public class ReadUTF {

    private static Random generator = new Random();

    private static final int TEST_ITERATIONS = 1000;

    private static final int A_NUMBER_NEAR_65535 = 60000;

    private static final int MAX_CORRUPTIONS_PER_CYCLE = 3;

    public static final void main(String[] args) throws Exception {
        for (int i=0; i<TEST_ITERATIONS; i++) {
            try {
                writeAndReadAString();
            } catch (UTFDataFormatException utfdfe) {
                if (utfdfe.getMessage() == null)
                    throw new RuntimeException("vague exception thrown");
            } catch (EOFException eofe) {
                // These are rare and beyond the scope of the test
            }
        }
    }

    private static void writeAndReadAString() throws Exception {
        // Write out a string whose UTF-8 encoding is quite possibly
        // longer than 65535 bytes
        int length = generator.nextInt(A_NUMBER_NEAR_65535) + 1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuffer testBuffer = new StringBuffer();
        for (int i=0; i<length; i++)
            testBuffer.append((char)generator.nextInt());
        String testString = testBuffer.toString();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(testString);

        // Corrupt the data to produce malformed characters
        byte[] testBytes = baos.toByteArray();
        int dataLength = testBytes.length;
        int corruptions = generator.nextInt(MAX_CORRUPTIONS_PER_CYCLE);
        for (int i=0; i<corruptions; i++) {
            int index = generator.nextInt(dataLength);
            testBytes[index] = (byte)generator.nextInt();
        }

        // Pay special attention to mangling the end to produce
        // partial characters at end
        testBytes[dataLength-1] = (byte)generator.nextInt();
        testBytes[dataLength-2] = (byte)generator.nextInt();

        // Attempt to decode the bytes back into a String
        ByteArrayInputStream bais = new ByteArrayInputStream(testBytes);
        DataInputStream dis = new DataInputStream(bais);
        dis.readUTF();
    }
}
