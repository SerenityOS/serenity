/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.image;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Performs "rough" image search.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class RoughImageFinder implements ImageFinder {

    double roughness = .0;
    int bigWidth, bigHeight;
    int[][] bigPixels;

    /**
     * Creates an instance allowing to find an image inside the one passed as
     * parameter with some "roughness".
     *
     * @param area - Image to search in.
     * @param roughness - Allowed
     */
    public RoughImageFinder(BufferedImage area, double roughness) {
        this.roughness = roughness;
        bigWidth = area.getWidth();
        bigHeight = area.getHeight();
        bigPixels = new int[bigWidth][bigHeight];
        for (int x = 0; x < bigWidth; x++) {
            for (int y = 0; y < bigHeight; y++) {
                bigPixels[x][y] = area.getRGB(x, y);
            }
        }
    }

    /**
     * Performs "rough" search.
     *
     * @param image an image to search.
     * @param index an ordinal image location index.
     * @return Point where number of unmatching pixels less or equal to      <code>image1.getWidth() * image1.getHeight() * roughness<code>
     */
    @Override
    public Point findImage(BufferedImage image, int index) {
        int smallWidth = image.getWidth();
        int smallHeight = image.getHeight();
        int[][] smallPixels = new int[smallWidth][smallHeight];
        for (int x = 0; x < smallWidth; x++) {
            for (int y = 0; y < smallHeight; y++) {
                smallPixels[x][y] = image.getRGB(x, y);
            }
        }
        double maxRoughPixels = (double) (smallWidth * smallHeight) * roughness;
        int count = 0;
        for (int X = 0; X <= bigWidth - smallWidth; X++) {
            for (int Y = 0; Y <= bigHeight - smallHeight; Y++) {
                int roughPixels = 0;
                for (int x = 0; x < smallWidth; x++) {
                    for (int y = 0; y < smallHeight; y++) {
                        if (smallPixels[x][y] != bigPixels[X + x][Y + y]) {
                            roughPixels++;
                            if (roughPixels > maxRoughPixels) {
                                break;
                            }
                        }
                    }
                    if (roughPixels > maxRoughPixels) {
                        break;
                    }
                }
                if (roughPixels <= maxRoughPixels) {
                    if (count == index) {
                        return new Point(X, Y);
                    }
                    count++;
                }
            }
        }
        return null;
    }
}
