/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertThrows;

/**
 * @test
 * @bug 8252739
 * @summary Verify Deflater.setDictionary(dictionary, offset, length) uses the offset
 * @run testng/othervm DeflaterDictionaryTests
 */
public class DeflaterDictionaryTests {
    // Output buffer size
    private static final int RESULT_SIZE = 1024;
    // Data to compress
    private static final String SRC_DATA = "Welcome to the US Open;".repeat(6);
    // Dictionary to be used
    private static final String DICTIONARY = "US Open";
    private static final int DICTIONARY_OFFSET = 1;
    private static final int DICTIONARY_LENGTH = 3;

    /**
     * DataProvider with offsets which should be valid for setDictionary
     *
     * @return valid offset values
     */
    @DataProvider(name = "validDictionaryOffsets")
    protected Object[][] validDictionaryOffsets() {
        return new Object[][]{
                {0},
                {DICTIONARY_OFFSET},
                {DICTIONARY_LENGTH}
        };
    }

    /**
     * DataProvider with  invalid offsets for setDictionary
     *
     * @return invalid offset values
     */
    @DataProvider(name = "invalidDictionaryOffsets")
    protected Object[][] invalidDictionaryOffsets() {
        return new Object[][]{
                {-1},
                {DICTIONARY_LENGTH + 2},
                {DICTIONARY.length()}
        };
    }

    /**
     * Validate that an offset can be used with Deflater::setDictionary
     *
     * @param dictionary_offset offset value to be used
     * @throws Exception if an error occurs
     */
    @Test(dataProvider = "validDictionaryOffsets")
    public void testByteArray(int dictionary_offset) throws Exception {
        byte[] input = SRC_DATA.getBytes(UTF_8);
        byte[] output = new byte[RESULT_SIZE];
        Deflater deflater = new Deflater();
        Inflater inflater = new Inflater();
        try {
            // Compress the bytes
            deflater.setDictionary(DICTIONARY.getBytes(UTF_8), dictionary_offset, DICTIONARY_LENGTH);
            deflater.setInput(input);
            deflater.finish();
            int compressedDataLength = deflater.deflate(output, 0, output.length, Deflater.NO_FLUSH);
            System.out.printf("Deflater::getTotalOut:%s, Deflater::getAdler: %s," +
                            " compressed length: %s%n", deflater.getTotalOut(),
                    deflater.getTotalOut(), compressedDataLength);
            deflater.finished();

            // Decompress the bytes
            inflater.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[RESULT_SIZE];
            int resultLength = inflater.inflate(result);
            if (inflater.needsDictionary()) {
                System.out.println("Specifying Dictionary");
                inflater.setDictionary(DICTIONARY.getBytes(UTF_8), dictionary_offset, DICTIONARY_LENGTH);
                resultLength = inflater.inflate(result);
            } else {
                System.out.println("Did not need to use a Dictionary");
            }
            inflater.finished();
            System.out.printf("Inflater::getAdler:%s, length: %s%n",
                    inflater.getAdler(), resultLength);

            Assert.assertEquals(SRC_DATA.length(), resultLength);
            Assert.assertEquals(input, Arrays.copyOf(result, resultLength));
        } finally {
            // Release Resources
            deflater.end();
            inflater.end();
        }
    }

