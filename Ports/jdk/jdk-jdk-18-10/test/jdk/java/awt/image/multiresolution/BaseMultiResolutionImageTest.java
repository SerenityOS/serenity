/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.MultiResolutionImage;
import java.util.List;

/**
 * @test
 * @bug 8029339
 * @author Alexander Scherbatiy
 * @summary Custom MultiResolution image support on HiDPI displays
 * @run main BaseMultiResolutionImageTest
 */
public class BaseMultiResolutionImageTest {

    public static void main(String[] args) {
        testZeroRVIMages();
        testNullRVIMages();
        testNullRVIMage();
        testIOOBException();
        testRVSizes();
        testBaseMRImage();
    }

    static void testZeroRVIMages() {
        try {
            new BaseMultiResolutionImage();
        } catch (IllegalArgumentException ignored) {
            return;
        }
        throw new RuntimeException("IllegalArgumentException is not thrown!");
    }

    static void testNullRVIMages() {
        try {
            new BaseMultiResolutionImage(null);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        throw new RuntimeException("IllegalArgumentException is not thrown!");
    }

    static void testNullRVIMage() {

        Image baseImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        try {
            new BaseMultiResolutionImage(baseImage, null);
        } catch (NullPointerException ignored) {
            return;
        }
        throw new RuntimeException("NullPointerException is not thrown!");
    }

    static void testIOOBException() {

        for (int baseImageIndex : new int[]{-3, 2, 4}) {
            try {
                new BaseMultiResolutionImage(baseImageIndex,
                        createRVImage(0), createRVImage(1));
            } catch (IndexOutOfBoundsException ignored) {
                continue;
            }

            throw new RuntimeException("IndexOutOfBoundsException is not thrown!");
        }
    }

    static void testRVSizes() {

        int imageSize = getSize(1);

        double[][] sizeArray = {
            {-imageSize, imageSize},
            {2 * imageSize, -2 * imageSize},
            {Double.POSITIVE_INFINITY, imageSize},
            {Double.POSITIVE_INFINITY, -imageSize},
            {imageSize, Double.NEGATIVE_INFINITY},
            {-imageSize, Double.NEGATIVE_INFINITY},
            {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY},
            {Double.NaN, imageSize},
            {imageSize, Double.NaN},
            {Double.NaN, Double.NaN},
            {Double.POSITIVE_INFINITY, Double.NaN}
        };

        for (double[] sizes : sizeArray) {
            try {
                MultiResolutionImage mrImage = new BaseMultiResolutionImage(
                        0, createRVImage(0), createRVImage(1));
                mrImage.getResolutionVariant(sizes[0], sizes[1]);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            throw new RuntimeException("IllegalArgumentException is not thrown!");
        }
    }

    static void testBaseMRImage() {
        int baseIndex = 1;
        int length = 3;
        BufferedImage[] resolutionVariants = new BufferedImage[length];
        for (int i = 0; i < length; i++) {
            resolutionVariants[i] = createRVImage(i);
        }

        BaseMultiResolutionImage mrImage = new BaseMultiResolutionImage(baseIndex,
                resolutionVariants);

        List<Image> rvImageList = mrImage.getResolutionVariants();
        if (rvImageList.size() != length) {
            throw new RuntimeException("Wrong size of resolution variants list!");
        }

        for (int i = 0; i < length; i++) {
            int imageSize = getSize(i);
            Image testRVImage = mrImage.getResolutionVariant(imageSize, imageSize);

            if (testRVImage != resolutionVariants[i]) {
                throw new RuntimeException("Wrong resolution variant!");
            }

            if (rvImageList.get(i) != resolutionVariants[i]) {
                throw new RuntimeException("Wrong resolution variant!");
            }
        }

        BufferedImage baseImage = resolutionVariants[baseIndex];

        if (baseImage.getWidth() != mrImage.getWidth(null)
                || baseImage.getHeight() != mrImage.getHeight(null)) {
            throw new RuntimeException("Base image is wrong!");
        }

        boolean passed = false;

        try {
            rvImageList.set(0, createRVImage(10));
        } catch (Exception e) {
            passed = true;
        }

        if (!passed) {
            throw new RuntimeException("Resolution variants list is modifiable!");
        }

        passed = false;

        try {
            rvImageList.remove(0);
        } catch (Exception e) {
            passed = true;
        }

        if (!passed) {
            throw new RuntimeException("Resolution variants list is modifiable!");
        }

        passed = false;

        try {
            rvImageList.add(0, createRVImage(10));
        } catch (Exception e) {
            passed = true;
        }

        if (!passed) {
            throw new RuntimeException("Resolution variants list is modifiable!");
        }

        passed = false;
        try {
            mrImage.getGraphics();
        } catch (UnsupportedOperationException e) {
            passed = true;
        }

        if (!passed) {
            throw new RuntimeException("getGraphics() method shouldn't be supported!");
        }
    }

    private static int getSize(int i) {
        return 8 * (i + 1);
    }

    private static BufferedImage createRVImage(int i) {
        return new BufferedImage(getSize(i), getSize(i),
                BufferedImage.TYPE_INT_RGB);
    }
}
