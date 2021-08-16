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
 * @bug 8194489
 * @summary The test checks whether BandedSampleModel computes appropriate
 *          size for allocating memory in DataBuffer.
 * @run main BandedSampleModelSizeTest
 */
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.util.Arrays;

public class BandedSampleModelSizeTest {
    // Constants
    private static final int TEST_NUM_BANDS = 3;
    private static final int TEST_SRC_IMG_DIM = 10;

    // Required sample models
    private static BandedSampleModel singleBankModel = null;
    private static BandedSampleModel multiBankModel = null;

    private static void initTest() {
        int[] bandOffsets = new int[TEST_NUM_BANDS];
        int[] bankIndices = new int[TEST_NUM_BANDS];

        /*
         * Create a BandedSampleModel to store samples of all bands in one
         * bank of DataBuffer.
         */
        bandOffsets[0] = 0;
        bandOffsets[1] = 120;
        bandOffsets[2] = 240;
        bankIndices[0] = 0;
        bankIndices[1] = 0;
        bankIndices[2] = 0;

        singleBankModel = new BandedSampleModel(DataBuffer.TYPE_BYTE,
                TEST_SRC_IMG_DIM, TEST_SRC_IMG_DIM, TEST_SRC_IMG_DIM,
                bankIndices, bandOffsets);

        /*
         * Create a BandedSampleModel to store samples of all bands in
         * different banks of DataBuffer.
         */
        bandOffsets[0] = 0;
        bandOffsets[1] = 20;
        bandOffsets[2] = 40;
        bankIndices[0] = 0;
        bankIndices[1] = 1;
        bankIndices[2] = 2;

        multiBankModel = new BandedSampleModel(DataBuffer.TYPE_BYTE,
                TEST_SRC_IMG_DIM, TEST_SRC_IMG_DIM, TEST_SRC_IMG_DIM,
                bankIndices, bandOffsets);
    }

    private static void testSingleBankModel() {
        int[] srcSamples = new int[TEST_NUM_BANDS];
        int[] resSamples = new int[TEST_NUM_BANDS];

        // Create image buffer for the requried sample model
        DataBuffer imgBuffer = singleBankModel.createDataBuffer();

        // Test the sample model by setting a pixel value & inspecting the same.
        Arrays.fill(srcSamples, 125);
        singleBankModel.setPixel(0, 0, srcSamples, imgBuffer);
        singleBankModel.getPixel(0, 0, resSamples, imgBuffer);
        if (!Arrays.equals(srcSamples, resSamples)) {
            throw new RuntimeException("Test Failed. Incorrect samples found"
                    + " in the image");
        }

        Arrays.fill(srcSamples, 250);
        singleBankModel.setPixel(9, 9, srcSamples, imgBuffer);
        singleBankModel.getPixel(9, 9, resSamples, imgBuffer);
        if (!Arrays.equals(srcSamples, resSamples)) {
            throw new RuntimeException("Test Failed. Incorrect samples found"
                    + " in the image");
        }
    }

    private static void testMultiBankModel() {
        int[] srcSamples = new int[TEST_NUM_BANDS];
        int[] resSamples = new int[TEST_NUM_BANDS];

        // Create image buffer for the required sample model
        DataBuffer imgBuffer = multiBankModel.createDataBuffer();

        // Test the sample model by setting a pixel value & inspecting the same.
        Arrays.fill(srcSamples, 125);
        multiBankModel.setPixel(0, 0, srcSamples, imgBuffer);
        multiBankModel.getPixel(0, 0, resSamples, imgBuffer);
        if (!Arrays.equals(srcSamples, resSamples)) {
            throw new RuntimeException("Test Failed. Incorrect samples found"
                    + " in the image");
        }

        Arrays.fill(srcSamples, 250);
        multiBankModel.setPixel(9, 9, srcSamples, imgBuffer);
        multiBankModel.getPixel(9, 9, resSamples, imgBuffer);
        if (!Arrays.equals(srcSamples, resSamples)) {
            throw new RuntimeException("Test Failed. Incorrect samples found"
                    + " in the image");
        }
    }

    public static void main(String args[]) {
        // Initialize the test
        initTest();

        // Test banded sample model with single bank of data buffer
        testSingleBankModel();

        // Test banded sample model with multiple banks of data buffer
        testMultiBankModel();
    }
}