    /**
     * Validate that a ByteBuffer can be used with Deflater::setDictionary
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testHeapByteBuffer() throws Exception {
        byte[] input = SRC_DATA.getBytes(UTF_8);
        byte[] output = new byte[RESULT_SIZE];
        ByteBuffer dictDef = ByteBuffer.wrap(DICTIONARY.getBytes(UTF_8), DICTIONARY_OFFSET, DICTIONARY_LENGTH);
        ByteBuffer dictInf = ByteBuffer.wrap(DICTIONARY.getBytes(UTF_8), DICTIONARY_OFFSET, DICTIONARY_LENGTH);
        Deflater deflater = new Deflater();
        Inflater inflater = new Inflater();
        try {
            // Compress the bytes
            deflater.setDictionary(dictDef);
            deflater.setInput(input);
            deflater.finish();
            int compressedDataLength = deflater.deflate(output, 0, output.length, Deflater.NO_FLUSH);
            System.out.printf("Deflater::getTotalOut:%s, Deflater::getAdler: %s," +
                            " compressed length: %s%n", deflater.getTotalOut(),
                    deflater.getTotalOut(), compressedDataLength);
            deflater.finished();

            // Decompress the bytes
            inflater.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[RESULT_SIZE];
            int resultLength = inflater.inflate(result);
            if (inflater.needsDictionary()) {
                System.out.println("Specifying Dictionary");
                inflater.setDictionary(dictInf);
                resultLength = inflater.inflate(result);
            } else {
                System.out.println("Did not need to use a Dictionary");
            }
            inflater.finished();
            System.out.printf("Inflater::getAdler:%s, length: %s%n",
                    inflater.getAdler(), resultLength);

            Assert.assertEquals(SRC_DATA.length(), resultLength);
            Assert.assertEquals(input, Arrays.copyOf(result, resultLength));
        } finally {
            // Release Resources
            deflater.end();
            inflater.end();
        }
    }

    /**
     * Validate that ByteBuffer::allocateDirect can be used with Deflater::setDictionary
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testByteBufferDirect() throws Exception {
        byte[] input = SRC_DATA.getBytes(UTF_8);
        byte[] output = new byte[RESULT_SIZE];
        ByteBuffer dictDef = ByteBuffer.allocateDirect(DICTIONARY.length());
        ByteBuffer dictInf = ByteBuffer.allocateDirect(DICTIONARY.length());
        dictDef.put(DICTIONARY.getBytes(UTF_8));
        dictInf.put(DICTIONARY.getBytes(UTF_8));
        dictDef.position(DICTIONARY_OFFSET);
        dictDef.limit(DICTIONARY_LENGTH);
        dictInf.position(DICTIONARY_OFFSET);
        dictInf.limit(DICTIONARY_LENGTH);
        Deflater deflater = new Deflater();
        Inflater inflater = new Inflater();
        try {
            // Compress the bytes
            deflater.setDictionary(dictDef.slice());
            deflater.setInput(input);
            deflater.finish();
            int compressedDataLength = deflater.deflate(output, 0, output.length, Deflater.NO_FLUSH);
            System.out.printf("Deflater::getTotalOut:%s, Deflater::getAdler: %s," +
                            " compressed length: %s%n", deflater.getTotalOut(),
                    deflater.getTotalOut(), compressedDataLength);
            deflater.finished();

            // Decompress the bytes
            inflater.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[RESULT_SIZE];
            int resultLength = inflater.inflate(result);
            if (inflater.needsDictionary()) {
                System.out.println("Specifying Dictionary");
                inflater.setDictionary(dictInf.slice());
                resultLength = inflater.inflate(result);
            } else {
                System.out.println("Did not need to use a Dictionary");
            }
            inflater.finished();
            System.out.printf("Inflater::getAdler:%s, length: %s%n",
                    inflater.getAdler(), resultLength);

            Assert.assertEquals(SRC_DATA.length(), resultLength);
            Assert.assertEquals(input, Arrays.copyOf(result, resultLength));
        } finally {
            // Release Resources
            deflater.end();
            inflater.end();
        }
    }

    /**
     * Validate that an invalid offset used with setDictionary will
     * throw an Exception
     *
     * @param dictionary_offset offset value to be used
     */
    @Test(dataProvider = "invalidDictionaryOffsets")
    public void testInvalidOffsets(int dictionary_offset) {
        byte[] dictionary = DICTIONARY.getBytes(UTF_8);

        Deflater deflater = new Deflater();
        Inflater inflater = new Inflater();
        try {
            assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                    deflater.setDictionary(dictionary, dictionary_offset, DICTIONARY_LENGTH));
            assertThrows(ArrayIndexOutOfBoundsException.class, () ->
                    inflater.setDictionary(dictionary, dictionary_offset, DICTIONARY_LENGTH));
        } finally {
            // Release Resources
            deflater.end();
            inflater.end();
        }
    }
}